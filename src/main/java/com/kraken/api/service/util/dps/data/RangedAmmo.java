package com.kraken.api.service.util.dps.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility table describing which ammo each ranged weapon can fire, ported from the
 * OSRS wiki DPS calculator. Weapons absent from the table do not use the ammo slot at all;
 * weapons mapped to an empty list (e.g. blowpipe, crystal bow) provide their own ammo.
 */
public final class RangedAmmo {

    /** How the ammo slot interacts with the equipped weapon for the purpose of ranged bonuses. */
    public enum AmmoApplicability {
        /** Include the ammo slot's ranged accuracy and strength bonuses. */
        INCLUDED,
        /** The ammo slot item is allowed but its ranged bonuses are not applied. */
        ALLOWED,
        /** The ammo is incompatible with the weapon (the weapon cannot fire). */
        INVALID
    }

    private static final Map<Integer, List<Integer>> AMMO_FOR_WEAPON = new HashMap<>();

    private RangedAmmo() {
    }

    private static List<Integer> concat(List<Integer> base, Integer... extra) {
        List<Integer> out = new ArrayList<>(base);
        out.addAll(Arrays.asList(extra));
        return out;
    }

    static {
        List<Integer> bowT1 = Arrays.asList(
                882, 883, 5616, 5622, 598, 942, 33553, // Bronze arrow + variants
                884, 885, 5617, 5623, 2532, 2533, 33559, // Iron arrow + variants
                22227, 22228, 22229, 22230); // barb assault
        List<Integer> cbT1 = Arrays.asList(
                877, 878, 6061, 6062, 879, 9236); // Bronze bolts + variants, opal bolts + (e)
        List<Integer> javelin = Arrays.asList(
                825, 831, 5642, 5648, // Bronze javelin + variants
                826, 832, 5643, 5649, // Iron javelin + variants
                827, 833, 5644, 5650, // Steel javelin + variants
                828, 834, 5645, 5651, // Mithril javelin + variants
                829, 835, 5646, 5652, // Adamant javelin + variants
                830, 836, 5647, 5653, // Rune javelin + variants
                21318, 21320, 21322, 21324, // Amethyst javelin + variants
                19484, 19486, 19488, 19490); // Dragon javelin + variants

        List<Integer> bowT5 = concat(bowT1, 886, 887, 5618, 5624, 2534, 2535, 33565); // Steel arrow + variants
        List<Integer> bowT20 = concat(bowT5, 888, 889, 5619, 5625, 2536, 2537, 33571); // Mithril arrow + variants
        List<Integer> bowT30 = concat(bowT20, 890, 891, 5620, 5626, 2538, 2539, 33577); // Adamant arrow + variants
        List<Integer> bowT40 = concat(bowT30, 892, 893, 5621, 5627, 78, 2540, 2541, 33583); // Rune arrow + variants, ice arrows
        List<Integer> bowT50 = concat(bowT40, 21326, 21332, 21334, 21336, 4160, 21328, 21330, 33589, 33601); // Amethyst arrow + variants, broad arrows
        List<Integer> bowT60 = concat(bowT50, 11212, 11227, 11228, 11229, 11217, 11222, 33595); // Dragon arrow + variants

        List<Integer> cbT16 = concat(cbT1, 9139, 9286, 9293, 9300, 9335, 9237); // Blurite bolts + variants, jade bolts + (e)
        List<Integer> cbT26 = concat(cbT16, 9140, 9287, 9294, 9301, 880, 9238, 9145, 9292, 9299, 9306); // Iron bolts + variants, pearl bolts + (e), silver bolts
        List<Integer> cbT31 = concat(cbT26, 9141, 9288, 9295, 9302, 9336, 9239); // Steel bolts + variants, topaz bolts + (e)
        List<Integer> cbT36 = concat(cbT31, 9142, 9289, 9296, 9303, 9337, 9240, 9338, 9241); // Mithril bolts + variants, sapphire/emerald bolts + (e)
        List<Integer> cbT46 = concat(cbT36, 9143, 9290, 9297, 9304, 9339, 9242, 9340, 9243); // Adamant bolts + variants, ruby/diamond bolts + (e)
        List<Integer> cbT61 = concat(cbT46, 9144, 9291, 9298, 9305, 11875, 21316, 9341, 9244, 9342, 9245); // Runite bolts + variants, broad bolts, amethyst broad bolts, dragonstone/onyx bolts + (e)
        List<Integer> cbT64 = concat(cbT61, 21905, 21924, 21926, 21928, 21955, 21932, 21957, 21934, 21959, 21936, 21961, 21938,
                21963, 21940, 21965, 21942, 21967, 21944, 21969, 21946, 21971, 21948, 21973, 21950); // Dragon bolts + variants, gem-tipped dragon bolts

        AMMO_FOR_WEAPON.put(11708, bowT1); // Cursed goblin bow
        AMMO_FOR_WEAPON.put(23357, bowT1); // Rain bow
        AMMO_FOR_WEAPON.put(9705, Collections.singletonList(9706)); // Training bow
        AMMO_FOR_WEAPON.put(841, bowT1); // Shortbow
        AMMO_FOR_WEAPON.put(839, bowT1); // Longbow
        AMMO_FOR_WEAPON.put(843, bowT5); // Oak shortbow
        AMMO_FOR_WEAPON.put(845, bowT5); // Oak longbow
        AMMO_FOR_WEAPON.put(4236, bowT5); // Signed oak bow
        AMMO_FOR_WEAPON.put(849, bowT20); // Willow shortbow
        AMMO_FOR_WEAPON.put(847, bowT20); // Willow longbow
        AMMO_FOR_WEAPON.put(10280, bowT20); // Willow comp bow
        AMMO_FOR_WEAPON.put(853, bowT30); // Maple shortbow
        AMMO_FOR_WEAPON.put(851, bowT30); // Maple longbow
        AMMO_FOR_WEAPON.put(2883, Arrays.asList(2866, 4773, 4778, 4783, 4788, 4793)); // Ogre bow
        AMMO_FOR_WEAPON.put(4827, Arrays.asList(2866, 4773, 4778, 4783, 4788, 4793, 4798, 4803)); // Comp ogre bow
        AMMO_FOR_WEAPON.put(857, bowT40); // Yew shortbow
        AMMO_FOR_WEAPON.put(855, bowT40); // Yew longbow
        AMMO_FOR_WEAPON.put(10282, bowT40); // Yew comp bow
        AMMO_FOR_WEAPON.put(28794, bowT50); // Bone shortbow
        AMMO_FOR_WEAPON.put(6724, bowT50); // Seercull
        AMMO_FOR_WEAPON.put(861, bowT50); // Magic shortbow
        AMMO_FOR_WEAPON.put(12788, bowT50); // Magic shortbow (i)
        AMMO_FOR_WEAPON.put(859, bowT50); // Magic longbow
        AMMO_FOR_WEAPON.put(10284, bowT50); // Magic comp bow
        AMMO_FOR_WEAPON.put(11235, bowT60); // Dark bow
        AMMO_FOR_WEAPON.put(27853, bowT60); // Dark bow (bh)
        AMMO_FOR_WEAPON.put(12424, bowT60); // 3rd age bow
        AMMO_FOR_WEAPON.put(27610, bowT60); // Venator bow
        AMMO_FOR_WEAPON.put(27612, bowT60); // Venator bow (uncharged)
        AMMO_FOR_WEAPON.put(20997, bowT60); // Twisted bow
        AMMO_FOR_WEAPON.put(29591, bowT60); // Scorching bow
        AMMO_FOR_WEAPON.put(837, cbT1); // Crossbow
        AMMO_FOR_WEAPON.put(767, cbT1); // Phoenix crossbow
        AMMO_FOR_WEAPON.put(9174, cbT1); // Bronze crossbow
        AMMO_FOR_WEAPON.put(9176, cbT16); // Blurite crossbow
        AMMO_FOR_WEAPON.put(9177, cbT26); // Iron crossbow
        AMMO_FOR_WEAPON.put(9179, cbT31); // Steel crossbow
        AMMO_FOR_WEAPON.put(9181, cbT36); // Mithril crossbow
        AMMO_FOR_WEAPON.put(9183, cbT46); // Adamant crossbow
        AMMO_FOR_WEAPON.put(9185, cbT61); // Rune crossbow
        AMMO_FOR_WEAPON.put(21902, cbT64); // Dragon crossbow
        AMMO_FOR_WEAPON.put(19478, javelin); // Light ballista
        AMMO_FOR_WEAPON.put(19481, javelin); // Heavy ballista
        AMMO_FOR_WEAPON.put(8880, concat(cbT16, 9140, 9287, 9294, 9301, 8882)); // Dorgeshuun crossbow
        AMMO_FOR_WEAPON.put(10156, Arrays.asList(10158, 10159)); // Hunters' crossbow
        AMMO_FOR_WEAPON.put(4734, Collections.singletonList(4740)); // Karil's crossbow
        AMMO_FOR_WEAPON.put(21012, cbT64); // Dragon hunter crossbow
        AMMO_FOR_WEAPON.put(11785, cbT64); // Armadyl crossbow
        AMMO_FOR_WEAPON.put(26374, cbT64); // Zaryte crossbow
        AMMO_FOR_WEAPON.put(33251, cbT64); // King's barrage
        AMMO_FOR_WEAPON.put(12924, Collections.emptyList()); // Toxic blowpipe (empty)
        AMMO_FOR_WEAPON.put(12926, Collections.emptyList()); // Toxic blowpipe (charged)
        AMMO_FOR_WEAPON.put(22547, Collections.emptyList()); // Craw's bow (empty)
        AMMO_FOR_WEAPON.put(22550, Collections.emptyList()); // Craw's bow (charged)
        AMMO_FOR_WEAPON.put(23983, Collections.emptyList()); // Crystal bow (empty)
        AMMO_FOR_WEAPON.put(23985, Collections.emptyList()); // Crystal bow (inactive)
        AMMO_FOR_WEAPON.put(24123, Collections.emptyList()); // Crystal bow (new)
        AMMO_FOR_WEAPON.put(27652, Collections.emptyList()); // Webweaver bow (empty)
        AMMO_FOR_WEAPON.put(27655, Collections.emptyList()); // Webweaver bow (charged)
        AMMO_FOR_WEAPON.put(25862, Collections.emptyList()); // Bow of faerdhinen (empty)
        AMMO_FOR_WEAPON.put(25865, Collections.emptyList()); // Bow of faerdhinen (charged)
        AMMO_FOR_WEAPON.put(10149, Collections.singletonList(10142)); // Swamp lizard, guam tar
        AMMO_FOR_WEAPON.put(10146, Collections.singletonList(10143)); // Orange salamander, marrentill tar
        AMMO_FOR_WEAPON.put(10147, Collections.singletonList(10144)); // Red salamander, tarromin tar
        AMMO_FOR_WEAPON.put(10148, Collections.singletonList(10145)); // Black salamander, harralander tar
        AMMO_FOR_WEAPON.put(28834, Collections.singletonList(28837)); // Tecu salamander, irit tar
        AMMO_FOR_WEAPON.put(28869, Arrays.asList(28872, 28878)); // Hunters' sunlight crossbow
        AMMO_FOR_WEAPON.put(29000, Collections.singletonList(28991)); // Eclipse atlatl
        AMMO_FOR_WEAPON.put(33245, bowT60); // Nature's recurve
    }

    /**
     * Determines how the equipped ammo interacts with the equipped weapon.
     * @param weaponId Canonical item id of the weapon, or null when unarmed
     * @param ammoId Canonical item id of the ammo slot item, or null when empty
     * @return AmmoApplicability describing whether the ammo bonuses apply
     */
    public static AmmoApplicability applicability(Integer weaponId, Integer ammoId) {
        List<Integer> valid = AMMO_FOR_WEAPON.get(weaponId == null ? -1 : weaponId);

        // The weapon does not use ammo
        if (valid == null || valid.isEmpty()) {
            return AmmoApplicability.ALLOWED;
        }

        if (ammoId != null && valid.contains(ammoId)) {
            return AmmoApplicability.INCLUDED;
        }

        return AmmoApplicability.INVALID;
    }

    /**
     * Gets the list of ammo item ids the given weapon can fire.
     * @param weaponId Canonical item id of the weapon
     * @return List of compatible ammo ids; empty when the weapon needs no ammo; null when
     *         the weapon is not a ranged weapon that uses the ammo slot
     */
    public static List<Integer> compatibleAmmo(int weaponId) {
        List<Integer> valid = AMMO_FOR_WEAPON.get(weaponId);
        return valid == null ? null : Collections.unmodifiableList(valid);
    }
}
