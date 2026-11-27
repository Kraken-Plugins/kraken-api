package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.PackFrame;

/** The Colosseum's part of a frame: the active modifiers and the slots the pack spawns itself. */
public final class ColosseumFrame implements PackFrame {
    public Mods mods = Mods.NONE;
    public int modifierMask;
    public int modifierTiers;
    /** Bit per slot: spawns at the reinforcement tick instead of wave start (starts dead). */
    public short reinforcementBits;
    /** Bit per slot: reserved Totemic totem slot, spawned by the pack on demand. */
    public short totemPoolBits;

    /** The frame's Colosseum part, created on first use. */
    public static ColosseumFrame of(Frame f) {
        if (f.pack instanceof ColosseumFrame) {
            return (ColosseumFrame) f.pack;
        }
        ColosseumFrame cf = new ColosseumFrame();
        f.pack = cf;
        return cf;
    }

    /** The frame's active modifiers ({@link Mods#NONE} for a frame without a Colosseum part). */
    public static Mods mods(Frame f) {
        return f.pack instanceof ColosseumFrame ? ((ColosseumFrame) f.pack).mods : Mods.NONE;
    }

    public boolean reinforcement(int slot) {
        return (reinforcementBits >> slot & 1) != 0;
    }

    public boolean totemPool(int slot) {
        return (totemPoolBits >> slot & 1) != 0;
    }

    @Override
    public PackFrame copy() {
        ColosseumFrame c = new ColosseumFrame();
        c.copyFrom(this);
        return c;
    }

    @Override
    public void copyFrom(PackFrame other) {
        ColosseumFrame o = (ColosseumFrame) other;
        mods = o.mods;
        modifierMask = o.modifierMask;
        modifierTiers = o.modifierTiers;
        reinforcementBits = o.reinforcementBits;
        totemPoolBits = o.totemPoolBits;
    }
}
