package com.kraken.api.simulation;

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
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.RuneLite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Captures an immutable, RuneLite-compatible snapshot directly from the live game state.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SimulationSnapshotService {
    private static final int DEFAULT_NPC_RADIUS = 32;

    /**
     * Optional metadata provider for overriding NPC combat values during capture.
     */
    @FunctionalInterface
    public interface NpcMetadataProvider {
        /**
         * Resolves simulated combat metadata for an NPC.
         *
         * @param npc live npc.
         * @param composition npc composition, can be null.
         * @return metadata override, or {@code null} to keep defaults.
         */
        NpcMetadata resolve(NPC npc, NPCComposition composition);
    }

    /**
     * Captured/specified simulated combat metadata for an NPC.
     */
    @Getter
    public static final class NpcMetadata {
        private final int attackRange;
        private final int attackSpeed;
        private final NpcAttackStyle attackStyle;
        private final int maxHit;
        private final boolean collidable;
        private final boolean stopWhenLineOfSight;

        /**
         * Creates NPC simulated combat metadata.
         *
         * @param attackRange npc attack range for LoS/range checks.
         * @param attackSpeed npc attack speed in ticks.
         * @param attackStyle npc attack style for prayer recommendation.
         * @param maxHit npc simulated max hit.
         * @param collidable whether NPC blocks movement.
         * @param stopWhenLineOfSight whether npc stops pathing once LoS exists.
         */
        public NpcMetadata(
                int attackRange,
                int attackSpeed,
                NpcAttackStyle attackStyle,
                int maxHit,
                boolean collidable,
                boolean stopWhenLineOfSight
        ) {
            if (attackRange <= 0) {
                throw new IllegalArgumentException("attackRange must be > 0");
            }
            if (attackSpeed <= 0) {
                throw new IllegalArgumentException("attackSpeed must be > 0");
            }
            if (maxHit < 0) {
                throw new IllegalArgumentException("maxHit must be >= 0");
            }

            this.attackRange = attackRange;
            this.attackSpeed = attackSpeed;
            this.attackStyle = attackStyle == null ? NpcAttackStyle.UNKNOWN : attackStyle;
            this.maxHit = maxHit;
            this.collidable = collidable;
            this.stopWhenLineOfSight = stopWhenLineOfSight;
        }
    }

    /**
     * Snapshot capture options.
     */
    @Getter
    public static final class CaptureOptions {
        private final int npcRadius;
        private final NpcMetadataProvider npcMetadataProvider;
        private final Map<Integer, Integer> foodHealingByItemId;

        /**
         * Creates capture options.
         *
         * @param npcRadius npc inclusion radius. {@code <= 0} means all loaded same-plane npcs.
         * @param npcMetadataProvider optional npc metadata override provider.
         * @param foodHealingByItemId optional {@literal itemId->heal} mapping for simulation eat actions.
         */
        public CaptureOptions(
                int npcRadius,
                NpcMetadataProvider npcMetadataProvider,
                Map<Integer, Integer> foodHealingByItemId
        ) {
            this.npcRadius = npcRadius;
            this.npcMetadataProvider = npcMetadataProvider;
            this.foodHealingByItemId = sanitizePositiveCountMap(foodHealingByItemId);
        }

        /**
         * Creates default capture options.
         */
        public CaptureOptions() {
            this(DEFAULT_NPC_RADIUS, null, Map.of());
        }

        /**
         * @param npcRadius npc inclusion radius.
         * @return options with updated radius.
         */
        public CaptureOptions withNpcRadius(int npcRadius) {
            return new CaptureOptions(npcRadius, npcMetadataProvider, foodHealingByItemId);
        }

        /**
         * @param provider npc metadata provider.
         * @return options with updated metadata provider.
         */
        public CaptureOptions withNpcMetadataProvider(NpcMetadataProvider provider) {
            return new CaptureOptions(npcRadius, provider, foodHealingByItemId);
        }

        /**
         * @param foodHealingByItemId {@literal itemId->heal} mapping.
         * @return options with updated food-heal mapping.
         */
        public CaptureOptions withFoodHealingByItemId(Map<Integer, Integer> foodHealingByItemId) {
            return new CaptureOptions(npcRadius, npcMetadataProvider, foodHealingByItemId);
        }
    }

    /**
     * Captures a snapshot with default options.
     *
     * @return immutable simulation snapshot.
     */
    public static SimulationSnapshot capture() {
        return capture(new CaptureOptions());
    }

    /**
     * Captures a snapshot using a Chebyshev-distance radius from the local player.
     * Set radius {@literal <=} 0 to include all loaded NPCs on the same plane.
     *
     * @param npcRadius NPC inclusion radius in tiles.
     * @return immutable simulation snapshot.
     */
    public static SimulationSnapshot capture(int npcRadius) {
        return capture(new CaptureOptions(npcRadius, null, Map.of()));
    }

    /**
     * Captures a snapshot using an NPC metadata override provider.
     *
     * @param npcRadius NPC inclusion radius in tiles.
     * @param npcMetadataProvider metadata provider used to override default npc combat metadata.
     * @return immutable simulation snapshot.
     */
    public static SimulationSnapshot capture(int npcRadius, NpcMetadataProvider npcMetadataProvider) {
        return capture(new CaptureOptions(npcRadius, npcMetadataProvider, Map.of()));
    }

    /**
     * Captures a snapshot using explicit capture options.
     *
     * @param options capture options.
     * @return immutable simulation snapshot.
     */
    public static SimulationSnapshot capture(CaptureOptions options) {
        Context context = RuneLite.getInjector().getInstance(Context.class);
        CaptureOptions safeOptions = options == null ? new CaptureOptions() : options;
        return context.runOnClientThread(() -> captureOnClientThread(context.getClient(), safeOptions));
    }

    static SimulationSnapshot captureOnClientThread(Client client, int npcRadius) {
        return captureOnClientThread(client, new CaptureOptions(npcRadius, null, Map.of()));
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

            NpcMetadata defaults = defaultNpcMetadata(composition);
            NpcMetadata override = safeOptions.getNpcMetadataProvider() == null
                    ? null
                    : safeOptions.getNpcMetadataProvider().resolve(npc, composition);
            NpcMetadata metadata = override == null ? defaults : sanitizeNpcMetadata(override, defaults);

            npcs.add(new SimulationNpcSnapshot(
                    npc.getIndex(),
                    npc.getId(),
                    npc.getName(),
                    npcPoint,
                    size,
                    metadata.getAttackRange(),
                    metadata.getAttackSpeed(),
                    metadata.getAttackStyle(),
                    metadata.getMaxHit(),
                    metadata.isCollidable(),
                    metadata.isStopWhenLineOfSight()
            ));
        }

        npcs.sort(Comparator.comparingInt(SimulationNpcSnapshot::getIndex));

        SimulationPlayerSnapshot playerSnapshot = capturePlayerSnapshot(client, safeOptions);

        return new SimulationSnapshot(
                client.getTickCount(),
                plane,
                worldView.getBaseX(),
                worldView.getBaseY(),
                copiedFlags,
                playerPoint,
                playerSnapshot,
                npcs
        );
    }

    private static SimulationPlayerSnapshot capturePlayerSnapshot(Client client, CaptureOptions options) {
        int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHp = Math.max(1, client.getRealSkillLevel(Skill.HITPOINTS));
        Prayer activeProtectionPrayer = getActiveProtectionPrayer(client);

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
                currentHp,
                maxHp,
                activeProtectionPrayer,
                inventoryItemQuantities,
                equippedItemIds,
                options.getFoodHealingByItemId()
        );
    }

    private static NpcMetadata defaultNpcMetadata(NPCComposition composition) {
        int attackRange = resolveAttackRange(composition);
        NpcAttackStyle style = attackRange > 1 ? NpcAttackStyle.RANGED : NpcAttackStyle.MELEE;
        boolean stopWhenLos = attackRange > 1;
        return new NpcMetadata(attackRange, 4, style, 0, true, stopWhenLos);
    }

    private static NpcMetadata sanitizeNpcMetadata(NpcMetadata provided, NpcMetadata fallback) {
        int attackRange = provided.getAttackRange() > 0 ? provided.getAttackRange() : fallback.getAttackRange();
        int attackSpeed = provided.getAttackSpeed() > 0 ? provided.getAttackSpeed() : fallback.getAttackSpeed();
        int maxHit = Math.max(0, provided.getMaxHit());
        NpcAttackStyle style = provided.getAttackStyle() == null ? fallback.getAttackStyle() : provided.getAttackStyle();
        return new NpcMetadata(
                attackRange,
                attackSpeed,
                style,
                maxHit,
                provided.isCollidable(),
                provided.isStopWhenLineOfSight()
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

    private static Map<Integer, Integer> sanitizePositiveCountMap(Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
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
            return Map.of();
        }
        return Map.copyOf(normalized);
    }
}
