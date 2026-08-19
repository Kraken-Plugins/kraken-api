package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.FairyRingWidgets;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;

/**
 * Rides a fairy ring to the destination its code names.
 *
 * <p>The code comes from the transport's display info, which the dataset fills in per destination
 * when it expands the ring network into journeys. Selection prefers the travel log, which offers one
 * entry per code and needs no knowledge of where the dials currently point; setting the dials by hand
 * is the fallback.</p>
 *
 * <p>Two destinations are not codes. Zanaris is reached by an option on the ring itself, and one
 * dataset entry chains several codes together for a multi hop journey, which a single selection
 * cannot express.</p>
 */
@Slf4j
public class FairyRingHandler implements TransportHandler {

    /** Display info used by the ring that leads to Zanaris rather than to a coded destination. */
    private static final String ZANARIS = "ZANARIS";

    /** Menu actions the travel log entries and dial buttons are known by. */
    private static final String[] TRAVEL_ACTIONS = {"Travel", "Teleport", "Confirm", "Select"};

    /** How long to wait for the ring interface to open. */
    private static final long INTERFACE_TIMEOUT_MS = 5_000;

    /** How long to wait for the ride to land. */
    private static final long RIDE_TIMEOUT_MS = 12_000;

    /** How far the player must move for the ride to count as having happened. */
    private static final int RIDE_DISTANCE = 16;

    @Override
    public boolean execute(TransportContext context) {
        String raw = context.getDisplayInfo();
        WorldPoint before = context.playerLocation();

        if (raw != null && ZANARIS.equalsIgnoreCase(raw.trim())) {
            return TransportEntityResolver.interactUsing(context, "Zanaris")
                    && awaitRide(context, before);
        }

        DisplayInfo displayInfo = DisplayInfo.parse(raw);
        if (displayInfo == null || !displayInfo.isFairyRing()) {
            log.debug("Fairy ring transport has no single code to select: {}", raw);
            return false;
        }

        String code = displayInfo.getFairyRingCode();
        if (!TransportEntityResolver.interact(context)) {
            return false;
        }

        if (!SleepService.sleepUntil(() -> isRingInterfaceOpen(context.getCtx()), INTERFACE_TIMEOUT_MS)) {
            log.debug("Fairy ring interface did not open");
            return false;
        }

        if (!select(context.getCtx(), code)) {
            log.debug("Could not select fairy ring code {}", code);
            return false;
        }

        return awaitRide(context, before);
    }

    /**
     * Selects a code, preferring the travel log over the dials.
     */
    private boolean select(Context ctx, String code) {
        int logEntry = FairyRingWidgets.logEntry(code);
        if (logEntry != FairyRingWidgets.NOT_FOUND && clickAny(ctx, logEntry)) {
            return true;
        }

        return setDials(ctx, code);
    }

    /**
     * Sets each dial to its letter and confirms.
     *
     * <p>Used when the travel log is not showing. Selecting a letter a dial already points at changes
     * nothing, so the current setting does not have to be read first.</p>
     */
    private boolean setDials(Context ctx, String code) {
        for (int i = 0; i < code.length(); i++) {
            int letter = FairyRingWidgets.dialLetter(code.charAt(i));
            if (letter == FairyRingWidgets.NOT_FOUND || !clickAny(ctx, letter)) {
                return false;
            }
            SleepService.sleepFor(1);
        }

        return clickAny(ctx, InterfaceID.Fairyrings.CONFIRM);
    }

    private boolean clickAny(Context ctx, int packedId) {
        WidgetEntity widget = ctx.widgets().get(packedId);
        if (widget == null || widget.isNull()) {
            return false;
        }

        for (String action : TRAVEL_ACTIONS) {
            if (widget.interact(action)) {
                return true;
            }
        }

        return false;
    }

    private boolean isRingInterfaceOpen(Context ctx) {
        WidgetEntity confirm = ctx.widgets().get(InterfaceID.Fairyrings.CONFIRM);
        if (confirm != null && confirm.isPresent()) {
            return true;
        }

        WidgetEntity log = ctx.widgets().get(InterfaceID.FairyringsLog.CONTENTS);
        return log != null && log.isPresent();
    }

    private boolean awaitRide(TransportContext context, WorldPoint before) {
        if (before == null) {
            SleepService.sleepFor(3);
            return true;
        }

        return SleepService.sleepUntil(() -> {
            WorldPoint now = context.playerLocation();
            return now != null && now.distanceTo(before) > RIDE_DISTANCE;
        }, RIDE_TIMEOUT_MS);
    }
}
