package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.walker.WalkResult;
import com.kraken.api.service.walker.Walker;
import com.kraken.api.service.walker.WalkerConfig;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import plugins.api.precondition.Waiter;
import plugins.api.requirements.BankState;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.suite.CancellationToken;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.NamedLocation;

import java.util.*;

/**
 * Live F2P tile checks from Varrock Square through Lumbridge's Death's Office and back outside.
 * No items or levels are required. Movement uses independent scene-coordinate destinations and
 * requires exact observed arrival; packet dispatch alone cannot pass a movement check.
 */
@Slf4j
@Singleton
public class TileServiceTest extends BaseApiTest {
    // An approach in the graveyard, not the entrance object's occupied tile. Resolve the live object
    // by name/action after arrival so object IDs and the instanced scene base are not hardcoded.
    private static final WorldPoint GRAVEYARD = new WorldPoint(3240, 3208, 0);
    private static final int SAMPLE_RADIUS = 4;
    private static final long MOVE_TIMEOUT_MS = 15_000;
    private static final long TRANSITION_TIMEOUT_MS = 30_000;
    private static final long TRAVEL_TIMEOUT_MS = 240_000;
    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    @Inject private TileService tiles;
    @Inject private MovementService movement;
    @Inject private Walker walker;
    @Inject private DialogueService dialogue;
    @Inject private Waiter waiter;

    /**
     * Declares the F2P starting point and sufficient time for travel and instance checks.
     * @return non-destructive requirements that leave inventory and equipment alone
     */
    @Override
    public TestRequirements requirements() {
        return TestRequirements.builder()
                .location(NamedLocation.LUMBRIDGE_GRAVEYARD)
                .bankState(BankState.CLOSED)
                .sideEffect(SideEffect.MOVES_PLAYER)
                .sideEffect(SideEffect.TELEPORTS)
                .timeoutMs(420_000)
                .orderHint(100)
                .build();
    }

    /** @return the Services-panel display name */
    @Override
    public String getTestName() {
        return "Tile Service";
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        CancellationToken cancellation = new CancellationToken();
        if (!assertTrue(ctx.runOnClientThread(() -> ready(ctx, false)),
                "Tile test requires a logged-in player outside an instance")) return false;

        boolean passed = checkScene(ctx, false, "Lumbridge");
        passed &= checkMovement(ctx, false, cancellation, "Lumbridge");

        WalkerConfig travel = WalkerConfig.builder().timeoutMillis(TRAVEL_TIMEOUT_MS)
                .pathfinderConfig(GlobalPathfinderConfig.builder()
                        .avoidWilderness(true).useCanoes(false).useTeleportationMinigames(false)
                        .useTeleportationSpells(false).useTeleportationItems(false).build())
                .build();
        log.info("Tile test: walking to Lumbridge graveyard at {}", GRAVEYARD);
        WalkResult arrival = walker.walkTo(GRAVEYARD, travel);
        if (!assertTrue(arrival.isSuccess(), "Tile test travel failed: " + arrival.getReason())) return false;

        boolean exitPassed = false;
        try {
            boolean entered = ctx.runOnClientThread(() -> ctx.gameObjects()
                    .withName("Death's Domain").withAction("Enter").sortByDistance().interact("Enter"));
            if (!assertTrue(entered, "No Death's Domain entrance with Enter action near Lumbridge graveyard")) return false;
            if (!assertTrue(waiter.until(() -> ctx.runOnClientThread(() -> ready(ctx, true)),
                    TRANSITION_TIMEOUT_MS, cancellation, "entering Death's Office"),
                    "Entry did not produce a loaded instance")) return false;
            if (!settleDialogue(ctx, cancellation)) return false;

            passed &= checkScene(ctx, true, "Death's Office");
            passed &= checkMovement(ctx, true, cancellation, "Death's Office");
        } finally {
            // Respect Stop: do not clear the interrupt or issue fresh movement after cancellation.
            if (!Thread.currentThread().isInterrupted() && !cancellation.isCancelled()) {
                exitPassed = leaveOffice(ctx, cancellation);
            } else {
                log.info("Tile test cancelled; no cleanup actions sent. Use the office Portal if still inside.");
            }
        }
        passed &= exitPassed;
        if (exitPassed) {
            passed &= checkScene(ctx, false, "Lumbridge after instance exit");
            passed &= checkMovement(ctx, false, cancellation, "Lumbridge after instance exit");
        }
        log.info("Tile test finished outside Death's Office: {}", passed ? "PASSED" : "FAILED");
        return passed;
    }

    private boolean ready(Context ctx, boolean instance) {
        Client client = ctx.getClient();
        WorldView view = client.getTopLevelWorldView();
        return client.getGameState() == GameState.LOGGED_IN && view != null
                && view.isInstance() == instance && client.getLocalPlayer() != null
                && client.getLocalPlayer().getLocalLocation() != null && view.getScene() != null
                && view.getCollisionMaps() != null;
    }

    private boolean checkScene(Context ctx, boolean instance, String phase) {
        return ctx.runOnClientThread(() -> {
            if (!assertTrue(ready(ctx, instance), phase + ": scene is unavailable or has the wrong instance state")) return false;
            WorldView view = ctx.getClient().getTopLevelWorldView();
            LocalPoint local = ctx.getClient().getLocalPlayer().getLocalLocation();
            WorldPoint scenePoint = WorldPoint.fromLocal(view, local.getX(), local.getY(), view.getPlane());
            WorldPoint template = templatePoint(view, local);
            Tile actualTile = view.getScene().getTiles()[view.getPlane()][local.getSceneX()][local.getSceneY()];
            log.info("Tile test [{}]: view={} base=({}, {}) plane={} local={} scene={} template={}",
                    phase, view.getId(), view.getBaseX(), view.getBaseY(), view.getPlane(), local, scenePoint, template);

            boolean passed = assertNotNull(actualTile, phase + ": player scene tile is missing");
            passed &= assertTrue(actualTile != null && tiles.getTile(scenePoint) == actualTile,
                    phase + ": getTile(WorldPoint) must address the live scene tile");
            // The x/y overload has no template-plane argument. Verify it only where the source
            // template plane matches the active scene plane, and report that coverage explicitly.
            if (template.getPlane() == view.getPlane()) {
                LocalPoint first = WorldPoint.toLocalInstance(view, template).stream()
                        .filter(point -> point.getPlane() == view.getPlane()).map(point -> LocalPoint.fromWorld(view, point))
                        .filter(Objects::nonNull).findFirst().orElse(null);
                Tile expected = first == null ? null : view.getScene().getTiles()[view.getPlane()][first.getSceneX()][first.getSceneY()];
                passed &= assertTrue(expected != null && tiles.getTile(template.getX(), template.getY()) == expected,
                        phase + ": getTile(x,y) must map to the first matching scene tile");
            } else {
                log.info("Tile test [{}]: getTile(x,y) cannot express template plane {}; WorldPoint overload tested",
                        phase, template.getPlane());
            }
            passed &= checkConversions(view, local, scenePoint, template, phase);
            passed &= assertTrue(tiles.isTileReachable(template), phase + ": player's tile must be reachable");
            passed &= assertTrue(!tiles.isTileReachable(null) && !tiles.isObjectReachable(null), phase + ": null reachability");
            passed &= assertTrue(tiles.getTile(new WorldPoint(view.getBaseX() - 1, view.getBaseY() - 1, view.getPlane())) == null,
                    phase + ": out-of-scene tile lookup must return null");
            if (!instance) {
                passed &= assertTrue(!tiles.isTileReachable(template.dz(1)), phase + ": another plane is unreachable");
            }
            passed &= checkFlood(template, phase);
            passed &= checkObjects(ctx, phase);
            for (int distance : new int[]{0, 1, 4, 104}) {
                passed &= assertEquals(distance * Perspective.LOCAL_TILE_SIZE, TileService.worldToLocalDistance(distance),
                        phase + ": world/local distance scaling");
                passed &= assertEquals(distance, TileService.localToWorldDistance(TileService.worldToLocalDistance(distance)),
                        phase + ": distance round trip");
            }
            return passed;
        });
    }

    private boolean checkConversions(WorldView view, LocalPoint local, WorldPoint scenePoint,
                                     WorldPoint template, String phase) {
        boolean passed = assertEquals(template, tiles.fromInstance(scenePoint), phase + ": scene-to-template conversion");
        List<WorldPoint> expected = new ArrayList<>(WorldPoint.toLocalInstance(view, template));
        List<WorldPoint> actual = tiles.toInstance(template);
        passed &= assertEquals(new HashSet<>(expected), new HashSet<>(actual), phase + ": all template occurrences");
        passed &= assertTrue(actual.contains(scenePoint), phase + ": round trip must include the original scene position");
        LocalPoint expectedFirst = expected.stream().filter(point -> point.getPlane() == view.getPlane())
                .map(point -> LocalPoint.fromWorld(view, point)).filter(Objects::nonNull).findFirst().orElse(null);
        passed &= assertEquals(expectedFirst, tiles.fromWorldInstance(template), phase + ": first active-plane occurrence");
        for (WorldPoint occurrence : actual) {
            passed &= assertEquals(template, tiles.fromInstance(occurrence), phase + ": occurrence round trip " + occurrence);
        }
        if (view.isInstance()) {
            WorldPoint absent = new WorldPoint(0, 0, 0);
            if (WorldPoint.toLocalInstance(view, absent).isEmpty()) {
                passed &= assertTrue(tiles.toInstance(absent).isEmpty(), phase + ": absent template must have no occurrences");
                passed &= assertNull(tiles.fromWorldInstance(absent), phase + ": absent template must have no local point");
            }
            log.info("Tile test [{}]: checked {} occurrence(s); local={} template={}", phase, actual.size(), local, template);
        }
        return passed;
    }

    private boolean checkFlood(WorldPoint origin, String phase) {
        boolean passed = true;
        for (boolean ignoreCollision : new boolean[]{false, true}) {
            Map<WorldPoint, Integer> zero = tiles.getReachableTilesFromTile(origin, 0, ignoreCollision);
            passed &= assertEquals(Collections.singletonMap(origin, 0), zero, phase + ": zero-radius flood");
            Map<WorldPoint, Integer> flood = tiles.getReachableTilesFromTile(origin, SAMPLE_RADIUS, ignoreCollision);
            passed &= assertEquals(0, flood.get(origin), phase + ": flood includes origin at distance zero");
            passed &= assertTrue(flood.size() > 1, phase + ": flood must expand beyond its seed");
            passed &= assertTrue(flood.size() <= 1 + 2 * SAMPLE_RADIUS * (SAMPLE_RADIUS + 1), phase + ": flood cardinal bound");
            for (Map.Entry<WorldPoint, Integer> entry : flood.entrySet()) {
                WorldPoint point = entry.getKey();
                int distance = entry.getValue();
                passed &= assertTrue(point.getPlane() == origin.getPlane() && distance >= 0 && distance <= SAMPLE_RADIUS,
                        phase + ": flood distance/plane for " + point);
                if (distance > 0) {
                    boolean predecessor = Arrays.stream(DIRECTIONS).anyMatch(direction ->
                            Objects.equals(flood.get(point.dx(direction[0]).dy(direction[1])), distance - 1));
                    passed &= assertTrue(predecessor, phase + ": flood tile lacks a predecessor: " + point);
                }
            }
            log.info("Tile test [{}]: flood ignoreCollision={} returned {} tiles", phase, ignoreCollision, flood.size());
        }
        return passed;
    }

    private boolean checkObjects(Context ctx, String phase) {
        List<GameObject> objects = ctx.gameObjects().within(12).sortByDistance().take(8).stream()
                .map(entity -> entity.raw()).collect(java.util.stream.Collectors.toList());
        boolean passed = assertTrue(!objects.isEmpty(), phase + ": no nearby objects to test composition/reachability");
        int reachable = 0;
        for (GameObject object : objects) {
            ObjectComposition expected = ctx.getClient().getObjectDefinition(object.getId());
            if (expected != null && expected.getImpostorIds() != null && expected.getImpostor() != null) expected = expected.getImpostor();
            ObjectComposition actual = tiles.getObjectComposition(object);
            passed &= assertTrue(expected != null && actual != null && expected.getId() == actual.getId(),
                    phase + ": object composition " + object.getId());
            boolean canReach = tiles.isObjectReachable(object);
            if (canReach) reachable++;
            log.info("Tile test [{}]: object={} name={} bounds={}..{} orientation={} reachable={}", phase,
                    object.getId(), actual == null ? null : actual.getName(), object.getSceneMinLocation(),
                    object.getSceneMaxLocation(), object.getOrientation(), canReach);
        }
        passed &= assertTrue(reachable > 0, phase + ": no nearby object has a reachable footprint or cardinal approach");
        return passed;
    }

    private boolean checkMovement(Context ctx, boolean instance, CancellationToken cancellation, String phase) {
        boolean passed = true;
        for (boolean localOverload : new boolean[]{false, true}) {
            MoveTarget target = ctx.runOnClientThread(() -> chooseTarget(ctx, instance));
            if (!assertNotNull(target, phase + ": need a clear three-tile cardinal walk")) return false;
            passed &= ctx.runOnClientThread(() -> assertTrue(tiles.isTileReachable(target.getTemplate()),
                    phase + ": independently walkable destination must be reachable"));
            log.info("Tile test [{}]: moveTo({}) from {} to scene={} template={}", phase,
                    localOverload ? "LocalPoint" : "WorldPoint", target.getStart(), target.getScenePoint(), target.getTemplate());
            ctx.runOnClientThread(() -> {
                if (!ready(ctx, instance)) throw new IllegalStateException("Scene changed before test movement");
                if (localOverload) movement.moveTo(target.getLocal());
                else movement.moveTo(target.getTemplate());
            });
            boolean arrived = waiter.until(() -> ctx.runOnClientThread(() -> ready(ctx, instance)
                            && target.getScenePoint().equals(ctx.getClient().getLocalPlayer().getWorldLocation())),
                    MOVE_TIMEOUT_MS, cancellation, phase + " exact movement arrival");
            passed &= assertTrue(arrived, phase + ": movement did not arrive at " + target.getScenePoint());
            if (!arrived) return false;
            // Reading again after movement tests cache inputs changing within a loaded scene.
            passed &= ctx.runOnClientThread(() -> tiles.isTileReachable(target.getTemplate()));
        }
        return passed;
    }

    /** Chooses a straight walk using RuneLite collision checks, independently of TileService. */
    MoveTarget chooseTarget(Context ctx, boolean instance) {
        if (!ready(ctx, instance)) return null;
        WorldView view = ctx.getClient().getTopLevelWorldView();
        WorldPoint start = ctx.getClient().getLocalPlayer().getWorldLocation();
        for (int[] direction : DIRECTIONS) {
            WorldPoint destination = start;
            boolean clear = true;
            for (int step = 0; step < 3; step++) {
                if (!destination.toWorldArea().canTravelInDirection(view, direction[0], direction[1])) {
                    clear = false;
                    break;
                }
                destination = destination.dx(direction[0]).dy(direction[1]);
            }
            if (!clear) continue;
            LocalPoint local = LocalPoint.fromWorld(view, destination);
            if (local == null) continue;
            WorldPoint template = templatePoint(view, local);
            // moveTo(template) addresses the first occurrence. Avoid an ambiguous repeated chunk;
            // all occurrences are still checked by checkConversions.
            WorldPoint first = WorldPoint.toLocalInstance(view, template).stream()
                    .filter(point -> point.getPlane() == view.getPlane()).findFirst().orElse(null);
            if (destination.equals(first)) return new MoveTarget(start, destination, template, local);
        }
        return null;
    }

    private WorldPoint templatePoint(WorldView view, LocalPoint local) {
        return view.isInstance() ? WorldPoint.fromLocalInstance(view.getScene(), local, view.getPlane())
                : WorldPoint.fromLocal(view, local.getX(), local.getY(), view.getPlane());
    }

    private boolean settleDialogue(Context ctx, CancellationToken cancellation) throws InterruptedException {
        long started = System.nanoTime();
        while (System.nanoTime() - started < java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(TRANSITION_TIMEOUT_MS)) {
            cancellation.throwIfCancelled("closing Death's Office dialogue");
            boolean closed = ctx.runOnClientThread(() -> {
                if (!dialogue.isDialoguePresent()) return true;
                if (dialogue.getDialogueOptions().isEmpty()) dialogue.continueDialogue();
                else if (dialogue.isOptionPresent("I think I'm done here.")) dialogue.selectOption("I think I'm done here.");
                return false;
            });
            if (closed) return true;
            // Dialogue advancement is an action, not a side-effect-free Waiter predicate.
            Thread.sleep(600);
        }
        return assertTrue(false, "Death's dialogue did not close. Complete any first-death tutorial manually, then rerun.");
    }

    private boolean leaveOffice(Context ctx, CancellationToken cancellation) throws InterruptedException {
        if (ctx.runOnClientThread(() -> ready(ctx, false))) return true;
        if (!ctx.runOnClientThread(() -> ready(ctx, true))) return false;
        if (!settleDialogue(ctx, cancellation)) return false;
        boolean sent = ctx.runOnClientThread(() -> ctx.gameObjects().withName("Portal")
                .withAction("Use").sortByDistance().interact("Use"));
        if (!assertTrue(sent, "Death's Office exit Portal/Use could not be dispatched")) return false;
        if (!assertTrue(waiter.until(() -> ctx.runOnClientThread(() -> ready(ctx, false)),
                TRANSITION_TIMEOUT_MS, cancellation, "leaving Death's Office"),
                "Portal did not return to a loaded non-instance scene")) return false;
        return ctx.runOnClientThread(() -> assertTrue(
                ctx.getClient().getLocalPlayer().getWorldLocation().distanceTo(GRAVEYARD) <= 30,
                "Death's Office exit returned somewhere other than Lumbridge graveyard"));
    }

    @Value
    static class MoveTarget {
        WorldPoint start;
        WorldPoint scenePoint;
        WorldPoint template;
        LocalPoint local;
    }
}
