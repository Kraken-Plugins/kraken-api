package com.kraken.api.service.util.dps.calc;

import com.kraken.api.service.util.dps.model.MonsterAttributeType;
import com.kraken.api.service.util.dps.model.MonsterData;

/**
 * Applies pre-fight defence reductions (dragon warhammer, bandos godsword, etc.) to a
 * monster, ported from the OSRS wiki DPS calculator's DefenceReduction.ts. Raid-specific
 * stat scaling (CoX/ToB/ToA party scaling) is not supported.
 */
public final class MonsterScaling {

    private MonsterScaling() {
    }

    /**
     * Gets the minimum defence level the monster's defence can be drained to.
     * @param m The monster
     * @return int the defence floor
     */
    public static int getDefenceFloor(MonsterData m) {
        int id = m.getId();
        if (DpsConstants.VERZIK_IDS.contains(id) || DpsConstants.VARDORVIS_IDS.contains(id)) {
            return m.getSkills().getDef();
        }
        if (DpsConstants.SOTETSEG_IDS.contains(id)) return 100;
        if (DpsConstants.NIGHTMARE_IDS.contains(id)) return 120;
        if (DpsConstants.AKKHA_IDS.contains(id)) return 70;
        if (DpsConstants.BABA_IDS.contains(id)) return 60;
        if (DpsConstants.KEPHRI_UNSHIELDED_IDS.contains(id) || DpsConstants.KEPHRI_SHIELDED_IDS.contains(id)) return 60;
        if (DpsConstants.ZEBAK_IDS.contains(id)) return 50;
        if (DpsConstants.P3_WARDEN_IDS.contains(id)) return 120;
        if (DpsConstants.TOA_OBELISK_IDS.contains(id)) return 60;
        if (DpsConstants.NEX_IDS.contains(id)) return 250;
        if (DpsConstants.ARAXXOR_IDS.contains(id)) return 90;
        if (DpsConstants.HUEYCOATL_IDS.contains(id)) return 120;
        if (DpsConstants.YAMA_IDS.contains(id)) return 145;
        return 0;
    }

    /**
     * Applies the monster's configured defence reductions in place.
     * @param m The monster to reduce (mutated)
     */
    public static void applyDefenceReductions(MonsterData m) {
        MonsterData.DefenceReductions r = m.getDefenceReductions();
        if (r == null) return;

        MonsterData.Skills skills = m.getSkills();
        int baseAtk = skills.getAtk();
        int baseStr = skills.getStr();
        int baseDef = skills.getDef();
        int defenceFloor = getDefenceFloor(m);

        if (r.isAccursed()) {
            skills.setDef(Math.max(defenceFloor, skills.getDef() * 17 / 20));
            skills.setMagic(Math.max(0, skills.getMagic() * 17 / 20));
        } else if (r.isVulnerability()) {
            skills.setDef(Math.max(defenceFloor, skills.getDef() * 9 / 10));
        }

        for (int i = 0; i < r.getElderMaul(); i++) {
            skills.setDef(Math.max(defenceFloor, skills.getDef() - skills.getDef() * 35 / 100));
        }
        for (int i = 0; i < r.getDwh(); i++) {
            skills.setDef(Math.max(defenceFloor, skills.getDef() - skills.getDef() * 3 / 10));
        }

        boolean demon = m.hasAttribute(MonsterAttributeType.DEMON);
        applyArclight(skills, r.getArclight(), demon ? 2 : 1, baseAtk, baseStr, baseDef, defenceFloor);
        applyArclight(skills, r.getEmberlight(), demon ? 3 : 1, baseAtk, baseStr, baseDef, defenceFloor);

        for (int i = 0; i < r.getTonalztic(); i++) {
            skills.setDef(Math.max(defenceFloor, skills.getDef() - skills.getMagic() / 8));
        }

        if (r.getSeercull() > 0) {
            skills.setMagic(Math.max(0, skills.getMagic() - r.getSeercull()));
        }

        int bgsDmg = r.getBgs();
        if (bgsDmg > 0) {
            // order matters: def, str, atk, magic, ranged; damage only propagates when a stat hits 0
            int[] remaining = {bgsDmg};
            skills.setDef(applyBgs(skills.getDef(), defenceFloor, remaining));
            skills.setStr(applyBgs(skills.getStr(), 0, remaining));
            skills.setAtk(applyBgs(skills.getAtk(), 0, remaining));
            skills.setMagic(applyBgs(skills.getMagic(), 0, remaining));
            skills.setRanged(applyBgs(skills.getRanged(), 0, remaining));
        }

        if (r.getAyak() > 0 && m.getDefensive().getMagic() > 0) {
            m.getDefensive().setMagic(Math.max(0, m.getDefensive().getMagic() - r.getAyak()));
        }
    }

    private static void applyArclight(MonsterData.Skills skills, int iter, int num, int baseAtk, int baseStr, int baseDef, int defenceFloor) {
        if (iter == 0) return;
        skills.setAtk(Math.max(0, skills.getAtk() - (iter * (num * baseAtk / 20 + 1))));
        skills.setStr(Math.max(0, skills.getStr() - (iter * (num * baseStr / 20 + 1))));
        skills.setDef(Math.max(defenceFloor, skills.getDef() - (iter * (num * baseDef / 20 + 1))));
    }

    private static int applyBgs(int startLevel, int floor, int[] remaining) {
        if (remaining[0] <= 0) return startLevel;
        int newLevel = Math.max(floor, startLevel - remaining[0]);
        if (newLevel > 0) {
            // if a skill fails to drain to 0, even due to a floor, the bgs does not propagate further
            remaining[0] = 0;
        } else {
            remaining[0] -= startLevel;
        }
        return newLevel;
    }
}
