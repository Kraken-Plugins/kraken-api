package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.handler.DialogueTransportHandler;
import com.kraken.api.service.walker.transport.handler.FairyRingHandler;
import com.kraken.api.service.walker.transport.handler.HubDialogueHandler;
import com.kraken.api.service.walker.transport.handler.HubResumePauseHandler;
import com.kraken.api.service.walker.transport.handler.ItemTeleportHandler;
import com.kraken.api.service.walker.transport.handler.ObjectTransportHandler;
import com.kraken.api.service.walker.transport.handler.SpellTeleportHandler;
import com.kraken.api.service.walker.transport.handler.UnsupportedTransportHandler;
import com.kraken.api.service.walker.transport.handler.WidgetSelectHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Constants;
import net.runelite.api.coords.WorldPoint;
import shortestpath.transport.Transport;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Operates one transport and reports whether the player got through it.
 *
 * <p>Dispatch is by {@link TransportShape} rather than by transport type, so twenty six kinds of
 * transport are served by a handful of handlers.</p>
 */
@Slf4j
@Singleton
public class TransportExecutor {

    /** Floor for how long to wait for a crossing to resolve. */
    private static final long MIN_ARRIVAL_TIMEOUT_MS = 4_000;

    /** Added to a transport's own advertised duration when waiting for it. */
    private static final long ARRIVAL_GRACE_MS = 3_000;

    /**
     * How long to wait for the player to finish walking the last approach tile before clicking.
     *
     * <p>Approach walking returns at distance ≤ 2, so without this the first Cross fires while they
     * still have a tile to walk, and the next round clicks again during that step.</p>
     */
    private static final long IDLE_BEFORE_CLICK_MS = 8_000;

    @Inject
    private Context ctx;

    @Inject
    private PlayerStateReader playerStateReader;

    @Inject
    private TileService tileService;

    private final Map<TransportShape, TransportHandler> handlers = new EnumMap<>(TransportShape.class);

    @Inject
    TransportExecutor(ObjectTransportHandler objectHandler,
                      DialogueTransportHandler dialogueHandler,
                      HubDialogueHandler hubDialogueHandler,
                      HubResumePauseHandler hubResumePauseHandler,
                      FairyRingHandler fairyRingHandler,
                      ItemTeleportHandler itemHandler,
                      SpellTeleportHandler spellHandler,
                      WidgetSelectHandler widgetSelectHandler,
                      UnsupportedTransportHandler unsupportedHandler) {
        handlers.put(TransportShape.SINGLE_CLICK, objectHandler);
        handlers.put(TransportShape.CLICK_THEN_DIALOGUE, dialogueHandler);
        handlers.put(TransportShape.HUB_DIALOGUE, hubDialogueHandler);
        handlers.put(TransportShape.HUB_RESUME_PAUSE, hubResumePauseHandler);
        handlers.put(TransportShape.FAIRY_RING, fairyRingHandler);
        handlers.put(TransportShape.ITEM_SUBOP, itemHandler);
        handlers.put(TransportShape.SPELL, spellHandler);
        handlers.put(TransportShape.CLICK_THEN_WIDGET, widgetSelectHandler);
        handlers.put(TransportShape.GROUPING_TELEPORT, unsupportedHandler);
        handlers.put(TransportShape.CANOE, unsupportedHandler);
    }

    /**
     * The result of trying to cross one transport.
     */
    @Getter
    public static final class Result {
        private final boolean crossed;
        private final boolean requirementsUnmet;
        private final boolean supported;
        private final String reason;

        private Result(boolean crossed, boolean requirementsUnmet, boolean supported, String reason) {
            this.crossed = crossed;
            this.requirementsUnmet = requirementsUnmet;
            this.supported = supported;
            this.reason = reason;
        }

        static Result crossed() {
            return new Result(true, false, true, "crossed");
        }

        static Result failed(String reason) {
            return new Result(false, false, true, reason);
        }

        static Result unmet(String reason) {
            return new Result(false, true, true, reason);
        }

        static Result unsupported(String reason) {
            return new Result(false, false, false, reason);
        }
    }

    /**
     * Operates a transport and waits to see whether the player got through.
     *
     * <p>If the destination is the next tile and already reachable, nothing is clicked: an open door
     * has no {@code Open} action, and operating it would fail. A tile that is reachable only by
     * walking around a wall is not an open door — skipping Climb-into on the Varrock underwall is
     * what stalled on the tunnel origin. Teleports are not skipped this way either. The player is
     * waited to idle before the click so a last approach tile is not a second Cross mid-step.</p>
     *
     * @param usage the transport edge chosen by the planner
     * @return what happened, with a readable reason when it did not work
     */
    public Result execute(GlobalPathfinder.TransportUsage usage) {
        if (usage == null) {
            return Result.unsupported("no transport to execute");
        }

        Transport transport = usage.getTransport();
        if (transport == null) {
            return Result.unsupported("route carries no transport detail for " + usage.getType());
        }

        TransportShape shape = TransportShapes.of(usage.getType());
        if (shape == null) {
            return Result.unsupported("no execution shape known for " + usage.getType());
        }

        TransportHandler handler = handlers.get(shape);
        if (handler == null) {
            return Result.unsupported("no handler registered for " + shape);
        }

        TransportRequirements.PlayerState state = playerStateReader.read(transport);
        List<String> unmet = TransportRequirements.unmetReasons(transport, state);
        if (!unmet.isEmpty()) {
            return Result.unmet(usage.getType() + " unusable: " + String.join("; ", unmet));
        }

        WorldPoint before = ctx.players().local().location();
        WorldPoint destination = usage.getDestination();
        boolean teleport = usage.getType() != null && usage.getType().isTeleport();
        if (TransportArrival.skipOperating(
                before, destination, destination != null && tileService.isTileReachable(destination), teleport)) {
            log.info("Walker: {} already reachable at {}, skipping {}", destination, before, describe(usage));
            return Result.crossed();
        }

        TransportContext context = TransportContext.builder()
                .ctx(ctx)
                .transport(transport)
                .origin(usage.getOrigin())
                .destination(usage.getDestination())
                .objectInfo(ObjectInfo.parse(AlKharidGate.liveObjectInfo(transport, state)))
                .displayInfo(usage.getDisplayInfo())
                .build();

        SleepService.sleepUntil(() -> ctx.players().local().isIdle(), IDLE_BEFORE_CLICK_MS);

        log.info("Walker: operating {} ({}) from {} toward {}",
                usage.getType(), describe(usage), before, destination);

        if (!handler.execute(context)) {
            if (shape == TransportShape.GROUPING_TELEPORT || shape == TransportShape.CANOE) {
                return Result.unsupported(usage.getType() + " has no destination chooser implemented");
            }
            return Result.failed("could not operate " + usage.getType()
                    + " (" + describe(usage) + ")");
        }

        if (!awaitArrival(usage, before, transport)) {
            WorldPoint now = ctx.players().local().location();
            return Result.failed("operated " + usage.getType() + " (" + describe(usage) + ") but ended at "
                    + now + " instead of " + usage.getDestination());
        }

        return Result.crossed();
    }

    /**
     * Waits for evidence the crossing happened.
     *
     * <p>A staircase lands on the destination's floor, a shortcut can drop the player short of the
     * recorded tile, and a teleport lands them far away — any of those counts, and the walker
     * re-plans from wherever they actually are. A plane change to any other floor is a climb in the
     * wrong direction and keeps the wait polling until it times out.
     * A door that is already open is counted before the click: the destination is the
     * next tile and already reachable in the live scene. A door that opens underfoot during the wait
     * is counted the same way. A tile that is only reachable by walking around a wall is not — that
     * is what clicked Climb-into again while the first climb was still playing.</p>
     *
     * <p>Warning overlays such as the wilderness ditch often appear a tick after the click. Dismissing
     * them here, on every poll, is what lets the crossing finish instead of timing out on the south
     * bank.</p>
     */
    private boolean awaitArrival(GlobalPathfinder.TransportUsage usage, WorldPoint before, Transport transport) {
        WorldPoint destination = usage.getDestination();
        long timeout = Math.max(MIN_ARRIVAL_TIMEOUT_MS,
                (long) transport.getDuration() * Constants.GAME_TICK_LENGTH + ARRIVAL_GRACE_MS);

        return SleepService.sleepUntil(() -> {
            WarningWidgets.dismiss(ctx);

            WorldPoint now = ctx.players().local().location();
            boolean reachable = destination != null && tileService.isTileReachable(destination);
            return TransportArrival.arrived(before, now, destination, reachable);
        }, timeout);
    }

    private String describe(GlobalPathfinder.TransportUsage usage) {
        String objectInfo = usage.getObjectInfo();
        if (objectInfo != null && !objectInfo.isEmpty()) {
            return objectInfo;
        }

        String displayInfo = usage.getDisplayInfo();
        return displayInfo != null && !displayInfo.isEmpty() ? displayInfo : "no description";
    }
}
