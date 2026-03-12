package com.kraken.api.simulation.snapshot;

import com.kraken.api.Context;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.RuneLite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures immutable snapshots from live RuneLite state.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimulationSnapshotService {
    private static final int DEFAULT_NPC_RADIUS = 32;

    /**
     * Capture options.
     */
    @Getter
    public static final class CaptureOptions {
        private final int npcRadius;

        /**
         * Creates options for how the game state is captured from the Cliwnr.
         *
         * @param npcRadius npc inclusion radius, or {@literal <=0} for all loaded same-plane npcs.
         */
        public CaptureOptions(int npcRadius) {
            this.npcRadius = npcRadius;
        }

        /**
         * Creates default options.
         */
        public CaptureOptions() {
            this(DEFAULT_NPC_RADIUS);
        }

        /**
         * @param npcRadius npc inclusion radius.
         * @return copied options.
         */
        public CaptureOptions withNpcRadius(int npcRadius) {
            return new CaptureOptions(npcRadius);
        }
    }

    /**
     * Captures with defaults.
     *
     * @return immutable snapshot.
     */
    public static SimulationSnapshot capture() {
        return capture(new CaptureOptions());
    }

    /**
     * Captures with explicit npc radius.
     *
     * @param npcRadius npc radius.
     * @return immutable snapshot.
     */
    public static SimulationSnapshot capture(int npcRadius) {
        return capture(new CaptureOptions(npcRadius));
    }

    /**
     * Captures with explicit options.
     *
     * @param options options.
     * @return immutable snapshot.
     */
    public static SimulationSnapshot capture(CaptureOptions options) {
        Context context = RuneLite.getInjector().getInstance(Context.class);
        CaptureOptions safeOptions = options == null ? new CaptureOptions() : options;
        return context.runOnClientThread(() -> captureOnClientThread(context.getClient(), safeOptions));
    }

    static SimulationSnapshot captureOnClientThread(Client client, int npcRadius) {
        return captureOnClientThread(client, new CaptureOptions(npcRadius));
    }

    static SimulationSnapshot captureOnClientThread(Client client, CaptureOptions options) {
        if (client == null) {
            throw new IllegalStateException("RuneLite client is not available");
        }

        CaptureOptions safeOptions = options == null ? new CaptureOptions() : options;
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
        int effectiveRadius = safeOptions.getNpcRadius() <= 0 ? Integer.MAX_VALUE : safeOptions.getNpcRadius();

        List<SimulationNpcSnapshot> npcs = new ArrayList<>();
        for (NPC npc : worldView.npcs()) {
            if (npc == null) {
                continue;
            }
            WorldPoint npcPoint = npc.getWorldLocation();
            if (npcPoint == null || npcPoint.getPlane() != plane) {
                continue;
            }
            int distance = Math.max(
                    Math.abs(npcPoint.getX() - playerPoint.getX()),
                    Math.abs(npcPoint.getY() - playerPoint.getY())
            );
            if (distance > effectiveRadius) {
                continue;
            }

            NPCComposition composition = npc.getComposition();
            int size = composition != null && composition.getSize() > 0 ? composition.getSize() : 1;
            npcs.add(new SimulationNpcSnapshot(
                    npc.getIndex(),
                    npc.getId(),
                    size,
                    npcPoint
            ));
        }
        npcs.sort(Comparator.comparingInt(SimulationNpcSnapshot::getIndex));

        SimulationPlayerSnapshot playerSnapshot = capturePlayerSnapshot(client, playerPoint, safeOptions);
        return new SimulationSnapshot(
                client.getTickCount(),
                plane,
                worldView.getBaseX(),
                worldView.getBaseY(),
                copiedFlags,
                playerSnapshot,
                npcs
        );
    }

    private static SimulationPlayerSnapshot capturePlayerSnapshot(Client client, WorldPoint playerPoint, CaptureOptions options) {
        int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHp = Math.max(1, client.getRealSkillLevel(Skill.HITPOINTS));
        Prayer activePrayer = getActiveProtectionPrayer(client);

        Map<Integer, Integer> inventoryItemQuantities = new HashMap<>();
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory != null) {
            for (Item item : inventory.getItems()) {
                if (item == null || item.getId() < 0 || item.getQuantity() <= 0) {
                    continue;
                }
                inventoryItemQuantities.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
        }

        Set<Integer> equippedItemIds = new HashSet<>();
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        if (equipment != null) {
            for (Item item : equipment.getItems()) {
                if (item == null || item.getId() < 0) {
                    continue;
                }
                equippedItemIds.add(item.getId());
            }
        }

        return new SimulationPlayerSnapshot(
                playerPoint,
                currentHp,
                maxHp,
                activePrayer,
                inventoryItemQuantities,
                equippedItemIds
        );
    }

    private static Prayer getActiveProtectionPrayer(Client client) {
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
            return Prayer.PROTECT_FROM_MELEE;
        }
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)) {
            return Prayer.PROTECT_FROM_MISSILES;
        }
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)) {
            return Prayer.PROTECT_FROM_MAGIC;
        }
        return null;
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

    private static Map<Integer, Integer> sanitizePositiveCountMap(Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> normalized = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : source.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
            if (key == null || key < 0 || value == null || value <= 0) {
                continue;
            }
            normalized.put(key, value);
        }
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(normalized);
    }
}
