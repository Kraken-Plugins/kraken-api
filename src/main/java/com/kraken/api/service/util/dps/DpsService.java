package com.kraken.api.service.util.dps;

import com.kraken.api.Context;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.util.dps.calc.DpsCalculator;
import com.kraken.api.service.util.dps.data.DpsDataStore;
import com.kraken.api.service.util.dps.model.AttackStyle;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.GearCategory;
import com.kraken.api.service.util.dps.model.GearSlot;
import com.kraken.api.service.util.dps.model.Loadout;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.PlayerSkills;
import com.kraken.api.service.util.dps.model.PrayerBoost;
import com.kraken.api.service.util.dps.model.WeaponCategory;
import com.kraken.api.service.util.dps.search.GearOptimizer;
import com.kraken.api.service.util.dps.search.GearSearchConfig;
import com.kraken.api.service.util.dps.search.GearSearchResult;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Old School RuneScape DPS calculator service. Ports the OSRS wiki DPS calculator's
 * algorithm (accuracy rolls, max hits, gear special effects, and full hit
 * distributions) so plugins can compute accurate damage-per-second values and
 * automatically pick the best gear against a target NPC.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * // DPS of the player's current setup against a goblin under attack
 * DpsResult result = dpsService.calculate(dpsService.currentLoadout(), goblinNpc);
 *
 * // Search worn + inventory gear for the loadout that kills the target fastest
 * GearSearchResult best = dpsService.findBestGear(targetNpc);
 * best.getItemsToEquip().values().forEach(item -> log.info("equip {}", item.getName()));
 * }</pre>
 *
 * <p>Not modelled: weapon special attacks, raid party scaling (CoX/ToB/ToA
 * invocations), and multi-phase boss mechanics.</p>
 */
@Slf4j
@Singleton
public class DpsService {

    @Inject
    private Context ctx;

    @Inject
    private DpsDataStore data;

    /**
     * Looks up a monster's combat stats by npc id.
     * @param npcId The npc id
     * @return MonsterData or null when the npc is unknown
     */
    public MonsterData monster(int npcId) {
        return data.monster(npcId);
    }

    /**
     * Looks up a monster's combat stats by name (case-insensitive). When multiple
     * versions exist the first is returned; use {@link #monstersByName(String)} for all.
     * @param name The monster name, e.g. "Goblin" or "Zulrah"
     * @return MonsterData or null when unknown
     */
    public MonsterData monster(String name) {
        List<MonsterData> found = data.monstersByName(name);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Looks up all versions of a monster by name.
     * @param name The monster name
     * @return List of matching monsters, possibly empty
     */
    public List<MonsterData> monstersByName(String name) {
        return data.monstersByName(name);
    }

    /**
     * Resolves a live NPC to its combat stats, matching by npc id first and
     * falling back to a name lookup.
     * @param npc The NPC entity
     * @return MonsterData or null when the NPC has no known stats
     */
    public MonsterData monster(NpcEntity npc) {
        if (npc == null) return null;
        MonsterData m = data.monster(npc.getId());
        if (m != null) return m;
        return monster(npc.getName());
    }

    /**
     * Looks up an item's combat stats by item id (cosmetic/locked variants resolve
     * to their base item).
     * @param itemId The item id
     * @return EquipmentItem or null when the item is not equippable
     */
    public EquipmentItem item(int itemId) {
        return data.equipment(itemId);
    }

    /**
     * Classifies an item as melee, ranged, magic, hybrid or no-offence gear based on
     * its stats. For example the Masori body's +43 ranged accuracy and negative magic
     * bonus classify it as RANGED gear.
     * @param itemId The item id
     * @return GearCategory or NONE when the item is unknown
     */
    public GearCategory categorize(int itemId) {
        EquipmentItem item = data.equipment(itemId);
        return item == null ? GearCategory.NONE : item.getGearCategory();
    }

    /**
     * Builds a loadout from the player's current in-game state: worn equipment,
     * real/boosted skill levels, active offensive prayers and the selected attack style.
     * @return Loadout snapshot of the player's current setup
     */
    public Loadout currentLoadout() {
        Map<GearSlot, EquipmentItem> equipment = currentEquipment();

        Client client = ctx.getClient();
        PlayerSkills skills = ctx.runOnClientThread(() -> PlayerSkills.builder()
                .attack(client.getRealSkillLevel(Skill.ATTACK))
                .strength(client.getRealSkillLevel(Skill.STRENGTH))
                .defence(client.getRealSkillLevel(Skill.DEFENCE))
                .ranged(client.getRealSkillLevel(Skill.RANGED))
                .magic(client.getRealSkillLevel(Skill.MAGIC))
                .hitpoints(client.getRealSkillLevel(Skill.HITPOINTS))
                .prayer(client.getRealSkillLevel(Skill.PRAYER))
                .mining(client.getRealSkillLevel(Skill.MINING))
                .build());

        PlayerSkills boosts = ctx.runOnClientThread(() -> PlayerSkills.builder()
                .attack(client.getBoostedSkillLevel(Skill.ATTACK) - client.getRealSkillLevel(Skill.ATTACK))
                .strength(client.getBoostedSkillLevel(Skill.STRENGTH) - client.getRealSkillLevel(Skill.STRENGTH))
                .defence(client.getBoostedSkillLevel(Skill.DEFENCE) - client.getRealSkillLevel(Skill.DEFENCE))
                .ranged(client.getBoostedSkillLevel(Skill.RANGED) - client.getRealSkillLevel(Skill.RANGED))
                .magic(client.getBoostedSkillLevel(Skill.MAGIC) - client.getRealSkillLevel(Skill.MAGIC))
                .hitpoints(client.getBoostedSkillLevel(Skill.HITPOINTS) - client.getRealSkillLevel(Skill.HITPOINTS))
                .prayer(client.getBoostedSkillLevel(Skill.PRAYER) - client.getRealSkillLevel(Skill.PRAYER))
                .mining(client.getBoostedSkillLevel(Skill.MINING) - client.getRealSkillLevel(Skill.MINING))
                .build());

        List<PrayerBoost> prayers = activePrayers();
        AttackStyle style = currentAttackStyle(equipment.get(GearSlot.WEAPON));

        return Loadout.builder()
                .equipment(equipment)
                .skills(skills)
                .boosts(boosts)
                .prayers(prayers)
                .style(style)
                .build();
    }

    /**
     * Reads the player's currently worn equipment mapped to gear slots.
     * @return Map of gear slot to the item's combat stats
     */
    public Map<GearSlot, EquipmentItem> currentEquipment() {
        Map<GearSlot, EquipmentItem> equipment = new EnumMap<>(GearSlot.class);
        for (EquipmentEntity entity : ctx.equipment().inInterface().list()) {
            if (entity == null || entity.raw() == null) continue;
            EquipmentInventorySlot rlSlot = entity.getSlot();
            GearSlot slot = GearSlot.fromRuneLite(rlSlot);
            if (slot == null) continue;
            EquipmentItem item = data.equipment(entity.getId());
            if (item != null) {
                equipment.put(slot, item);
            }
        }
        return equipment;
    }

    /**
     * Collects every equippable item the player has available — both worn gear and
     * wieldable/wearable items in the inventory — with full combat stats.
     * @return List of available equipment
     */
    public List<EquipmentItem> availableGear() {
        List<EquipmentItem> items = new ArrayList<>();
        for (EquipmentEntity entity : ctx.equipment().all().list()) {
            if (entity == null || entity.raw() == null) continue;
            EquipmentItem item = data.equipment(entity.getId());
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private List<PrayerBoost> activePrayers() {
        return ctx.runOnClientThread(() -> {
            List<PrayerBoost> active = new ArrayList<>();
            for (PrayerBoost boost : PrayerBoost.values()) {
                try {
                    Prayer prayer = Prayer.valueOf(boost.name());
                    if (ctx.getClient().isPrayerActive(prayer)) {
                        active.add(boost);
                    }
                } catch (IllegalArgumentException ignored) {
                    // prayer not present in this RuneLite api version
                }
            }
            return active;
        });
    }

    private AttackStyle currentAttackStyle(EquipmentItem weapon) {
        WeaponCategory category = weapon == null ? WeaponCategory.UNARMED : weapon.getWeaponCategory();
        List<AttackStyle> styles = category.getAttackStyles();
        int index = ctx.getVarpValue(VarPlayer.ATTACK_STYLE);
        if (index < 0 || index >= styles.size()) {
            return styles.get(0);
        }
        return styles.get(index);
    }

    /**
     * Creates a calculator for full access to intermediate values (attack roll, hit
     * distribution, etc.).
     * @param loadout The player loadout
     * @param monster The target monster
     * @return DpsCalculator ready to query
     */
    public DpsCalculator calculator(Loadout loadout, MonsterData monster) {
        return new DpsCalculator(data, loadout, monster);
    }

    /**
     * Calculates DPS and related combat metrics for a loadout against a monster.
     * @param loadout The player loadout to evaluate
     * @param monster The target monster
     * @return DpsResult with dps, max hit, accuracy, and time-to-kill
     */
    public DpsResult calculate(Loadout loadout, MonsterData monster) {
        return DpsResult.from(new DpsCalculator(data, loadout, monster));
    }

    /**
     * Calculates DPS for a loadout against an npc id.
     * @param loadout The player loadout to evaluate
     * @param npcId The target npc id
     * @return DpsResult or null when the npc has no known combat stats
     */
    public DpsResult calculate(Loadout loadout, int npcId) {
        MonsterData m = data.monster(npcId);
        if (m == null) {
            log.warn("No monster data found for npc id {}", npcId);
            return null;
        }
        return calculate(loadout, m);
    }

    /**
     * Calculates DPS for a loadout against a live NPC.
     * @param loadout The player loadout to evaluate
     * @param npc The target NPC
     * @return DpsResult or null when the NPC has no known combat stats
     */
    public DpsResult calculate(Loadout loadout, NpcEntity npc) {
        MonsterData m = monster(npc);
        if (m == null) {
            log.warn("No monster data found for npc {}", npc == null ? null : npc.getName());
            return null;
        }
        return calculate(loadout, m);
    }

    /**
     * Calculates the DPS of the player's current in-game setup against a live NPC.
     * @param npc The target NPC
     * @return DpsResult or null when the NPC has no known combat stats
     */
    public DpsResult calculateCurrent(NpcEntity npc) {
        return calculate(currentLoadout(), npc);
    }

    /**
     * Searches the player's worn and inventory gear for the highest-DPS setup against
     * a live NPC, using the player's current skill levels, boosts and active prayers.
     * @param npc The target NPC
     * @return GearSearchResult or null when the NPC has no known combat stats
     */
    public GearSearchResult findBestGear(NpcEntity npc) {
        MonsterData m = monster(npc);
        if (m == null) {
            log.warn("No monster data found for npc {}", npc == null ? null : npc.getName());
            return null;
        }
        return findBestGear(m, configFromCurrentState());
    }

    /**
     * Searches the player's worn and inventory gear for the highest-DPS setup against
     * an npc id, using the player's current skill levels, boosts and active prayers.
     * @param npcId The target npc id
     * @return GearSearchResult or null when the npc has no known combat stats
     */
    public GearSearchResult findBestGear(int npcId) {
        MonsterData m = data.monster(npcId);
        if (m == null) {
            log.warn("No monster data found for npc id {}", npcId);
            return null;
        }
        return findBestGear(m, configFromCurrentState());
    }

    /**
     * Searches the player's worn and inventory gear for the highest-DPS setup against
     * a monster with explicit search options.
     * @param monster The target monster
     * @param config Search options (skills, prayers, slayer task, styles to search)
     * @return GearSearchResult with the best loadout found and the swaps needed to reach it
     */
    public GearSearchResult findBestGear(MonsterData monster, GearSearchConfig config) {
        return findBestGear(availableGear(), currentEquipment(), monster, config);
    }

    /**
     * Runs the smart gear search over an explicit list of candidate items. This overload
     * is fully offline — it does not query the client — making it useful for planning
     * loadouts (e.g. from the bank) or for unit tests.
     * @param candidates All items to consider
     * @param currentWorn Currently worn gear used to compute the change set, may be null
     * @param monster The target monster
     * @param config Search options
     * @return GearSearchResult with the best loadout found
     */
    public GearSearchResult findBestGear(List<EquipmentItem> candidates, Map<GearSlot, EquipmentItem> currentWorn,
                                         MonsterData monster, GearSearchConfig config) {
        long start = System.currentTimeMillis();
        GearSearchResult result = new GearOptimizer(data).search(candidates, monster, config, currentWorn);
        log.info("Gear search vs {} evaluated {} loadouts in {}ms", monster.getName(),
                result.getEvaluations(), System.currentTimeMillis() - start);
        return result;
    }

    private GearSearchConfig configFromCurrentState() {
        Loadout current = currentLoadout();
        return GearSearchConfig.builder()
                .skills(current.getSkills())
                .boosts(current.getBoosts())
                .prayers(current.getPrayers())
                .build();
    }
}
