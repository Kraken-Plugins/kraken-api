package com.kraken.api.simulation.tick;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Footprints;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.Scratch;
import com.kraken.api.simulation.world.SimState;

/**
 * What a hook sees of the tick being advanced: the state, its frame and data, the thread's scratch, the tick
 * number, the player's start of tick tile, the current NPC's spawn gates and sight, and the core's launch helpers.
 * The engine owns one context and reuses it for every tick, so hooks must not keep a reference past their call.
 */
public final class TickContext {
    private final Engine engine;
    // Package private: the engine sets these directly every tick and every NPC turn (calls here cost throughput).
    SimState st;
    Frame frame;
    MechanicsData data;
    int tick;
    int playerX;
    int playerY;
    boolean canMove;
    boolean canGainLos;
    boolean canAttack;
    boolean los;

    /** Free for the pack within one tick (a rule across NPCs of the tick); zeroed at the start of every tick. */
    public int tickScratch;

    TickContext(Engine engine) {
        this.engine = engine;
    }

    /**
     * The state being advanced.
     * @return the state
     */
    public SimState state() {
        return st;
    }

    /**
     * The state's frame.
     * @return the frame
     */
    public Frame frame() {
        return frame;
    }

    /**
     * The loaded tables.
     * @return the data
     */
    public MechanicsData data() {
        return data;
    }

    /**
     * The engine's working memory.
     * @return the scratch
     */
    public Scratch scratch() {
        return engine.scratch();
    }

    /**
     * The wave tick being simulated.
     * @return the tick
     */
    public int tick() {
        return tick;
    }

    /**
     * The player's x at the start of the tick, the tile NPCs judge this tick. It is set when the NPC turns begin, so
     * {@code PackRules.onTickStart} and the hooks run while the player's hits land on NPCs ({@code onNpcDamaged}, and
     * {@code onDeath} and {@code onBodyDeleted} for those kills) still see the previous tick's value; read
     * {@code SimState.playerPos} there.
     * @return the x
     */
    public int playerX() {
        return playerX;
    }

    /**
     * The player's y at the start of the tick, set when the NPC turns begin (see {@link #playerX}).
     * @return the y
     */
    public int playerY() {
        return playerY;
    }

    /**
     * Whether the current NPC's first step gate is open. Set for each living NPC after its {@code onTurnStart}, so it
     * is only meaningful in {@code NpcBehavior.turn} and {@code attack}.
     * @return true when it may move
     */
    public boolean canMove() {
        return canMove;
    }

    /**
     * Whether the current NPC's hold on sight gate is open; only meaningful in {@code NpcBehavior.turn} and
     * {@code attack}.
     * @return true when sight may hold it
     */
    public boolean canGainLos() {
        return canGainLos;
    }

    /**
     * Whether the current NPC's attack gate is open; only meaningful in {@code NpcBehavior.turn} and {@code attack}.
     * @return true when it may attack
     */
    public boolean canAttack() {
        return canAttack;
    }

    /**
     * Whether the current NPC has reach and sight of the player's start of tick tile after the core's movement (read
     * in {@code NpcBehavior.attack}).
     * @return true with reach and sight
     */
    public boolean los() {
        return los;
    }

    /**
     * The core's standard attack: when the attack gate is open, the NPC has reach and sight, and its cooldown has run
     * out, it resets the cooldown and launches one styled hit of the type's max hit and rolls, landing after the
     * type's hit delay rule at the current distance.
     * @param slot the attacker's slot
     */
    public void standardAttack(int slot) {
        if (!canAttack || !los || st.npcCooldown[slot] > 0) {
            return;
        }
        NpcDef def = frame.def(slot);
        st.npcCooldown[slot] = (byte) def.attackSpeed;
        launchStyled(slot, def.styleCode, def.maxHit, def.rolls, hitDelay(slot), false);
        notifyAttack(slot, def.styleCode);
    }

    /**
     * Launch resolved NPC attack: the overhead active right now decides the damage, so a fully blocked launch never
     * enters the ring. The frame's max hit bonus adjusts both channels; this tick's per style worst and style mask
     * record the launch either way.
     * @param slot the attacker's slot
     * @param style the attack's style
     * @param maxHit per roll max hit before modifiers
     * @param rolls damage rolls
     * @param delay ticks until it lands
     * @param venomOnUnprayed whether an unprayed hit venoms the player
     */
    public void launchStyled(int slot, int style, int maxHit, int rolls, int delay, boolean venomOnUnprayed) {
        st.tickStyleMask |= (byte) (1 << style);
        int worst = maxHit + frame.damage.npcMaxHitBonus;
        recordLaunchedWorst(style, worst * rolls);
        if (st.overhead == Style.overheadFor(style)) {
            missedFlickRisk(worst * rolls);
            return;
        }
        int expected = worst * Hits.unprayedFactor1000(frame) / 1000;
        long hit = Hits.encode(delay, style, expected, worst, slot, 0, rolls);
        if (venomOnUnprayed) {
            hit = Hits.applyVenom(hit);
        }
        Hits.push(st, hit);
    }

    /**
     * Adds launched worst case damage to this tick's per style totals (ranged, magic, everything else as melee).
     * @param style the style
     * @param worst the worst case damage
     */
    public void recordLaunchedWorst(int style, int worst) {
        switch (style) {
            case Style.RANGED:
                st.tickRangedWorst += (short) worst;
                break;
            case Style.MAGIC:
                st.tickMagicWorst += (short) worst;
                break;
            default:
                st.tickMeleeWorst += (short) worst;
        }
    }

    /**
     * A hit the overhead blocks only because the command switched the prayer this tick is a flick that lands late
     * if the click does: its worst counts toward this tick's burst, so a survival gate prices a missed switch (worst
     * channel only, never expected damage).
     * @param worst the blocked hit's worst case
     */
    public void missedFlickRisk(int worst) {
        if (st.prayerSwitched) {
            st.tickBurstWorst += (short) worst;
        }
    }

    /**
     * The current hit delay of a slot's standard attack: its type's rule at the Chebyshev distance from the player's
     * start of tick tile to the nearest footprint tile.
     * @param slot the attacker's slot
     * @return the delay in ticks
     */
    public int hitDelay(int slot) {
        NpcDef def = frame.def(slot);
        return def.hitDelayTicks(Footprints.toTile(Coords.x(st.npcPos[slot]), Coords.y(st.npcPos[slot]), def.size,
                playerX, playerY));
    }

    /**
     * Heals an NPC up to its max, crediting the weighted heal accumulator.
     * @param slot the healed slot
     * @param amount hitpoints
     */
    public void healNpc(int slot, int amount) {
        int before = st.npcHp[slot];
        st.npcHp[slot] = (short) Math.min(frame.npcMaxHp[slot], before + amount);
        st.accThreatHealed1000 += (st.npcHp[slot] - before) * data.damageWeight1000[frame.npcTypeIdx[slot]];
    }

    /**
     * Applies damage to the player through both channels ({@link Hits#damagePlayer}).
     * @param expected expected damage
     * @param worst worst case damage
     * @param instances damage instances
     */
    public void damagePlayer(int expected, int worst, int instances) {
        Hits.damagePlayer(st, expected, worst, instances);
    }

    /**
     * Reports an attack launch to the listener, if any.
     * @param slot the attacker's slot
     * @param style the style, or -1 when unknown
     */
    public void notifyAttack(int slot, int style) {
        AttackListener l = engine.attackListener;
        if (l != null) {
            l.onNpcAttack(slot, style, tick);
        }
    }

    /**
     * Reports a heal cast to the listener, if any.
     * @param srcSlot the healer's slot
     * @param targetSlot the healed slot
     * @param amount hitpoints
     */
    public void notifyHeal(int srcSlot, int targetSlot, int amount) {
        AttackListener l = engine.attackListener;
        if (l != null) {
            l.onNpcHeal(srcSlot, targetSlot, amount, st.tick);
        }
    }

    /**
     * Reports a tile targeted launch to the listener, if any.
     * @param srcSlot the attacker's slot
     * @param tile the player's packed tile at launch
     * @param landDelayTicks ticks until it lands
     */
    public void notifyTileAttack(int srcSlot, int tile, int landDelayTicks) {
        AttackListener l = engine.attackListener;
        if (l != null) {
            l.onTileAttack(srcSlot, tile, landDelayTicks, tick);
        }
    }
}
