package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.EquipSet;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;

/**
 * Shared test gear: a plausible tribrid loadout with the warband style triangle baked into the per type damage
 * tables (as V2's gear catalogue would), and player arming.
 */
public final class Loadouts {
    private Loadouts() {
    }

    /** Sets 0 ranged / 1 magic / 2 melee, endgame calibre expected damage; triangle targets take their always max
     *  hit. */
    public static EquipSet[] tribrid(MechanicsData data) {
        int types = data.npcs.length;
        EquipSet ranged = new EquipSet(Style.RANGED, 4, 10, types);
        Arrays.fill(ranged.expectedDamage, 28);
        Arrays.fill(ranged.maxHit, 56);
        ranged.expectedDamage[data.typeIndex("warband_seer")] = 50;
        ranged.prayerBonus = 8;
        ranged.offensiveDrain = 24;

        EquipSet magic = new EquipSet(Style.MAGIC, 5, 10, types);
        Arrays.fill(magic.expectedDamage, 26);
        Arrays.fill(magic.maxHit, 52);
        magic.expectedDamage[data.typeIndex("warband_berserker")] = 48;
        magic.prayerBonus = 6;
        magic.offensiveDrain = 24;

        EquipSet melee = new EquipSet(Style.MELEE, 5, 1, types);
        Arrays.fill(melee.expectedDamage, 30);
        Arrays.fill(melee.maxHit, 60);
        melee.expectedDamage[data.typeIndex("warband_archer")] = 50;
        melee.prayerBonus = 4;
        melee.offensiveDrain = 24;

        return new EquipSet[]{ranged, magic, melee, null};
    }

    /**
     * Full supplies and vitals on a synthetic wave player. The supply load runs deeper than one inventory: the
     * engine's expected value damage channel books a guaranteed fraction of every unprayed overlap hit that live
     * accuracy rolls sometimes spare.
     */
    public static void arm(MechanicsData data, Frame frame, SimState st) {
        frame.equipSets = tribrid(data);
        frame.finishPlayer(data);
        frame.finishEquip(data);
        st.hpExpected = frame.playerMaxHp;
        st.prayerExpected = frame.playerMaxPrayer;
        st.foodCount = 30;
        st.karamCount = 12;
        st.brewDoses = 8;
        st.restoreDoses = 12;
        st.runEnergy100 = 10000;
        st.specEnergy10 = 1000;
        st.resetAccumulators();
    }
}
