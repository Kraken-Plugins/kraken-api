package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.core.ClientThreadException;
import com.kraken.api.core.script.ScriptStoppedException;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.handler.ClearThenWalkHandler;
import com.kraken.api.service.walker.transport.handler.DialogueTransportHandler;
import com.kraken.api.service.walker.transport.handler.FairyRingHandler;
import com.kraken.api.service.walker.transport.handler.HubDialogueHandler;
import com.kraken.api.service.walker.transport.handler.HubResumePauseHandler;
import com.kraken.api.service.walker.transport.handler.ItemTeleportHandler;
import com.kraken.api.service.walker.transport.handler.ObjectTransportHandler;
import com.kraken.api.service.walker.transport.handler.SpellTeleportHandler;
import com.kraken.api.service.walker.transport.handler.UnsupportedTransportHandler;
import com.kraken.api.service.walker.transport.handler.WalkTransportHandler;
import com.kraken.api.service.walker.transport.handler.WidgetSelectHandler;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Constants;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Operates one transport and reports whether the player got through it.
 *
 * <p>Dispatch is by {@link TransportShape} rather than by kind of transport, so more than two dozen
 * kinds are served by a handful of handlers. The {@link TransportRegistry} can supply the handler
 * instead: a registered transport may bring its own, and a handler registered for a shape takes the
 * place of the built in one.</p>
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

    private final TransportRegistry transportRegistry;

    private final Map<TransportShape, TransportHandler> handlers = new EnumMap<>(TransportShape.class);

    @Inject
    TransportExecutor(TransportRegistry transportRegistry,
                      ObjectTransportHandler objectHandler,
                      ClearThenWalkHandler clearThenWalkHandler,
                      WalkTransportHandler walkHandler,
                      DialogueTransportHandler dialogueHandler,
                      HubDialogueHandler hubDialogueHandler,
                      HubResumePauseHandler hubResumePauseHandler,
                      FairyRingHandler fairyRingHandler,
                      ItemTeleportHandler itemHandler,
                      SpellTeleportHandler spellHandler,
                      WidgetSelectHandler widgetSelectHandler,
                      UnsupportedTransportHandler unsupportedHandler) {
        this.transportRegistry = transportRegistry;
        handlers.put(TransportShape.SINGLE_CLICK, objectHandler);
        handlers.put(TransportShape.CLEAR_THEN_WALK, clearThenWalkHandler);
        handlers.put(TransportShape.WALK, walkHandler);
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
     * <p>An obstacle that is cleared and then walked through, such as a web, is always handed to
     * its handler, including when the way is already open: the handler is what walks the player
     * across, and counting an open web as crossed without moving would leave the next round planning
     * the same edge from the same tile. A step that is only walked is handed to its handler for the
     * same reason.</p>
     *
     * <p>Everything about the transport comes from its definition: its shape, what it clicks and
     * what the player needs. That is so whether the definition is part of the bundled dataset or
     * was registered by a plugin.</p>
     *
     * @param usage the transport chosen by the planner
     * @return what happened, with a readable reason when it did not work
     */
    public Result execute(GlobalPathfinder.TransportUsage usage) {
        if (usage == null) {
            return Result.unsupported("no transport to execute");
        }

        TransportDefinition definition = usage.getDefinition();
        if (definition == null) {
            return Result.unsupported("route carries no transport detail for " + usage.getType());
        }

        String label = label(usage);

        // A route can outlive the registration it was planned through. Once a plugin has closed its
        // handle, nothing of that plugin is run on its behalf.
        if (!transportRegistry.snapshot().holds(definition)) {
            return Result.failed(label + " was unregistered after the route was planned");
        }

        TransportShape shape = definition.getShape();
        TransportHandler handler = resolveHandler(definition, shape);
        if (handler == null) {
            return Result.unsupported(shape == null
                    ? "no execution shape known for " + label
                    : "no handler registered for " + shape);
        }

        List<String> unmet = unmetReasons(definition, handler);
        if (!unmet.isEmpty()) {
            return Result.unmet(label + " unusable: " + String.join("; ", unmet));
        }

        ObjectInfo objectInfo = definition.toObjectInfo();
        WorldPoint before = ctx.players().local().location();
        WorldPoint destination = usage.getDestination();
        boolean teleport = usage.isTeleport();

        TransportContext context = TransportContext.builder()
                .ctx(ctx)
                .origin(usage.getOrigin())
                .destination(destination)
                .objectInfo(objectInfo)
                .displayInfo(usage.getDisplayInfo())
                .definition(definition)
                .build();

        if (needsNoOperating(shape, before, destination, teleport)) {
            log.info("Walker: {} already reachable at {}, skipping {}", destination, before, describe(usage));
            return Result.crossed();
        }

        SleepService.sleepUntil(() -> ctx.players().local().isIdle(), IDLE_BEFORE_CLICK_MS);

        log.info("Walker: operating {} ({}) from {} toward {}",
                label, describe(usage), before, destination);

        Result notOperated = operate(handler, context, label, describe(usage));
        if (notOperated != null) {
            return notOperated;
        }

        if (!awaitArrival(usage, before)) {
            WorldPoint now = ctx.players().local().location();
            return Result.failed("operated " + label + " (" + describe(usage) + ") but ended at "
                    + now + " instead of " + usage.getDestination());
        }

        return Result.crossed();
    }

    /**
     * Reports whether a transport can be counted as crossed without operating it, because its
     * destination is the next tile and already reachable, as with a door someone left open.
     *
     * <p>That shortcut never applies to an obstacle that is cleared and then walked through, or to
     * a step that is only walked. The handler is what walks the player across, and an open web that
     * was counted as crossed while the player stood still would be planned again from the same
     * tile.</p>
     *
     * @param shape how the transport is operated, may be null
     * @param here where the player stands
     * @param destination where the transport leads, may be null
     * @param teleport true when the transport is a teleport, which is always operated
     * @return true when nothing should be clicked
     */
    boolean needsNoOperating(TransportShape shape, WorldPoint here, WorldPoint destination, boolean teleport) {
        if (shape == TransportShape.CLEAR_THEN_WALK || shape == TransportShape.WALK) {
            return false;
        }
        return TransportArrival.skipOperating(
                here, destination, destination != null && tileService.isTileReachable(destination), teleport);
    }

    /**
     * Runs a handler and turns a refusal or a fault into a result.
     *
     * <p>A handler that came from the registry is someone else's code, so an exception it throws
     * ends this crossing with a failure instead of escaping into the walk. Two exceptions always
     * pass through, whoever threw them: a stopping script has to unwind, and a client thread call
     * whose outcome is unknown must not be reported as a plain failure.</p>
     *
     * <p>A handler that could not operate the transport may have said why through
     * {@link TransportContext#fail(String)}; that reason is reported in place of the general one.</p>
     *
     * @param handler the handler to run
     * @param context the crossing to operate
     * @param label the transport's name for the reason text
     * @param description what the transport clicks, for the reason text
     * @return null when the handler operated the transport, otherwise why it did not
     */
    Result operate(TransportHandler handler, TransportContext context, String label, String description) {
        boolean operated;
        try {
            operated = handler.execute(context);
        } catch (ScriptStoppedException | ClientThreadException e) {
            throw e;
        } catch (RuntimeException e) {
            if (handlers.containsValue(handler)) {
                throw e;
            }
            log.warn("Walker: the registered handler for {} threw", label, e);
            return Result.failed("the registered handler for " + label + " threw " + e);
        }

        if (operated) {
            return null;
        }
        if (handler instanceof UnsupportedTransportHandler) {
            return Result.unsupported(label + " has no destination chooser implemented");
        }

        String reason = context.getFailureReason();
        return Result.failed(reason != null
                ? label + ": " + reason
                : "could not operate " + label + " (" + description + ")");
    }

    /**
     * Picks the handler that operates a transport.
     *
     * <p>The most specific choice wins: a registered transport's own handler, then a handler
     * registered for the whole shape, then the built in handler for the shape.</p>
     *
     * @param definition the definition of the transport, may be null
     * @param shape how the transport is operated, may be null when the definition brings a handler
     * @return the handler, or null when nothing can operate the transport
     */
    TransportHandler resolveHandler(TransportDefinition definition, TransportShape shape) {
        if (definition != null && definition.getHandler() != null) {
            return definition.getHandler();
        }
        if (shape == null) {
            return null;
        }

        TransportHandler registered = transportRegistry.snapshot().handlerFor(shape);
        return registered != null ? registered : handlers.get(shape);
    }

    /**
     * Checks a transport's requirements and its own condition against live state.
     *
     * <p>A teleport usable from anywhere is also refused above its wilderness level, and a spell
     * that the built in handler would cast is refused while the client reports it as not castable.
     * The planner applied both rules when it chose the route; the player may have moved or spent a
     * rune since.</p>
     *
     * @param definition the transport about to be operated
     * @param handler the handler that will operate it
     * @return the reasons it cannot be used right now, empty when it can
     */
    private List<String> unmetReasons(TransportDefinition definition, TransportHandler handler) {
        TransportRequirements.PlayerState state =
                playerStateReader.read(Collections.singletonList(definition.getRequirements()));
        List<String> unmet = new ArrayList<>(TransportRequirements.unmetReasons(definition.getRequirements(), state));

        if (definition.isUsableAnywhere()) {
            int wildernessLevel = WildernessLevels.of(ctx.players().local().location());
            if (!usableAtWildernessLevel(definition, wildernessLevel)) {
                unmet.add("not usable at wilderness level " + wildernessLevel);
            }
        }

        if (handler == handlers.get(TransportShape.SPELL)) {
            String refusal = SpellTeleportHandler.castRefusal(
                    ctx.getService(MagicService.class), definition.getDisplayInfo());
            if (refusal != null) {
                unmet.add(refusal);
            }
        }

        if (!unmet.isEmpty() || definition.getAvailableWhen() == null) {
            return unmet;
        }

        boolean available = Boolean.TRUE.equals(
                ctx.runOnClientThread(() -> definition.getAvailableWhen().getAsBoolean(), Boolean.FALSE));
        return available ? unmet : Collections.singletonList("its own condition no longer holds");
    }

    /**
     * Reports whether a registered teleport works at a wilderness level.
     *
     * @param definition the registered teleport
     * @param wildernessLevel the planner's wilderness bucket for where the player stands, as
     *                        {@link WildernessLevels} reports it
     * @return true when the player is not too deep in the wilderness for it
     */
    static boolean usableAtWildernessLevel(TransportDefinition definition, int wildernessLevel) {
        return wildernessLevel <= definition.getMaxWildernessLevel();
    }

    /**
     * Names a transport for logs and failure reasons by the id of its definition.
     */
    private static String label(GlobalPathfinder.TransportUsage usage) {
        return usage.getDefinition() != null
                ? usage.getDefinition().getId()
                : String.valueOf(usage.getType());
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
    private boolean awaitArrival(GlobalPathfinder.TransportUsage usage, WorldPoint before) {
        WorldPoint destination = usage.getDestination();
        long timeout = Math.max(MIN_ARRIVAL_TIMEOUT_MS,
                (long) usage.getDuration() * Constants.GAME_TICK_LENGTH + ARRIVAL_GRACE_MS);

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
