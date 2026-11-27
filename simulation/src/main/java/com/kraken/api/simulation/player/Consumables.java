package com.kraken.api.simulation.player;

import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Eating and drinking, in the enforced order food, then one potion, then karambwan, so the classic same tick combo
 * eats legally and heals apply before this tick's hits land.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Consumables {

    /**
     * Applies the consumables a command asks for, each when its delay has run out and a dose is carried.
     * @param st the state
     * @param f the state's frame
     * @param cons {@code Command.CONSUME_*} bits
     */
    public static void consume(SimState st, Frame f, int cons) {
        if ((cons & Command.CONSUME_FOOD) != 0 && st.foodDelay <= 0 && st.foodCount > 0 && f.foodDef != null) {
            heal(st, f.foodDef.heal, f.overhealCap);
            st.foodDelay = (byte) f.foodDef.delayTicks;
            st.attackCooldown += (byte) f.foodDef.attackPushback;
            st.foodCount--;
            st.accSuppliesUsed += 1;
        }
        if ((cons & Command.CONSUME_BREW) != 0 && st.potDelay <= 0 && st.brewDoses > 0 && f.brewDef != null) {
            heal(st, f.brewDef.heal, 0);
            st.potDelay = (byte) f.brewDef.delayTicks;
            st.brewDoses--;
            st.accSuppliesUsed += 1 << 16;
        } else if ((cons & Command.CONSUME_RESTORE) != 0 && st.potDelay <= 0 && st.restoreDoses > 0
                && f.restoreDef != null) {
            st.prayerExpected = (short) Math.min(f.playerMaxPrayer, st.prayerExpected + f.restoreDef.prayerRestore);
            st.potDelay = (byte) f.restoreDef.delayTicks;
            st.restoreDoses--;
            st.accSuppliesUsed += 1 << 24;
        }
        if ((cons & Command.CONSUME_KARAMBWAN) != 0 && st.karamDelay <= 0 && st.karamCount > 0 && f.karamDef != null) {
            heal(st, f.karamDef.heal, 0);
            st.karamDelay = (byte) f.karamDef.delayTicks;
            st.attackCooldown += (byte) f.karamDef.attackPushback;
            st.karamCount--;
            st.accSuppliesUsed += 1 << 8;
        }
    }

    private static void heal(SimState st, int amount, int overhealCap) {
        int cap = Math.max(st.hpExpected, st.frame.playerMaxHp + overhealCap);
        st.hpExpected = (short) Math.min(st.hpExpected + amount, cap);
    }
}
