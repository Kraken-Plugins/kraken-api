package com.kraken.api.service.util.dps.search;

import com.kraken.api.service.util.dps.DpsResult;
import com.kraken.api.service.util.dps.calc.DpsCalculator;
import com.kraken.api.service.util.dps.data.DpsDataStore;
import com.kraken.api.service.util.dps.data.RangedAmmo;
import com.kraken.api.service.util.dps.model.AttackStyle;
import com.kraken.api.service.util.dps.model.CombatStance;
import com.kraken.api.service.util.dps.model.CombatStyleType;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.GearCategory;
import com.kraken.api.service.util.dps.model.GearSlot;
import com.kraken.api.service.util.dps.model.Loadout;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.PrayerBoost;
import com.kraken.api.service.util.dps.model.SpellData;
import com.kraken.api.service.util.dps.model.WeaponCategory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Smart gear search: given the items a player has available (worn + inventory), finds the
 * combination that maximizes DPS against a target monster.
 *
 * <p>The search works per combat style class (melee/ranged/magic). Items are first
 * pre-filtered using their {@link GearCategory} — e.g. a Masori body with +43 ranged
 * accuracy is only considered for ranged setups — which keeps the space small. For each
 * candidate weapon the search hill-climbs slot-by-slot using full DPS evaluations, then
 * probes well-known set effects (void, slayer helmet, salve, obsidian, crystal, barrows
 * sets) whose pieces have weak individual stats and would otherwise never be selected.</p>
 */
@Slf4j
public class GearOptimizer {

    /** Slots optimized by hill climbing, in evaluation order. Weapon and ammo are fixed per iteration. */
    private static final GearSlot[] CLIMB_SLOTS = {
            GearSlot.HEAD, GearSlot.CAPE, GearSlot.NECK, GearSlot.BODY, GearSlot.SHIELD,
            GearSlot.LEGS, GearSlot.HANDS, GearSlot.FEET, GearSlot.RING
    };

    /** Items with weak or no stats whose special effects make them worth considering anyway. */
    private static final Set<String> SPECIAL_ITEM_NAMES = new HashSet<>(Arrays.asList(
            // slayer
            "Slayer helmet", "Slayer helmet (i)", "Black mask", "Black mask (i)",
            // undead / revenants
            "Salve amulet", "Salve amulet (e)", "Salve amulet(i)", "Salve amulet(ei)", "Amulet of avarice",
            // void
            "Void melee helm", "Void ranger helm", "Void mage helm",
            "Void knight top", "Void knight robe", "Void knight gloves",
            "Elite void top", "Elite void robe",
            // obsidian / tzhaar
            "Obsidian helmet", "Obsidian platebody", "Obsidian platelegs", "Berserker necklace",
            // crystal armour (bowfa/crystal bow synergy)
            "Crystal helm", "Crystal body", "Crystal legs",
            // barrows set pieces
            "Dharok's helm", "Dharok's platebody", "Dharok's platelegs",
            "Karil's coif", "Karil's leathertop", "Karil's leatherskirt",
            "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt",
            "Amulet of the damned",
            // misc special effects
            "Chaos gauntlets", "Efaritay's aid"
    ));

    /** Set effect probes: groups of items forced together after the initial climb. */
    private static final List<List<String>> PROBE_SETS = Arrays.asList(
            Arrays.asList("Void melee helm", "Void knight top", "Void knight robe", "Void knight gloves"),
            Arrays.asList("Void melee helm", "Elite void top", "Elite void robe", "Void knight gloves"),
            Arrays.asList("Void ranger helm", "Void knight top", "Void knight robe", "Void knight gloves"),
            Arrays.asList("Void ranger helm", "Elite void top", "Elite void robe", "Void knight gloves"),
            Arrays.asList("Void mage helm", "Void knight top", "Void knight robe", "Void knight gloves"),
            Arrays.asList("Void mage helm", "Elite void top", "Elite void robe", "Void knight gloves"),
            Arrays.asList("Obsidian helmet", "Obsidian platebody", "Obsidian platelegs"),
            Arrays.asList("Crystal helm", "Crystal body", "Crystal legs"),
            Arrays.asList("Dharok's helm", "Dharok's platebody", "Dharok's platelegs"),
            Arrays.asList("Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Amulet of the damned"),
            Arrays.asList("Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Amulet of the damned")
    );

    private static final int MAX_CLIMB_PASSES = 3;
    private static final int MAX_CANDIDATES_PER_SLOT = 4;

    /** Standard elemental combat spells with their base magic level requirements. */
    private static final Object[][] ELEMENTAL_SPELLS = {
            {"Wind Strike", 1}, {"Water Strike", 5}, {"Earth Strike", 9}, {"Fire Strike", 13},
            {"Wind Bolt", 17}, {"Water Bolt", 23}, {"Earth Bolt", 29}, {"Fire Bolt", 35},
            {"Wind Blast", 41}, {"Water Blast", 47}, {"Earth Blast", 53}, {"Fire Blast", 59},
            {"Wind Wave", 62}, {"Water Wave", 65}, {"Earth Wave", 70}, {"Fire Wave", 75},
            {"Wind Surge", 81}, {"Water Surge", 85}, {"Earth Surge", 90}, {"Fire Surge", 95}
    };

    private final DpsDataStore data;
    private int evaluations;

    public GearOptimizer(DpsDataStore data) {
        this.data = data;
    }

    /**
     * Runs the smart gear search.
     * @param candidates All items available to the player (worn + inventory)
     * @param monster The target monster
     * @param config Search options
     * @param currentWorn The player's currently worn gear by slot, used to compute the change set
     * @return GearSearchResult with the best loadouts found
     */
    public GearSearchResult search(List<EquipmentItem> candidates, MonsterData monster,
                                   GearSearchConfig config, Map<GearSlot, EquipmentItem> currentWorn) {
        evaluations = 0;

        // canonicalize and dedupe candidates by item id
        Map<Integer, EquipmentItem> unique = new LinkedHashMap<>();
        for (EquipmentItem item : candidates) {
            if (item == null) continue;
            EquipmentItem canonical = data.equipment(item.getId());
            EquipmentItem resolved = canonical != null ? canonical : item;
            unique.putIfAbsent(resolved.getId(), resolved);
        }
        List<EquipmentItem> all = new ArrayList<>(unique.values());

        Map<GearCategory, DpsResult> bestPerStyle = new EnumMap<>(GearCategory.class);
        DpsResult best = null;

        for (GearCategory styleClass : config.getStyles()) {
            if (styleClass != GearCategory.MELEE && styleClass != GearCategory.RANGED && styleClass != GearCategory.MAGIC) {
                continue;
            }
            DpsResult styleBest = searchStyle(styleClass, all, monster, config);
            if (styleBest != null) {
                bestPerStyle.put(styleClass, styleBest);
                if (best == null || styleBest.getDps() > best.getDps()) {
                    best = styleBest;
                }
            }
        }

        Map<GearSlot, EquipmentItem> toEquip = new EnumMap<>(GearSlot.class);
        List<GearSlot> toRemove = new ArrayList<>();
        if (best != null && currentWorn != null) {
            for (GearSlot slot : GearSlot.values()) {
                EquipmentItem target = best.getLoadout().get(slot);
                EquipmentItem worn = currentWorn.get(slot);
                int targetId = target == null ? -1 : target.getId();
                int wornId = worn == null ? -1 : data.canonicalItemId(worn.getId());
                if (targetId == wornId) continue;
                if (target == null) {
                    toRemove.add(slot);
                } else {
                    toEquip.put(slot, target);
                }
            }
        }

        return GearSearchResult.builder()
                .best(best)
                .bestPerStyle(bestPerStyle)
                .itemsToEquip(toEquip)
                .slotsToRemove(toRemove)
                .evaluations(evaluations)
                .build();
    }

    // ------------------------------------------------------------------
    // Per-style search
    // ------------------------------------------------------------------

    private DpsResult searchStyle(GearCategory styleClass, List<EquipmentItem> all,
                                  MonsterData monster, GearSearchConfig config) {
        List<EquipmentItem> weapons = new ArrayList<>();
        for (EquipmentItem item : all) {
            if (item.getGearSlot() != GearSlot.WEAPON) continue;
            GearCategory cat = item.getGearCategory();
            if (cat == styleClass || cat == GearCategory.HYBRID) {
                weapons.add(item);
            }
        }

        // rank weapons by a rough per-tick damage heuristic and cap the number fully evaluated
        weapons.sort(Comparator.comparingDouble((EquipmentItem w) -> weaponHeuristic(w, styleClass)).reversed());
        if (weapons.size() > config.getMaxWeaponsPerStyle()) {
            weapons = weapons.subList(0, config.getMaxWeaponsPerStyle());
        }
        if (weapons.isEmpty() && styleClass == GearCategory.MELEE) {
            weapons = new ArrayList<>();
            weapons.add(null); // unarmed
        }
        if (weapons.isEmpty()) {
            return null;
        }

        Map<GearSlot, List<EquipmentItem>> slotCandidates = buildSlotCandidates(styleClass, all);
        List<PrayerBoost> prayers = resolvePrayers(styleClass, config);

        DpsResult styleBest = null;
        for (EquipmentItem weapon : weapons) {
            DpsResult result = searchWeapon(weapon, styleClass, all, slotCandidates, prayers, monster, config);
            if (result != null && (styleBest == null || result.getDps() > styleBest.getDps())) {
                styleBest = result;
            }
        }
        return styleBest;
    }

    private DpsResult searchWeapon(EquipmentItem weapon, GearCategory styleClass, List<EquipmentItem> all,
                                   Map<GearSlot, List<EquipmentItem>> slotCandidates, List<PrayerBoost> prayers,
                                   MonsterData monster, GearSearchConfig config) {
        WeaponCategory category = weapon == null ? WeaponCategory.UNARMED : weapon.getWeaponCategory();

        // choose ammo: required-ammo weapons use their best compatible ammo, others leave the slot free
        EquipmentItem ammo = null;
        if (weapon != null) {
            List<Integer> compatible = RangedAmmo.compatibleAmmo(weapon.getId());
            if (compatible != null && !compatible.isEmpty()) {
                ammo = bestCompatibleAmmo(compatible, all);
                if (ammo == null) {
                    return null; // weapon has no usable ammo available
                }
            }
        }

        List<AttackStyle> styleOptions = attackStyleOptions(category, styleClass);
        if (styleOptions.isEmpty()) {
            return null;
        }

        List<SpellData> spellOptions = spellOptions(weapon, styleClass, monster, config);

        DpsResult weaponBest = null;
        for (AttackStyle attackStyle : styleOptions) {
            for (SpellData spell : spellOptions) {
                Loadout loadout = Loadout.builder()
                        .skills(config.getSkills())
                        .boosts(config.getBoosts())
                        .prayers(prayers)
                        .style(attackStyle)
                        .spell(attackStyle.getStance() != null && attackStyle.getStance().isCastStance() ? spell : null)
                        .onSlayerTask(config.isOnSlayerTask())
                        .inWilderness(config.isInWilderness())
                        .blowpipeDartId(config.getBlowpipeDartId())
                        .build();
                if (weapon != null) {
                    loadout.getEquipment().put(GearSlot.WEAPON, weapon);
                }
                if (ammo != null) {
                    loadout.getEquipment().put(GearSlot.AMMO, ammo);
                }

                // start from the best-by-heuristic item in each slot
                for (GearSlot slot : CLIMB_SLOTS) {
                    if (slot == GearSlot.SHIELD && weapon != null && weapon.isTwoHanded()) continue;
                    List<EquipmentItem> options = slotCandidates.get(slot);
                    if (options == null || options.isEmpty()) continue;
                    loadout.getEquipment().put(slot, options.get(0));
                }

                double dps = evaluate(loadout, monster);
                dps = hillClimb(loadout, slotCandidates, monster, weapon, dps, Collections.emptySet());

                // probe set effects the climb cannot discover incrementally
                for (List<String> probe : PROBE_SETS) {
                    double probed = probeSet(loadout, probe, slotCandidates, all, monster, weapon, dps);
                    if (probed > dps) {
                        dps = probed;
                    }
                }

                if (dps > 0 && (weaponBest == null || dps > weaponBest.getDps())) {
                    weaponBest = DpsResult.from(new DpsCalculator(data, loadout, monster));
                }
            }
        }
        return weaponBest;
    }

    /**
     * Iteratively improves the loadout one slot at a time until no single swap improves DPS.
     * The loadout is mutated in place to the best configuration found.
     */
    private double hillClimb(Loadout loadout, Map<GearSlot, List<EquipmentItem>> slotCandidates,
                             MonsterData monster, EquipmentItem weapon, double currentDps, Set<GearSlot> lockedSlots) {
        for (int pass = 0; pass < MAX_CLIMB_PASSES; pass++) {
            boolean improved = false;
            for (GearSlot slot : CLIMB_SLOTS) {
                if (lockedSlots.contains(slot)) continue;
                if (slot == GearSlot.SHIELD && weapon != null && weapon.isTwoHanded()) {
                    loadout.getEquipment().remove(GearSlot.SHIELD);
                    continue;
                }

                List<EquipmentItem> options = slotCandidates.getOrDefault(slot, Collections.emptyList());
                EquipmentItem original = loadout.get(slot);

                EquipmentItem bestItem = original;
                double bestDps = currentDps;

                List<EquipmentItem> withEmpty = new ArrayList<>(options);
                withEmpty.add(null);
                for (EquipmentItem option : withEmpty) {
                    if (sameItem(option, original)) continue;
                    setSlot(loadout, slot, option);
                    double dps = evaluate(loadout, monster);
                    if (dps > bestDps) {
                        bestDps = dps;
                        bestItem = option;
                    }
                }

                setSlot(loadout, slot, bestItem);
                if (!sameItem(bestItem, original)) {
                    improved = true;
                    currentDps = bestDps;
                }
            }
            if (!improved) break;
        }
        return currentDps;
    }

    /**
     * Forces a set of named items into the loadout (when all are available) and re-optimizes
     * the remaining slots. Returns the resulting DPS when it beats the current best, in which
     * case the loadout keeps the probed configuration; otherwise restores the original.
     */
    private double probeSet(Loadout loadout, List<String> setNames, Map<GearSlot, List<EquipmentItem>> slotCandidates,
                            List<EquipmentItem> all, MonsterData monster, EquipmentItem weapon, double currentDps) {
        Map<GearSlot, EquipmentItem> pieces = new EnumMap<>(GearSlot.class);
        for (String name : setNames) {
            EquipmentItem found = null;
            for (EquipmentItem item : all) {
                if (name.equals(item.getName())) {
                    found = item;
                    break;
                }
            }
            if (found == null || found.getGearSlot() == null) {
                return currentDps; // set is not fully available
            }
            pieces.put(found.getGearSlot(), found);
        }

        if (weapon != null && weapon.isTwoHanded() && pieces.containsKey(GearSlot.SHIELD)) {
            return currentDps;
        }

        Loadout snapshot = loadout.copy();
        for (Map.Entry<GearSlot, EquipmentItem> e : pieces.entrySet()) {
            loadout.getEquipment().put(e.getKey(), e.getValue());
        }

        double dps = evaluate(loadout, monster);
        dps = hillClimb(loadout, slotCandidates, monster, weapon, dps, pieces.keySet());

        if (dps > currentDps) {
            return dps;
        }

        // revert
        loadout.setEquipment(snapshot.getEquipment());
        return currentDps;
    }

    // ------------------------------------------------------------------
    // Candidate construction
    // ------------------------------------------------------------------

    private Map<GearSlot, List<EquipmentItem>> buildSlotCandidates(GearCategory styleClass, List<EquipmentItem> all) {
        Map<GearSlot, List<EquipmentItem>> bySlot = new EnumMap<>(GearSlot.class);
        for (EquipmentItem item : all) {
            GearSlot slot = item.getGearSlot();
            if (slot == null || slot == GearSlot.WEAPON || slot == GearSlot.AMMO) continue;

            GearCategory cat = item.getGearCategory();
            boolean matches = cat == styleClass || cat == GearCategory.HYBRID;
            boolean special = SPECIAL_ITEM_NAMES.contains(item.getName());
            if (!matches && !special) continue;

            bySlot.computeIfAbsent(slot, s -> new ArrayList<>()).add(item);
        }

        // rank by additive heuristic and cap the list, but always keep special items
        for (Map.Entry<GearSlot, List<EquipmentItem>> e : bySlot.entrySet()) {
            List<EquipmentItem> items = e.getValue();
            items.sort(Comparator.comparingInt((EquipmentItem i) -> armourHeuristic(i, styleClass)).reversed());
            if (items.size() > MAX_CANDIDATES_PER_SLOT) {
                List<EquipmentItem> trimmed = new ArrayList<>(items.subList(0, MAX_CANDIDATES_PER_SLOT));
                for (EquipmentItem item : items.subList(MAX_CANDIDATES_PER_SLOT, items.size())) {
                    if (SPECIAL_ITEM_NAMES.contains(item.getName())) {
                        trimmed.add(item);
                    }
                }
                e.setValue(trimmed);
            }
        }
        return bySlot;
    }

    private EquipmentItem bestCompatibleAmmo(List<Integer> compatible, List<EquipmentItem> all) {
        EquipmentItem best = null;
        for (EquipmentItem item : all) {
            if (item.getGearSlot() != GearSlot.AMMO) continue;
            if (!compatible.contains(item.getId())) continue;
            if (best == null || item.getBonuses().getRanged_str() > best.getBonuses().getRanged_str()) {
                best = item;
            }
        }
        return best;
    }

    private List<AttackStyle> attackStyleOptions(WeaponCategory category, GearCategory styleClass) {
        List<AttackStyle> options = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (AttackStyle style : category.getAttackStyles()) {
            if (style.getType() == null) continue;
            boolean matches;
            switch (styleClass) {
                case MELEE:
                    matches = style.getType().isMelee();
                    break;
                case RANGED:
                    // longrange is strictly worse than rapid for dps
                    matches = style.getType() == CombatStyleType.RANGED && style.getStance() != CombatStance.LONGRANGE;
                    break;
                case MAGIC:
                    matches = style.getType() == CombatStyleType.MAGIC
                            && style.getStance() != CombatStance.LONGRANGE
                            && style.getStance() != CombatStance.DEFENSIVE_AUTOCAST;
                    break;
                default:
                    matches = false;
            }
            if (!matches) continue;
            String key = style.getType() + "|" + style.getStance() + "|" + style.getName();
            if (seen.add(key)) {
                options.add(style);
            }
        }
        return options;
    }

    private List<SpellData> spellOptions(EquipmentItem weapon, GearCategory styleClass,
                                         MonsterData monster, GearSearchConfig config) {
        List<SpellData> none = Collections.singletonList(null);
        if (styleClass != GearCategory.MAGIC) {
            return none;
        }
        if (config.getSpell() != null) {
            return Collections.singletonList(config.getSpell());
        }

        WeaponCategory category = weapon == null ? WeaponCategory.UNARMED : weapon.getWeaponCategory();
        if (category == WeaponCategory.POWERED_STAFF || category == WeaponCategory.POWERED_WAND
                || category == WeaponCategory.SALAMANDER) {
            // built-in spell
            return none;
        }
        if (!category.isMagicWeapon()) {
            return none;
        }

        int magicLevel = config.getSkills().getMagic();
        List<SpellData> options = new ArrayList<>();

        SpellData bestGeneric = bestElementalSpell(magicLevel, null);
        if (bestGeneric != null) {
            options.add(bestGeneric);
        }

        // a weaker spell matching the monster's elemental weakness may out-damage the generic pick
        if (monster.getWeakness() != null) {
            SpellData weaknessSpell = bestElementalSpell(magicLevel, monster.getWeakness().getElement());
            if (weaknessSpell != null && (bestGeneric == null || !weaknessSpell.getName().equals(bestGeneric.getName()))) {
                options.add(weaknessSpell);
            }
        }

        return options.isEmpty() ? none : options;
    }

    private SpellData bestElementalSpell(int magicLevel, String element) {
        SpellData best = null;
        for (Object[] entry : ELEMENTAL_SPELLS) {
            String name = (String) entry[0];
            int level = (Integer) entry[1];
            if (level > magicLevel) continue;
            SpellData spell = data.spell(name);
            if (spell == null) continue;
            if (element != null && !element.equals(spell.getElement())) continue;
            if (best == null || data.spellMaxHit(spell, magicLevel) >= data.spellMaxHit(best, magicLevel)) {
                best = spell;
            }
        }
        return best;
    }

    private List<PrayerBoost> resolvePrayers(GearCategory styleClass, GearSearchConfig config) {
        if (!config.isUseBestPrayers()) {
            return config.getPrayers() == null ? new ArrayList<>() : config.getPrayers();
        }
        switch (styleClass) {
            case MELEE:
                return Collections.singletonList(PrayerBoost.PIETY);
            case RANGED:
                return Collections.singletonList(PrayerBoost.RIGOUR);
            case MAGIC:
                return Collections.singletonList(PrayerBoost.AUGURY);
            default:
                return new ArrayList<>();
        }
    }

    // ------------------------------------------------------------------
    // Heuristics and helpers
    // ------------------------------------------------------------------

    private double weaponHeuristic(EquipmentItem weapon, GearCategory styleClass) {
        int speed = Math.max(1, weapon.getSpeed());
        switch (styleClass) {
            case MELEE: {
                int acc = Math.max(weapon.getOffensive().getStab(),
                        Math.max(weapon.getOffensive().getSlash(), weapon.getOffensive().getCrush()));
                return (acc + 2.0 * weapon.getBonuses().getStr()) / speed;
            }
            case RANGED:
                return (weapon.getOffensive().getRanged() + 2.0 * weapon.getBonuses().getRanged_str() + 40) / Math.max(1, speed - 1);
            case MAGIC:
                // powered staves carry implicit max hits; favour magic accuracy and damage
                return (weapon.getOffensive().getMagic() + 2.0 * weapon.getBonuses().getMagic_str() / 10.0)
                        / (weapon.getWeaponCategory().isMagicWeapon() && weapon.getSpeed() > 0 ? speed : 5);
            default:
                return 0;
        }
    }

    private int armourHeuristic(EquipmentItem item, GearCategory styleClass) {
        switch (styleClass) {
            case MELEE:
                return Math.max(item.getOffensive().getStab(),
                        Math.max(item.getOffensive().getSlash(), item.getOffensive().getCrush()))
                        + 2 * item.getBonuses().getStr();
            case RANGED:
                return item.getOffensive().getRanged() + 2 * item.getBonuses().getRanged_str();
            case MAGIC:
                return item.getOffensive().getMagic() + item.getBonuses().getMagic_str() / 2;
            default:
                return 0;
        }
    }

    private boolean sameItem(EquipmentItem a, EquipmentItem b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.getId() == b.getId();
    }

    private void setSlot(Loadout loadout, GearSlot slot, EquipmentItem item) {
        if (item == null) {
            loadout.getEquipment().remove(slot);
        } else {
            loadout.getEquipment().put(slot, item);
        }
    }

    private double evaluate(Loadout loadout, MonsterData monster) {
        evaluations++;
        try {
            return new DpsCalculator(data, loadout, monster).getDps();
        } catch (Exception e) {
            log.debug("DPS evaluation failed for loadout", e);
            return 0;
        }
    }
}
