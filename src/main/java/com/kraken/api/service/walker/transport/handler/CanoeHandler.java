package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.CanoeStage;
import com.kraken.api.service.walker.transport.CanoeWidgets;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.WarningWidgets;
import com.kraken.api.service.walker.transport.WidgetClicks;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.List;

/**
 * Builds a canoe at a canoe station and paddles it to another stop on the same river.
 *
 * <p>A canoe is not a single click. The station's tree is felled, the trunk is shaped into a hull,
 * the hull is pushed into the water, and only then does paddling it open the map the destination is
 * chosen from. The station is one object that offers one action per stage, so the handler reads the
 * action, uses it, and waits for the station to move on, until the canoe is afloat. A station that
 * is already part way along is carried on from where it stands.</p>
 *
 * <p>The largest hull the shaping interface offers is the one shaped. The game offers a hull only
 * at the woodcutting level it needs, and a larger hull reaches every stop a smaller one does, so
 * that hull reaches the destination whenever the player's level allows the trip at all. The map
 * then offers {@code Travel to} only on the stops the floating canoe reaches. When the destination
 * is not among them the crossing fails and says so, which is what happens when a smaller canoe was
 * left afloat at the station earlier.</p>
 *
 * <p>A canoe transport states the station to click and names the stop in its display info, which
 * is all this works from.</p>
 */
@Slf4j
public class CanoeHandler implements TransportHandler {

    /** How long the whole build may take before the crossing is given up. */
    private static final long BUILD_TIMEOUT_MS = 120_000;

    /**
     * How long the player may stand idle after one of the station's actions was used before the
     * station is taken not to have responded.
     */
    private static final long STAGE_IDLE_TIMEOUT_MS = 30_000;

    /** How long a station that cannot be read is waited for, as while the scene reloads. */
    private static final int STATION_TIMEOUT_MS = 5_000;

    /** How long to wait for the shaping interface or the river map to open. */
    private static final long INTERFACE_TIMEOUT_MS = 10_000;

    /** How long to wait for the open river map to offer the destination. */
    private static final long DESTINATION_TIMEOUT_MS = 3_000;

    /** How long to wait for the canoe to arrive once a stop was chosen. */
    private static final long TRAVEL_TIMEOUT_MS = 30_000;

    /** How many times to try paddling before deciding the river map will not open. */
    private static final int MAX_PADDLE_ATTEMPTS = 3;

    /** How far the player must move for the journey to count as having happened. */
    private static final int TRAVEL_DISTANCE = 16;

    /** The chat option that confirms a journey the game asks about, such as one into the Wilderness. */
    private static final String CONFIRM_OPTION = "Yes";

    @Override
    public boolean execute(TransportContext context) {
        String destination = context.getDisplayInfo() != null ? context.getDisplayInfo().trim() : "";
        if (destination.isEmpty()) {
            return context.fail("the canoe names no stop to paddle to");
        }
        if (context.getObjectInfo() == null || context.getOrigin() == null) {
            return context.fail("the canoe names no station to build at");
        }

        WorldPoint before = context.playerLocation();
        List<String> offered = null;
        int paddleAttempts = 0;
        long deadline = System.currentTimeMillis() + BUILD_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            offered = SleepService.sleepUntilNotNull(() -> stationActions(context), STATION_TIMEOUT_MS);
            if (offered == null) {
                return context.fail("no canoe station stands by " + context.getOrigin());
            }

            CanoeStage stage = CanoeStage.of(offered);
            if (stage == CanoeStage.BUSY) {
                SleepService.sleep(Constants.GAME_TICK_LENGTH);
                continue;
            }

            String action = stage.actionIn(offered);
            log.debug("Canoe station by {} is at {}, using '{}'", context.getOrigin(), stage, action);
            if (!TransportEntityResolver.interactUsing(context, action)) {
                return context.fail("could not use " + action + " on the canoe station");
            }

            if (stage == CanoeStage.PADDLE) {
                if (SleepService.sleepUntil(() -> isMapOpen(context.getCtx()), INTERFACE_TIMEOUT_MS)) {
                    return paddleTo(context, destination, before);
                }
                if (++paddleAttempts >= MAX_PADDLE_ATTEMPTS) {
                    return context.fail("the canoe map did not open after " + action + " was used "
                            + paddleAttempts + " times");
                }
                continue;
            }

            if (stage == CanoeStage.SHAPE && !shapeLargestHull(context)) {
                return false;
            }

            if (!awaitNextStage(context, stage, deadline)) {
                return context.fail("the canoe station still offers " + action + " after it was used");
            }
        }

        return context.fail("no canoe was afloat after " + BUILD_TIMEOUT_MS / 1000
                + " seconds, and the station offers " + offered);
    }

    /**
     * Reads the menu actions the station offers right now.
     *
     * @param context the canoe being operated
     * @return the station's actions, or null when no station is found by the transport's origin
     */
    private List<String> stationActions(TransportContext context) {
        TransportEntityResolver.Located station = TransportEntityResolver.locate(
                context.getCtx(), context.getObjectInfo(), context.getOrigin());
        return station != null ? station.getActions() : null;
    }

    /**
     * Waits for the station to leave a stage after its action was used.
     *
     * <p>Felling and shaping take as long as the player's axe and level make them, so the wait
     * does not run out while the player is still at work. It ends without the station having moved
     * on once the player has stood idle for {@link #STAGE_IDLE_TIMEOUT_MS}, or when the time for
     * the whole build is up. A station that cannot be read counts as not having moved, so a scene
     * that is reloading does not end the wait early.</p>
     *
     * @param context the canoe being operated
     * @param stage the stage whose action was just used
     * @param deadline when the whole build is given up, in epoch milliseconds
     * @return true when the station moved on to another stage
     */
    private boolean awaitNextStage(TransportContext context, CanoeStage stage, long deadline) {
        boolean[] movedOn = new boolean[1];
        long[] lastAtWork = {System.currentTimeMillis()};

        SleepService.sleepUntil(() -> {
            List<String> actions = stationActions(context);
            movedOn[0] = actions != null && CanoeStage.of(actions) != stage;
            if (movedOn[0]) {
                return true;
            }

            long now = System.currentTimeMillis();
            if (!context.getCtx().players().local().isIdle()) {
                lastAtWork[0] = now;
            }
            return now - lastAtWork[0] > STAGE_IDLE_TIMEOUT_MS;
        }, Math.max(0, deadline - System.currentTimeMillis()));

        return movedOn[0];
    }

    /**
     * Waits for the shaping interface and clicks the largest hull it offers.
     *
     * @param context the canoe being operated
     * @return true when a hull was clicked, false with a failure reason otherwise
     */
    private boolean shapeLargestHull(TransportContext context) {
        Context ctx = context.getCtx();
        Widget[] found = new Widget[1];
        boolean offered = SleepService.sleepUntil(() -> {
            found[0] = ctx.runOnClientThread(() -> largestHullButton(ctx.getClient()), null);
            return found[0] != null;
        }, INTERFACE_TIMEOUT_MS);
        if (!offered) {
            return context.fail("the canoe shaping interface offered no hull to make");
        }

        String action = CanoeWidgets.makeAction(found[0].getActions());
        if (action == null || !ctx.getInteractionManager().interact(found[0], action)) {
            return context.fail("could not choose a hull on the canoe shaping interface");
        }

        log.debug("Shaping a canoe with '{}'", Text.removeTags(action));
        return true;
    }

    /**
     * Finds the button of the largest hull the shaping interface offers. Client thread only.
     *
     * @param client the game client
     * @return the button, or null when the interface is closed or offers no hull
     */
    private static Widget largestHullButton(Client client) {
        for (int layerId : CanoeWidgets.hullLayers()) {
            Widget layer = client.getWidget(layerId);
            if (layer == null || layer.isHidden()) {
                continue;
            }

            Widget button = WidgetClicks.childAt(layer, 0);
            if (button != null && CanoeWidgets.makeAction(button.getActions()) != null) {
                return button;
            }
        }
        return null;
    }

    /**
     * Chooses the destination on the river map and waits for the canoe to get there.
     *
     * <p>The map is given a moment to put its actions on the stops before the destination is taken
     * to be out of reach. A journey the game asks about first is confirmed on every poll of the
     * wait, because the question appears a moment after the stop is chosen.</p>
     *
     * @param context the canoe being operated
     * @param destination the stop to paddle to
     * @param before where the player stood before the canoe was built, may be null
     * @return true when the canoe left, false with a failure reason otherwise
     */
    private boolean paddleTo(TransportContext context, String destination, WorldPoint before) {
        Context ctx = context.getCtx();
        Widget[] found = new Widget[1];
        SleepService.sleepUntil(() -> {
            found[0] = ctx.runOnClientThread(() -> destinationButton(ctx.getClient(), destination), null);
            return found[0] != null;
        }, DESTINATION_TIMEOUT_MS);

        String action = found[0] != null ? CanoeWidgets.travelAction(found[0].getActions(), destination) : null;
        if (action == null) {
            return context.fail("the canoe afloat at this station does not reach " + destination);
        }
        if (!ctx.getInteractionManager().interact(found[0], action)) {
            return context.fail("could not choose " + destination + " on the canoe map");
        }

        if (before == null) {
            SleepService.sleepFor(3);
            return true;
        }

        DialogueService dialogue = ctx.getService(DialogueService.class);
        boolean left = SleepService.sleepUntil(() -> {
            WarningWidgets.dismiss(ctx);
            if (dialogue.isOptionPresent(CONFIRM_OPTION)) {
                dialogue.selectOption(CONFIRM_OPTION);
            }

            WorldPoint now = context.playerLocation();
            return now != null && now.distanceTo(before) > TRAVEL_DISTANCE;
        }, TRAVEL_TIMEOUT_MS);

        return left || context.fail("chose " + destination + " on the canoe map but the canoe did not leave");
    }

    private static boolean isMapOpen(Context ctx) {
        return Boolean.TRUE.equals(ctx.runOnClientThread(() -> {
            for (int layerId : CanoeWidgets.destinationLayers()) {
                Widget layer = ctx.getClient().getWidget(layerId);
                if (layer != null && !layer.isHidden()) {
                    return true;
                }
            }
            return false;
        }, Boolean.FALSE));
    }

    /**
     * Finds the button of a stop on whichever river map is open. Client thread only.
     *
     * @param client the game client
     * @param destination the stop to paddle to
     * @return the button, or null when no open map offers that stop
     */
    private static Widget destinationButton(Client client, String destination) {
        for (int layerId : CanoeWidgets.destinationLayers()) {
            Widget layer = client.getWidget(layerId);
            if (layer == null || layer.isHidden()) {
                continue;
            }

            for (Widget button : layer.getStaticChildren()) {
                if (button != null && CanoeWidgets.travelAction(button.getActions(), destination) != null) {
                    return button;
                }
            }
        }
        return null;
    }
}
