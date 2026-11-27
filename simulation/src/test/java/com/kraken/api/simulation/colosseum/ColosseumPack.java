package com.kraken.api.simulation.colosseum;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kraken.api.simulation.data.ContentPack;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.npc.PackRules;
import com.kraken.api.simulation.trace.TracePack;
import com.kraken.api.simulation.world.Frame;

/**
 * The Fortis Colosseum content pack, a parity copy of Auto Colosseum V2 3.0.0's Colosseum content carved onto the
 * simulation core (V2 owns the pack; this copy proves the core against V2's recorded waves). Tables load from
 * {@code /colosseum} on the test classpath: the core's npcs.json, player.json and pipeline.json, and the pack's own
 * modifiers.json and waves.json. Lanes and flags are documented in {@link Lanes}.
 */
public final class ColosseumPack implements ContentPack {
    public static final String NAME = "colosseum";
    private static final String ROOT = "/colosseum";
    private static final Gson GSON = new Gson();

    private ColosseumData col;
    private MechanicsData data;
    private ColosseumTrace trace;

    private ColosseumPack() {
    }

    /** Loads the pack's tables from {@code /colosseum} and binds its behaviours. */
    public static ColosseumPack load() {
        return load(ROOT);
    }

    /** Loads the pack's tables from another resource root and binds its behaviours. */
    public static ColosseumPack load(String root) {
        ColosseumPack pack = new ColosseumPack();
        pack.data = MechanicsData.load(ColosseumPack.class, root, (tables, npcFile) -> pack.bind(root, tables, npcFile));
        pack.trace = new ColosseumTrace(pack.col);
        return pack;
    }

    private PackRules bind(String root, MechanicsData tables, JsonObject npcFile) {
        col = new ColosseumData(ColosseumPack.class, root, npcFile);
        for (int t = 0; t < tables.npcs.length; t++) {
            tables.bindBehavior(t, behaviorFor(tables.npcs[t]));
        }
        return new ColosseumRules(col);
    }

    private NpcBehavior behaviorFor(NpcDef def) {
        JsonObject block = def.block(NAME);
        ColosseumNpc cfg = block == null ? new ColosseumNpc() : GSON.fromJson(block, ColosseumNpc.class);
        switch (def.behavior) {
            case "manticore":
                return new ManticoreBehavior(cfg.manticore != null ? cfg.manticore : new ColosseumNpc.Manticore(), col);
            case "javelin":
                return new JavelinBehavior(cfg.javelin != null ? cfg.javelin : new ColosseumNpc.Javelin());
            case "minotaur":
                return new MinotaurBehavior(cfg.minotaur != null ? cfg.minotaur : new ColosseumNpc.Minotaur());
            case "warband":
                return new WarbandBehavior(cfg.warband != null ? cfg.warband : new ColosseumNpc.Warband());
            case "bee":
                return new BeeBehavior();
            case "totem":
                return new TotemBehavior();
            case "none":
                return null;
            default:
                throw new IllegalArgumentException("unknown behavior '" + def.behavior + "' for " + def.key);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public MechanicsData data() {
        return data;
    }

    @Override
    public TracePack trace() {
        return trace;
    }

    /** The pack's own tables. */
    public ColosseumData colosseum() {
        return col;
    }

    /** A frame on this pack's data with the given modifiers applied. */
    public Frame frame(Mods mods) {
        Frame f = new Frame();
        f.data = data;
        mods.apply(f);
        return f;
    }

    /** Resolves a modifier mask and tiers against this pack's modifier table. */
    public Mods mods(int mask, int tiers) {
        return Mods.resolve(col.modifiers, mask, tiers);
    }

    /** The manticore behaviour of a type, or null when it is not a manticore. */
    public static ManticoreBehavior manticore(NpcDef def) {
        return def.behaviorImpl instanceof ManticoreBehavior ? (ManticoreBehavior) def.behaviorImpl : null;
    }

    /** The javelin settings of a type, or null when it is not a javelin colossus. */
    public static ColosseumNpc.Javelin javelin(NpcDef def) {
        return def.behaviorImpl instanceof JavelinBehavior ? ((JavelinBehavior) def.behaviorImpl).cfg : null;
    }

    /** The warband settings of a type, or null when it is not a warband member. */
    public static ColosseumNpc.Warband warband(NpcDef def) {
        return def.behaviorImpl instanceof WarbandBehavior ? ((WarbandBehavior) def.behaviorImpl).cfg : null;
    }

    /** The minotaur settings of a type, or null when it is not a minotaur. */
    public static ColosseumNpc.Minotaur minotaur(NpcDef def) {
        return def.behaviorImpl instanceof MinotaurBehavior ? ((MinotaurBehavior) def.behaviorImpl).cfg : null;
    }
}
