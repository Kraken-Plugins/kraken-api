package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.world.SimState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * What the Colosseum keeps in the core's lanes and pack flags. Per NPC lanes (byte, {@code SimState.npcLane}):
 *
 * <pre>
 * manticore   lane 0 volley pattern id (-1 unknown)      lane 1 orbs left in the running volley (3..1, 0 idle)
 * javelin     lane 0 attacks since the last toss (-1 unknown)
 * bee swarm   lane 0 crawl counter                       lane 1 ticks until respawn (0 none)
 * totem       lane 0 beneficiary slot (-1 none)          lane 1 ticks until the next heal cast
 * </pre>
 *
 * Pack flags: {@link #FLAG_CHARGED} (a manticore has locked its volley style) and {@link #FLAG_TOTEM_SPAWNED} (an NPC
 * already spawned its Totemic totem). World lanes ({@code SimState.packLane}): {@link #SOLAR} the Solarflare clock
 * plus one (0 = no flares), {@link #ALIVE_TOTEMS}. Accumulator lanes ({@code SimState.accLane}): bee kills, totem
 * kills, minotaur heals, totem ticks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Lanes {
    public static final int PATTERN = 0;
    public static final int ORBS = 1;
    public static final int JAVELIN_COUNT = 0;
    public static final int BEE_CRAWL = 0;
    public static final int BEE_RESPAWN = 1;
    public static final int TOTEM_BENEFICIARY = 0;
    public static final int TOTEM_HEAL = 1;

    public static final byte FLAG_CHARGED = SimState.FLAG_PACK_0;
    public static final byte FLAG_TOTEM_SPAWNED = SimState.FLAG_PACK_1;

    public static final int SOLAR = 0;
    public static final int ALIVE_TOTEMS = 1;

    public static final int ACC_BEE_KILLS = 0;
    public static final int ACC_TOTEM_KILLS = 1;
    public static final int ACC_MINOTAUR_HEALS = 2;
    public static final int ACC_TOTEM_TICKS = 3;

    /** The Solarflare clock (ring index | ticks to the next step {@code << 5}), or -1 when no flares run. */
    public static int solar(SimState st) {
        return st.packLane[SOLAR] - 1;
    }

    public static void setSolar(SimState st, int packed) {
        st.packLane[SOLAR] = packed + 1;
    }
}
