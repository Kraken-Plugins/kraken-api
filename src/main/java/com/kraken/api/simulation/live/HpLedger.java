package com.kraken.api.simulation.live;

import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;

/**
 * NPC hit point tracking: hp = max minus the damage hitsplats plus the heal hitsplats, clamped to [1, max] while
 * alive, and re anchored against the client's health bar ratio whenever the ledger drifts outside the ratio's
 * resolution.
 */
public final class HpLedger {
    private final int[] damage = new int[SimState.MAX_NPCS];
    private final int[] heals = new int[SimState.MAX_NPCS];

    /**
     * Books one NPC hitsplat.
     * @param slot the NPC's slot, ignored when negative
     * @param amount the splat's amount
     * @param heal whether the splat heals
     */
    public void onNpcHitsplat(int slot, int amount, boolean heal) {
        if (slot < 0) {
            return;
        }
        if (heal) {
            heals[slot] += amount;
        } else {
            damage[slot] += amount;
        }
    }

    /**
     * Current hp estimate for a slot. When the health bar contradicts the ledger by more than one ratio step the
     * ledger re anchors to the bar, absorbing lost or misattributed hitsplats.
     * @param slot the NPC's slot
     * @param maxHp the type's max hp
     * @param healthRatio the client's health ratio, -1 when no bar shows
     * @param healthScale the client's health scale, -1 when no bar shows
     * @return the hp estimate, 0 when the bar reads empty
     */
    public int npcHp(int slot, int maxHp, int healthRatio, int healthScale) {
        int ledger = clamp(maxHp - damage[slot] + heals[slot], 1, maxHp);
        if (healthRatio >= 0 && healthScale > 1) {
            if (healthRatio == 0) {
                return 0;
            }
            int barHp = Math.max(1, (int) Math.round((double) healthRatio * maxHp / healthScale));
            int step = Math.max(1, maxHp / healthScale);
            if (Math.abs(ledger - barHp) > step + 1) {
                damage[slot] = maxHp - barHp + heals[slot];
                return barHp;
            }
        }
        return ledger;
    }

    /**
     * Forgets a slot's splats.
     * @param slot the slot
     */
    public void clearSlot(int slot) {
        damage[slot] = 0;
        heals[slot] = 0;
    }

    /** Forgets every slot's splats. */
    public void reset() {
        Arrays.fill(damage, 0);
        Arrays.fill(heals, 0);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
