package com.kraken.api.service.prayer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.core.interaction.timing.ClickLocation;
import com.kraken.api.core.interaction.timing.ClickScheduler;
import com.kraken.api.core.interaction.timing.ClickTiming;
import com.kraken.api.core.interaction.timing.ScheduledClick;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.kraken.api.service.prayer.InteractablePrayer.isOverhead;

/**
 * Turns prayers on and off through the prayer interface and reports whether they are active.
 */
@Slf4j
@Singleton
public class PrayerService {

    @Inject
    private Context ctx;

    @Inject
    private InteractionManager interactionManager;

    @Inject
    private ClickScheduler clickScheduler;

    private final Map<Prayer, ScheduledPrayerClick> scheduledClicks = new EnumMap<>(Prayer.class);

    /**
     * Wrapper method which turns a prayer on.
     * @param prayer The prayer to turn on.
     */
    public void activatePrayer(Prayer prayer) {
        toggle(prayer, true);
    }

    /**
     * Wrapper method which turns a prayer off.
     * @param prayer The prayer to turn off.
     * @return Boolean true if the prayer was activated/deactivated successfully and false otherwise.
     */
    public boolean deactivatePrayer(Prayer prayer) {
        return toggle(prayer, false);
    }

    /**
     * Toggles a prayer on or off. This will use reflection based prayer toggles by default.
     * @param prayer The Prayer to toggle
     * @return Boolean true if the prayer was activated/deactivated successfully and false otherwise.
     */
    public boolean toggle(Prayer prayer) {
        if(ctx.getClient().isPrayerActive(prayer)) {
            return toggle(prayer, false);
        } else {
            return toggle(prayer, true);
        }
    }

    /**
     * Toggles a prayer on or off.
     * @param prayer The Prayer to toggle
     * @param activate True if the prayer should be turned on and false if it should be turned off
     * @return Boolean true if the prayer was activated/deactivated successfully and false otherwise.
     */
    public boolean toggle(Prayer prayer, boolean activate) {
        return toggle(prayer, activate, null);
    }

    /**
     * Toggles a prayer on or off, clicking a point chosen beforehand.
     *
     * @param prayer   the prayer to toggle.
     * @param activate true if the prayer should be turned on and false if it should be turned off.
     * @param location where to click, or null to choose a point now.
     * @return true if a click was sent, false if the prayer was already in that state or cannot be used.
     */
    private boolean toggle(Prayer prayer, boolean activate, ClickLocation location) {
        InteractablePrayer prayerExtended = usablePrayer(prayer);
        if (prayerExtended == null) {
            return false;
        }

        boolean currentlyActive = isActive(prayer);

        // Do nothing if already in desired state
        if (currentlyActive == activate) {
            return false;
        }

        if (location == null) {
            interactionManager.interact(prayerExtended.getIndex(), -1, -1, 1);
        } else {
            interactionManager.interact(prayerExtended.getIndex(), -1, -1, 1, location.getPoint());
        }
        return true;
    }

    /**
     * Turns a prayer on or off with a click sent by {@link ClickScheduler} at a chosen point in the current tick,
     * instead of at once. A click sent before the scheduler's cutoff applies on the next tick, as {@link #toggle}
     * does.
     *
     * <p>The click point is chosen once, when the scheduler first needs it, and that point is both the end of the
     * travel time from the previous click and the point the click lands on.
     *
     * <p>The prayer counts as being in its new state from now until the click has been sent and the client shows the
     * change (by the next client cycle), or one tick has passed. In that time a second request for the same state
     * does nothing, and a request for the opposite state cancels the click if it has not been sent yet. When the
     * click falls due it checks the prayer again, so a change the player made in the meantime is never reversed.
     *
     * @param owner    the object scheduling the click, used by {@link #cancelScheduled(Object)}; usually the plugin
     *                 helper that plans the prayers.
     * @param prayer   the prayer to turn on or off.
     * @param activate true to turn the prayer on, false to turn it off.
     * @param timing   when the click should leave; {@link ClickTiming#immediate()} sends it within this call.
     * @return true if a click was scheduled or sent, false if none was needed or the prayer cannot be used.
     */
    public boolean schedule(Object owner, Prayer prayer, boolean activate, ClickTiming timing) {
        InteractablePrayer interactable = usablePrayer(prayer);
        if (interactable == null) {
            return false;
        }

        ScheduledPrayerClick previous;
        synchronized (scheduledClicks) {
            previous = scheduledClicks.get(prayer);
        }

        boolean active = isActive(prayer);
        boolean heading = active;
        if (previous != null && isInFlight(previous, active)) {
            if (previous.isActivate() == activate) {
                return false;
            }

            if (previous.getClick().cancel()) {
                forget(prayer, previous);
            } else {
                heading = previous.isActivate();
            }
        }

        if (heading == activate) {
            return false;
        }

        PrayerClickPoint point = new PrayerClickPoint(interactable);
        ScheduledClick click = clickScheduler.scheduleTargeted(owner, timing, point::get,
                () -> toggle(prayer, activate, point.get()));
        synchronized (scheduledClicks) {
            scheduledClicks.put(prayer, new ScheduledPrayerClick(click, activate));
        }
        return click.isPending() || click.isSent();
    }

    /**
     * Returns true while a click from {@link #schedule} for this prayer is waiting to be sent, or has been sent but
     * the client does not show its change yet (at most one tick).
     *
     * @param prayer the prayer to check.
     * @return true while the prayer has a click in flight.
     */
    public boolean isInFlight(Prayer prayer) {
        ScheduledPrayerClick scheduled;
        synchronized (scheduledClicks) {
            scheduled = scheduledClicks.get(prayer);
        }
        return scheduled != null && isInFlight(scheduled, isActive(prayer));
    }

    /**
     * Returns the state the prayer is heading to: the state a click in flight from {@link #schedule} will leave it
     * in, otherwise whether it is active. Plan against this rather than {@link #isActive} so a prayer is never
     * clicked twice.
     *
     * @param prayer the prayer to check.
     * @return true if the prayer is on or is being turned on.
     */
    public boolean willBeActive(Prayer prayer) {
        ScheduledPrayerClick scheduled;
        synchronized (scheduledClicks) {
            scheduled = scheduledClicks.get(prayer);
        }

        boolean active = isActive(prayer);
        if (scheduled == null) {
            return active;
        }

        if (isInFlight(scheduled, active)) {
            return scheduled.isActivate();
        }

        forget(prayer, scheduled);
        return active;
    }

    /**
     * Cancels every prayer click {@code owner} scheduled that has not been sent yet.
     *
     * @param owner the owner passed to {@link #schedule}.
     */
    public void cancelScheduled(Object owner) {
        clickScheduler.cancel(owner);
        synchronized (scheduledClicks) {
            scheduledClicks.values().removeIf(scheduled -> scheduled.getClick().getOwner() == owner
                    && scheduled.getClick().getState() == ScheduledClick.State.CANCELLED);
        }
    }

    /**
     * @param scheduled a scheduled prayer click.
     * @param active    whether its prayer is active now.
     * @return true while the click is pending, or sent less than a tick ago without the client showing its change.
     */
    private boolean isInFlight(ScheduledPrayerClick scheduled, boolean active) {
        ScheduledClick click = scheduled.getClick();
        switch (click.getState()) {
            case PENDING:
            case SENDING:
                return true;
            case SENT:
                return active != scheduled.isActivate()
                        && clickScheduler.currentTimeMillis() - click.getSentAtMillis() < ClickScheduler.TICK_LENGTH_MS;
            default:
                return false;
        }
    }

    private void forget(Prayer prayer, ScheduledPrayerClick scheduled) {
        synchronized (scheduledClicks) {
            scheduledClicks.remove(prayer, scheduled);
        }
    }

    /**
     * Finds the prayer's widget and checks the player can use it now.
     *
     * @param prayer the prayer to check.
     * @return the prayer's widget data, or null when the prayer is null, has no widget, the player has no prayer
     * points left or the player's level is too low.
     */
    private InteractablePrayer usablePrayer(Prayer prayer) {
        if (prayer == null) {
            return null;
        }

        InteractablePrayer prayerExtended = InteractablePrayer.of(prayer);
        if (prayerExtended == null) {
            return null;
        }

        if (ctx.getClient().getBoostedSkillLevel(Skill.PRAYER) <= 0 ||
                ctx.getClient().getRealSkillLevel(Skill.PRAYER) < prayerExtended.getLevel()) {
            return null;
        }
        return prayerExtended;
    }

    /**
     * Where one scheduled prayer click lands, chosen the first time it is asked for and the same every time after.
     */
    private final class PrayerClickPoint {
        private final InteractablePrayer prayer;
        private ClickLocation location;
        private boolean chosen;

        private PrayerClickPoint(InteractablePrayer prayer) {
            this.prayer = prayer;
        }

        /**
         * @return the click's point and the prayer icon's bounds, or null when the icon's widget does not exist.
         */
        synchronized ClickLocation get() {
            if (!chosen) {
                location = interactionManager.chooseWidgetClick(prayer.getIndex(), 1);
                chosen = true;
            }
            return location;
        }
    }

    /**
     * A prayer click handed to the scheduler, and the state it turns the prayer to.
     */
    @Value
    private static class ScheduledPrayerClick {
        ScheduledClick click;
        boolean activate;
    }

    /**
     * Returns true if the prayer is active and false otherwise.
     * @param prayer Prayer to check.
     * @return Boolean true if the prayer is active (on) and false otherwise.
     */
    public boolean isActive(Prayer prayer) {
        boolean basicPrayerActive = ctx.runOnClientThreadOptional(() -> ctx.getClient().isPrayerActive(prayer)).orElse(false);
        boolean inLMS = ctx.getVarbitValue(VarbitID.BR_INGAME) != 0;

        switch (prayer) {
            case DEADEYE:
                boolean deadeye = ctx.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
                return basicPrayerActive && (deadeye && !inLMS);
            case MYSTIC_VIGOUR:
                boolean mysticVigour = ctx.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
                return basicPrayerActive && (mysticVigour && !inLMS);
            default:
                return basicPrayerActive;
        }
    }

    /**
     * Deactivates prayers based on the provided parameters. This method can limit
     * the deactivation to specific types of prayers and ensure certain prayers are preserved.
     *
     * <p>This method iterates through all available {@code Prayer} values, selectively
     * deactivating prayers according to the following conditions:
     * <ul>
     *   <li>It skips prayers with names containing "RP_" (Ruinous Prayers).</li>
     *   <li>If {@code keepPreserveOn} is {@code true}, the {@code PRESERVE} prayer will not be deactivated.</li>
     *   <li>If {@code onlyProtectionPrayers} is {@code true}, only prayers categorized as protection prayers
     *       will be deactivated.</li>
     *   <li>Deactivation will stop once the number of actions taken reaches {@code maxActions}.</li>
     * </ul>
     *
     * @param keepPreserveOn a boolean indicating whether the {@code PRESERVE} prayer should remain active.
     * @param onlyProtectionPrayers a boolean indicating whether only protection prayers should be deactivated.
     * @param maxActions the maximum number of deactivation actions to perform.
     * @return {@code true} if all applicable prayers were successfully processed within the {@code maxActions} limit;
     *         {@code false} if the method aborted early due to reaching the {@code maxActions} limit.
     */
    public boolean deactivateAll(boolean keepPreserveOn, boolean onlyProtectionPrayers, int maxActions) {
        int actionsTaken = 0;
        for (Prayer prayer : Prayer.values()) {
            // Skip the Ruinous prayers
            if (prayer.name().contains("RP_")) {
                continue;
            }

            if (actionsTaken >= maxActions) {
                return false;
            }

            if (prayer == Prayer.PRESERVE && keepPreserveOn) {
                continue;
            }

            // If only protection prayers should be deactivated, check if this is a protection prayer
            if (onlyProtectionPrayers && !isProtectionPrayer(prayer)) {
                continue;
            }

            boolean deactivated = deactivatePrayer(prayer);
            if (deactivated) {
                actionsTaken++;
            }
        }
        return true;
    }

    /**
     * Deactivates all prayers.
     * This method allows specifying whether to keep the preserve prayer on when deactivating other prayers
     *
     * @param keepPreserve {@literal true} to retain specified preserved states during deactivation,
     *                     {@literal false} to deactivate entirely without preservation.
     *
     * @return {@literal true} if the deactivation process completes successfully,
     *         {@literal false} otherwise.
     */
    public boolean deactivateAll(boolean keepPreserve) {
        return deactivateAll(keepPreserve, false, Prayer.values().length);
    }

    /**
     * Deactivates all prayers (including preserve)
     */
    public void deactivateAll() {
        deactivateAll(false, false, Prayer.values().length);
    }

    /**
     * Deactivates all protection prayers (protect from range, melee, and magic) but will keep other prayers
     * like preserve and protect item active.
     */
    public void deactivateProtectionPrayers() {
        deactivateAll(false, true, Prayer.values().length);
    }

    /**
     * Helper method to check if a prayer is a protection prayer.
     * @param prayer The prayer to check.
     * @return true if the prayer is a protection prayer (melee, range, or magic).
     */
    private boolean isProtectionPrayer(Prayer prayer) {
        return prayer == Prayer.PROTECT_FROM_MELEE ||
                prayer == Prayer.PROTECT_FROM_MISSILES ||
                prayer == Prayer.PROTECT_FROM_MAGIC;
    }

    /**
     * Performs a one-tick prayer flick, enabling and disabling specified prayers within the same game tick.
     * This ensures minimal prayer drain while maintaining the effects of the selected prayers.
     *
     * <p>The method first identifies any currently active prayers and deactivates them if needed.
     * Then, the specified prayers in the {@literal @<varargs>} parameter are activated.
     * If {@code disableAll} is set to {@code true}, all active prayers are deactivated before
     * activating the specified ones; otherwise, only the prayers included in the list are toggled.</p>
     *
     * @param disableAll A {@code boolean} value indicating if all currently active prayers should be
     *                   disabled before activating the specified prayers. If {@code true}, all existing
     *                   prayers (except Ruinous Prayers) are deactivated.
     * @param prayers    A varargs list of {@code Prayer} objects that should be activated during the
     *                   one-tick flick process. These prayers are toggled on after any necessary
     *                   deactivations are completed. If no prayers are provided, no activation takes place.
     */
    public void oneTickFlick(boolean disableAll, Prayer... prayers) {
        // Check if there are any active prayers when you first start flicking
        int active = 0;
        for (Prayer prayer : Prayer.values()) {
            // Skip the Ruinous prayers
            if (prayer.name().contains("RP_")) {
                continue;
            }

            if (isActive(prayer)) {
                active++;
            }
        }

        // The way flicking works is you need to send a deactivation then activation within the same game tick
        if (active > 0) {
            if (disableAll) {
                deactivateAll(false, false, 4);
            } else {
                for (Prayer p : prayers) {
                    deactivatePrayer(p);
                }
            }
        }

        for (Prayer p2 : prayers) {
            activatePrayer(p2);
        }
    }

    /**
     * Retrieves a list of all currently active prayers.
     *
     * <p>This method iterates through all available {@link InteractablePrayer} instances
     * and checks if they are active by invoking {@code isActive()} on each. Any prayers
     * identified as active are added to the returned list.</p>
     *
     * @return A {@link List} of {@link InteractablePrayer} objects that are currently active.
     */
    public static List<InteractablePrayer> getActivePrayers() {
        List<InteractablePrayer> active = new ArrayList<>();

        for (InteractablePrayer prayer : InteractablePrayer.values()) {
            if (prayer.isActive()) {
                active.add(prayer);
            }
        }

        return active;
    }


    /**
     * Retrieves the currently active overhead {@link InteractablePrayer}.
     *
     * <p>This method iterates through all available {@link InteractablePrayer} values
     * and checks each one for the following conditions:
     * <ul>
     *   <li>The prayer is currently active, as determined by its state.</li>
     *   <li>The prayer is classified as an overhead prayer based on additional logic.</li>
     * </ul>
     * If a prayer meets both conditions, it is returned immediately. If no active overhead
     * prayer is found, the method returns {@code null}.
     *
     * @return The active overhead {@link InteractablePrayer}, or {@code null} if no overhead prayer is active.
     */
    public InteractablePrayer getActiveOverhead() {
        for (InteractablePrayer prayer : InteractablePrayer.values()) {
            if (!prayer.isActive() || !isOverhead(prayer)) continue;
            return prayer;
        }

        return null;
    }

    /**
     * Checks if a given quick prayer is set
     * @param prayer The quick prayer to check
     * @return True if the quick prayer is set and false otherwise
     */
    public boolean isQuickPrayerSet(InteractablePrayer prayer) {
        int selectedQuickPrayersVarbit = ctx.getVarbitValue(4102);
        return (selectedQuickPrayersVarbit & (1 << prayer.getQuickPrayerIndex())) != 0;
    }

    /**
     * Determines if quick prayers are currently enabled for the player.
     *
     * <p>The method checks the value of the {@literal @VarbitID.QUICKPRAYER_ACTIVE}
     * game state variable to assess whether quick prayers are active. A return value
     * of {@code true} indicates that quick prayers are enabled, while {@code false}
     * signifies that they are disabled.</p>
     *
     * @return {@code true} if quick prayers are enabled, {@code false} otherwise.
     */
    public boolean isQuickPrayerEnabled() {
        return ctx.runOnClientThread(() -> ctx.getClient().getVarbitValue(VarbitID.QUICKPRAYER_ACTIVE) == 1);
    }

    /**
     * Sets the quick prayers for the player by sending the appropriate widget action packets.
     * If a specified prayer is already set as a quick prayer, it will be skipped.
     *
     * @param prayers The {@literal @}varargs array of {@link InteractablePrayer} objects to be set as quick prayers.
     *                Null values and prayers that are already set will be ignored.
     */
    public void setQuickPrayers(InteractablePrayer... prayers) {
        interactionManager.interact(InterfaceID.Orbs.PRAYERBUTTON, -1, -1, 2);
        for(InteractablePrayer prayer : prayers) {
            if(prayer == null) continue;
            if(isQuickPrayerSet(prayer)) continue;
            interactionManager.interact(InterfaceID.Quickprayer.BUTTONS, prayer.getQuickPrayerIndex(), -1, 1);
        }
        interactionManager.interact(InterfaceID.Quickprayer.CLOSE, -1, -1, 1);
    }

    /**
     * Checks if the player is out of prayer.
     * @return Returns true if the player is out of prayer and false if they still have prayer.
     */
    public boolean isOutOfPrayer() {
        return ctx.getClient().getBoostedSkillLevel(Skill.PRAYER) <= 0;
    }

    /**
     * Toggles the set quick prayers. If the quick prayers are on, they will be turned off.
     * If they are off, they will be turned on.
     */
    public void toggleQuickPrayers() {
        interactionManager.interact(InterfaceID.Orbs.PRAYERBUTTON, -1, -1, 1);
    }

    /**
     * Turns on the set quick prayers
     */
    public void enableQuickPrayers() {
        if(!isQuickPrayerEnabled()) {
            toggleQuickPrayers();
        }
    }

    /**
     * Turns off the set quick prayers
     */
    public void disableQuickPrayers() {
        if(isQuickPrayerEnabled()) {
            toggleQuickPrayers();
        }
    }

    /**
     * Performs a quick flick of the player's quick prayers.
     *
     * <p>This method is designed to activate and deactivate the player's quick prayers
     * in a single operation. If quick prayers are currently disabled, they will be
     * turned on by invoking {@code enableQuickPrayers()}. If quick prayers are already
     * enabled, the method performs a rapid sequence of toggles by calling
     * {@code toggleQuickPrayers()} twice.</p>
     *
     * <p>The behavior of this method ensures that quick prayers are briefly flicked
     * and restored, which may be used in scenarios such as prayer management or minimizing
     * prayer drain during certain game activities.</p>
     *
     * <ul>
     *   <li>If quick prayers are not enabled, they are activated, and no toggle operation is performed.</li>
     *   <li>If quick prayers are enabled, they are toggled off and back on in quick succession.</li>
     * </ul>
     *
     */
    public void flickQuickPrayers() {
        if(!isQuickPrayerEnabled()) {
            enableQuickPrayers();
            return;
        }
        toggleQuickPrayers();
        toggleQuickPrayers();
    }

}