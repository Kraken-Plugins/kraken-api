package plugins.colosseum.simulation.live;

import com.kraken.api.Context;
import plugins.colosseum.simulation.ColoConstants;
import plugins.colosseum.simulation.ColoCoords;
import plugins.colosseum.simulation.ColoFrame;
import plugins.colosseum.simulation.ColoGrid;
import plugins.colosseum.simulation.ColoNpcType;
import plugins.colosseum.simulation.ColoState;
import plugins.colosseum.simulation.LoadoutConfig;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;

/**
 * Builds a {@link ColoFrame} + {@link ColoState} pair from the live client without mutating
 * anything: the arena collision grid (anchored on the colosseum region), every tracked wave
 * NPC with cooldown/charge estimates from the {@link ColoWaveTracker}, and the player's
 * vitals, supplies and consumption timers.
 */
@Getter
public final class ColoCapture {
    private static final int SPECIAL_ATTACK_VARP = 300;

    private final ColoFrame frame;
    private final ColoState state;

    private ColoCapture(ColoFrame frame, ColoState state) {
        this.frame = frame;
        this.state = state;
    }

    /**
     * Captures the current game state on the client thread.
     *
     * @param ctx kraken context.
     * @param tracker wave tracker fed by the hosting plugin.
     * @param loadout player loadout configuration.
     * @param currentGearSet gear set index the player currently wears.
     * @return capture, or null when not in a capturable state (no player/collision).
     */
    public static ColoCapture capture(Context ctx, ColoWaveTracker tracker, LoadoutConfig loadout, int currentGearSet) {
        return ctx.runOnClientThread(() -> captureOnClientThread(ctx.getClient(), tracker, loadout, currentGearSet));
    }

    static ColoCapture captureOnClientThread(Client client, ColoWaveTracker tracker, LoadoutConfig loadout, int currentGearSet) {
        if (client == null) {
            return null;
        }
        WorldView worldView = client.getTopLevelWorldView();
        Player player = client.getLocalPlayer();
        if (worldView == null || player == null) {
            return null;
        }
        WorldPoint playerPoint = player.getWorldLocation();
        if (playerPoint == null) {
            return null;
        }
        int plane = worldView.getPlane();
        CollisionData[] collisionMaps = worldView.getCollisionMaps();
        if (collisionMaps == null || plane < 0 || plane >= collisionMaps.length || collisionMaps[plane] == null) {
            return null;
        }

        // Anchor a 64x64 grid on the player's region (the whole colosseum arena is one
        // region), which exactly fits the engine's 6-bit coordinate packing.
        int regionId = playerPoint.getRegionID();
        int baseX = (regionId >>> 8) << 6;
        int baseY = (regionId & 0xFF) << 6;
        ColoGrid grid = ColoGrid.fromCollisionFlags(
                collisionMaps[plane].getFlags(),
                baseX - worldView.getBaseX(),
                baseY - worldView.getBaseY(),
                64,
                64,
                baseX,
                baseY,
                plane
        );
        short playerLocal = grid.toLocal(playerPoint.getX(), playerPoint.getY());
        if (!ColoCoords.isPresent(playerLocal)) {
            return null;
        }

        // Collect tracked wave NPCs inside the grid.
        int slotCount = 0;
        ColoNpcType[] types = new ColoNpcType[ColoConstants.MAX_NPCS];
        int[] indices = new int[ColoConstants.MAX_NPCS];
        short[] maxHp = new short[ColoConstants.MAX_NPCS];
        ColoWaveTracker.NpcRecord[] slotRecords = new ColoWaveTracker.NpcRecord[ColoConstants.MAX_NPCS];
        short[] positions = new short[ColoConstants.MAX_NPCS];
        for (ColoWaveTracker.NpcRecord record : tracker.records()) {
            if (slotCount >= ColoConstants.MAX_NPCS) {
                break;
            }
            if (record.npc() == null || record.npc().getWorldLocation() == null) {
                continue;
            }
            WorldPoint npcPoint = record.npc().getWorldLocation();
            if (npcPoint.getPlane() != plane) {
                continue;
            }
            short local = grid.toLocal(npcPoint.getX(), npcPoint.getY());
            if (!ColoCoords.isPresent(local)) {
                continue;
            }
            types[slotCount] = record.getType();
            indices[slotCount] = record.getNpcIndex();
            maxHp[slotCount] = (short) record.getType().getMaxHitpoints();
            slotRecords[slotCount] = record;
            positions[slotCount] = local;
            slotCount++;
        }

        ColoNpcType[] trimmedTypes = new ColoNpcType[slotCount];
        int[] trimmedIndices = new int[slotCount];
        short[] trimmedMaxHp = new short[slotCount];
        System.arraycopy(types, 0, trimmedTypes, 0, slotCount);
        System.arraycopy(indices, 0, trimmedIndices, 0, slotCount);
        System.arraycopy(maxHp, 0, trimmedMaxHp, 0, slotCount);

        int maxHitpoints = Math.max(1, client.getRealSkillLevel(Skill.HITPOINTS));
        int prayerCap = Math.max(1, client.getRealSkillLevel(Skill.PRAYER));
        ColoFrame frame = new ColoFrame(
                grid,
                loadout,
                trimmedTypes,
                trimmedIndices,
                trimmedMaxHp,
                maxHitpoints,
                prayerCap,
                tracker.isWaveActive(),
                tracker.getWarbandCyclePhase(),
                0
        );

        ColoState state = new ColoState();
        state.reset(frame);
        state.setTick(Math.max(0, tracker.getWaveTick()));

        int spec = client.getVarpValue(SPECIAL_ATTACK_VARP) / 10;
        state.setPlayer(
                playerLocal,
                client.getBoostedSkillLevel(Skill.HITPOINTS),
                client.getBoostedSkillLevel(Skill.PRAYER),
                client.getEnergy(),
                spec,
                activeOverhead(client),
                currentGearSet
        );

        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        state.setSupplies(
                countItems(inventory, loadout.getFoodItemIds()),
                countItems(inventory, loadout.getComboItemIds()),
                countItems(inventory, loadout.getBrewItemIds()),
                countItems(inventory, loadout.getRestoreItemIds())
        );

        int weaponSpeed = loadout.gearSet(currentGearSet).getAttackSpeedTicks();
        state.setCooldowns(
                tracker.playerFoodDelayEstimate(),
                0,
                0,
                tracker.playerAttackDelayEstimate(weaponSpeed)
        );

        // Carry the live interaction into the state: if the player is already attacking a
        // tracked NPC, plans that keep fighting it need no fresh attack click.
        net.runelite.api.Actor interacting = player.getInteracting();
        if (interacting instanceof net.runelite.api.NPC) {
            int slot = frame.slotByRuneliteIndex(((net.runelite.api.NPC) interacting).getIndex());
            if (slot >= 0) {
                state.setAttackTarget(slot);
            }
        }

        for (int slot = 0; slot < slotCount; slot++) {
            ColoWaveTracker.NpcRecord record = slotRecords[slot];
            state.activateNpc(slot, positions[slot], tracker.lastKnownHp(record), tracker.cooldownEstimate(record));
            if (record.getType() == ColoNpcType.MANTICORE) {
                state.setManticoreState(
                        slot,
                        record.pattern(),
                        tracker.manticoreChargingStarted(record),
                        tracker.manticoreOrbsRemaining(record)
                );
            } else if (record.getType() == ColoNpcType.JAVELIN_COLOSSUS) {
                state.setJavelinAutos(slot, tracker.javelinAutosSinceSpecial(record));
            }
        }

        return new ColoCapture(frame, state);
    }

    private static byte activeOverhead(Client client) {
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
            return ColoState.OVERHEAD_MELEE;
        }
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)) {
            return ColoState.OVERHEAD_MISSILES;
        }
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)) {
            return ColoState.OVERHEAD_MAGIC;
        }
        return ColoState.OVERHEAD_NONE;
    }

    private static int countItems(ItemContainer container, int[] itemIds) {
        if (container == null || itemIds == null) {
            return 0;
        }
        int count = 0;
        for (Item item : container.getItems()) {
            if (item == null || item.getId() < 0) {
                continue;
            }
            for (int id : itemIds) {
                if (item.getId() == id) {
                    count += Math.max(1, item.getQuantity());
                    break;
                }
            }
        }
        return count;
    }
}
