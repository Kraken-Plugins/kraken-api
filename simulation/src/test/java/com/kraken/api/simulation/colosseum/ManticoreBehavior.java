package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.tick.TickContext;
import com.kraken.api.simulation.world.SimState;

/**
 * Manticore volley state machine: an uncharged manticore starts its 10 tick charge on the first tick it has the
 * player in range and sight (the charge animation comes 4 ticks later, the throw 6 after that); a charged one with
 * expired cooldown starts a 3 orb volley when it sees the player, one orb per consecutive tick, styles from its locked
 * pattern, but only one manticore may start a volley per tick (the rest are pushed back, {@link ColosseumRules}). A
 * running volley keeps firing even if sight is lost, and a charged manticore out of sight holds its volley. Prayer is
 * read per orb as it leaves and the hitsplat comes a tick later, so blocking a triple takes three prayers in pattern
 * order. Mantimayhem I doubles each unprayed orb, II adds venom, III randomizes the pattern (unknown patterns average
 * expected damage over the possible styles and take the worst case for the survival channel).
 */
public final class ManticoreBehavior extends NpcBehavior {
    final ColosseumNpc.Manticore cfg;
    private final ColosseumData col;

    ManticoreBehavior(ColosseumNpc.Manticore cfg, ColosseumData col) {
        this.cfg = cfg;
        this.col = col;
    }

    @Override
    public void attack(TickContext c, int i) {
        SimState st = c.state();
        byte[] pattern = st.npcLane[Lanes.PATTERN];
        byte[] orbs = st.npcLane[Lanes.ORBS];
        boolean canAttack = c.canAttack();
        boolean los = c.los();
        if ((st.npcFlags[i] & Lanes.FLAG_CHARGED) == 0) {
            if (canAttack && los) {
                st.npcFlags[i] |= Lanes.FLAG_CHARGED;
                st.npcCooldown[i] = (byte) cfg.chargeTicks;
            }
        } else if (orbs[i] == 0 && canAttack && los && st.npcCooldown[i] <= 0
                && (c.tickScratch & ColosseumRules.TICK_VOLLEY_STARTED) == 0) {
            orbs[i] = 3;
            st.npcCooldown[i] = (byte) c.frame().def(i).attackSpeed;
            c.tickScratch |= ColosseumRules.TICK_VOLLEY_STARTED;
        }
        if (orbs[i] > 0) {
            int orbsLeft = Math.min(3, orbs[i]);
            int patternId = pattern[i];
            int orbIndex = 3 - orbsLeft;
            launchOrb(c, st, i, patternId, orbIndex);
            c.notifyAttack(i, patternId >= 0 ? col.patternStyles[patternId][orbIndex] : -1);
            orbs[i] = (byte) (orbsLeft - 1);
        }
    }

    private void launchOrb(TickContext c, SimState st, int slot, int patternId, int orbIndex) {
        Mods mods = ColosseumFrame.mods(st.frame);
        NpcDef def = st.frame.def(slot);
        int rolls = mods.mantimayhemTier >= 1 ? 2 : 1;
        boolean venom = mods.mantimayhemTier >= 2;
        int delay = def.hitDelayTicks(0);
        if (patternId >= 0) {
            int style = col.patternStyles[patternId][orbIndex];
            c.launchStyled(slot, style, orbMax(style), rolls, delay, venom);
            return;
        }
        // Unknown pattern: below Mantimayhem III the last orb is always melee; the first two are ranged or magic with
        // expected damage averaged and worst case maxed.
        boolean mm3 = mods.mantimayhemTier >= 3;
        if (!mm3 && orbIndex == 2) {
            c.launchStyled(slot, Style.MELEE, cfg.maxHitMelee, rolls, delay, venom);
            return;
        }
        int bonus = st.frame.damage.npcMaxHitBonus;
        int n = 0;
        int sumExpected = 0;
        int maxWorst = 0;
        int factor = Hits.unprayedFactor1000(st.frame);
        for (int style = 0; style <= (mm3 ? 2 : 1); style++) {
            n++;
            // The orb is one of these styles: every one counts as launched, so a tick with an unknown orb is one a
            // single prayer cannot be sure to cover.
            st.tickStyleMask |= (byte) (1 << style);
            c.recordLaunchedWorst(style, (orbMax(style) + bonus) * rolls);
            if (st.overhead == Style.overheadFor(style)) {
                continue;
            }
            int worst = orbMax(style) + bonus;
            sumExpected += worst * factor / 1000;
            maxWorst = Math.max(maxWorst, worst);
        }
        if (maxWorst == 0) {
            return;
        }
        long hit = Hits.encode(delay, Style.TYPELESS, sumExpected / n, maxWorst, slot, 0, rolls);
        if (venom) {
            hit = Hits.applyVenom(hit);
        }
        Hits.push(st, hit);
    }

    private int orbMax(int style) {
        switch (style) {
            case Style.RANGED:
                return cfg.maxHitRanged;
            case Style.MAGIC:
                return cfg.maxHitMagic;
            default:
                return cfg.maxHitMelee;
        }
    }

    @Override
    public void onDeath(TickContext c, int slot) {
        c.state().npcLane[Lanes.ORBS][slot] = 0;
    }

    /** The charge animation, {@code chargeToThrowTicks} before the throw. */
    public boolean isVolleyTelegraph(int animId) {
        return NpcDef.contains(cfg.volleyTelegraphAnims, animId);
    }

    /** The throw animation, on the tick the first orb leaves. */
    public boolean isVolleyThrow(int animId) {
        return NpcDef.contains(cfg.volleyThrowAnims, animId);
    }
}
