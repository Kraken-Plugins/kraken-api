package com.kraken.api.simulation;

import com.kraken.api.Context;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Captures an immutable, RuneLite-compatible snapshot directly from the live game state.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimulationSnapshotService {
    private static final int DEFAULT_NPC_RADIUS = 32;

    /**
     * Captures a snapshot with the default NPC inclusion radius.
     *
     * @return immutable simulation snapshot.
     */
    public static SimulationSnapshot capture() {
        return capture(DEFAULT_NPC_RADIUS);
    }

    /**
     * Captures a snapshot using a Chebyshev-distance radius from the local player.
     * Set radius {@literal <=} 0 to include all loaded NPCs on the same plane.
     *
     * @param npcRadius NPC inclusion radius in tiles.
     * @return immutable simulation snapshot.
     */
    public static SimulationSnapshot capture(int npcRadius) {
        Context context = RuneLite.getInjector().getInstance(Context.class);
        return context.runOnClientThread(() -> captureOnClientThread(context.getClient(), npcRadius));
    }

    static SimulationSnapshot captureOnClientThread(Client client, int npcRadius) {
        if (client == null) {
            throw new IllegalStateException("RuneLite client is not available");
        }

        WorldView worldView = client.getTopLevelWorldView();
        Player localPlayer = client.getLocalPlayer();
        if (worldView == null || localPlayer == null) {
            throw new IllegalStateException("Cannot capture simulation snapshot without worldView and localPlayer");
        }

        int plane = worldView.getPlane();
        WorldPoint playerPoint = localPlayer.getWorldLocation();
        if (playerPoint == null) {
            throw new IllegalStateException("Local player world point is null");
        }

        CollisionData[] collisionMaps = worldView.getCollisionMaps();
        if (collisionMaps == null || plane < 0 || plane >= collisionMaps.length || collisionMaps[plane] == null) {
            throw new IllegalStateException("No collision map loaded for plane " + plane);
        }

        int[][] copiedFlags = deepCopyFlags(collisionMaps[plane].getFlags());
        int effectiveRadius = npcRadius <= 0 ? Integer.MAX_VALUE : npcRadius;

        List<SimulationNpcSnapshot> npcs = new ArrayList<>();
        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (npc == null) {
                continue;
            }

            WorldPoint npcPoint = npc.getWorldLocation();
            if (npcPoint == null || npcPoint.getPlane() != plane) {
                continue;
            }

            int chebyshevDistance = Math.max(
                    Math.abs(npcPoint.getX() - playerPoint.getX()),
                    Math.abs(npcPoint.getY() - playerPoint.getY())
            );
            if (chebyshevDistance > effectiveRadius) {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(worldView, npcPoint);
            if (localPoint == null) {
                continue;
            }

            int sceneX = localPoint.getSceneX();
            int sceneY = localPoint.getSceneY();
            if (sceneX < 0 || sceneY < 0 || sceneX >= copiedFlags.length || sceneY >= copiedFlags[sceneX].length) {
                continue;
            }

            NPCComposition composition = npc.getComposition();
            int size = composition != null && composition.getSize() > 0 ? composition.getSize() : 1;
            int attackRange = resolveAttackRange(composition);
            boolean stopWhenLos = attackRange > 1;

            npcs.add(new SimulationNpcSnapshot(
                    npc.getIndex(),
                    npc.getId(),
                    npc.getName(),
                    npcPoint,
                    size,
                    attackRange,
                    true,
                    stopWhenLos
            ));
        }

        npcs.sort(Comparator.comparingInt(SimulationNpcSnapshot::getIndex));

        return new SimulationSnapshot(
                client.getTickCount(),
                plane,
                worldView.getBaseX(),
                worldView.getBaseY(),
                copiedFlags,
                playerPoint,
                npcs
        );
    }

    private static int resolveAttackRange(NPCComposition composition) {
        if (composition == null) {
            return 1;
        }

        // RuneLite composition int 13 is commonly used for attack range in many NPC defs.
        int rangedValue = composition.getIntValue(13);
        if (rangedValue > 0 && rangedValue <= 15) {
            return rangedValue;
        }
        return 1;
    }

    private static int[][] deepCopyFlags(int[][] source) {
        if (source == null || source.length == 0) {
            throw new IllegalArgumentException("Collision map flags cannot be empty");
        }
        int[][] copied = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copied[i] = source[i].clone();
        }
        return copied;
    }
}
