package com.kraken.api.simulation.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.npc.PackRules;
import com.kraken.api.simulation.tick.TickContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A content pack's mechanics tables, loaded once into primitive friendly structures: npcs.json, player.json and
 * pipeline.json under the pack's resource root, with the pack's behaviours and world rules bound. Gson appears only
 * while loading; the tick and rollout hot paths read arrays.
 */
public final class MechanicsData {
    private static final Gson GSON = new Gson();

    /** The NPC types, indexed by type index. */
    public final NpcDef[] npcs;
    /** Player side constants and the consumable table. */
    public final PlayerData player;
    /** Tick pipeline toggles. */
    public final PipelineFlags flags;
    /**
     * Per type value credited to {@code SimState.accKillValue} on a kill; 0 until a planner fills it. Every frame and
     * engine on these tables reads the same array, so fill it once after loading, before any rollout.
     */
    public final int[] killValue;
    /**
     * Per type weight x1000 of damage dealt to and healed on that type; 1000 until a planner fills it, once after
     * loading like {@link #killValue}.
     */
    public final int[] damageWeight1000;

    private PackRules rules = PackRules.NONE;
    private final Map<String, Integer> typeIndexByKey = new HashMap<>();
    private final Map<Integer, Integer> typeIndexById = new HashMap<>();

    /**
     * Builds tables from definitions already in memory (tests and tools); bind behaviours afterwards.
     * @param npcs the types, in type index order
     * @param player the player constants
     * @param flags the pipeline flags
     */
    public MechanicsData(NpcDef[] npcs, PlayerData player, PipelineFlags flags) {
        this.npcs = npcs;
        this.player = player;
        this.flags = flags;
        killValue = new int[npcs.length];
        damageWeight1000 = new int[npcs.length];
        Arrays.fill(damageWeight1000, 1000);
        for (int t = 0; t < npcs.length; t++) {
            npcs[t].finish();
            typeIndexByKey.put(npcs[t].key, t);
            for (int id : npcs[t].ids) {
                typeIndexById.put(id, t);
            }
        }
    }

    /**
     * Loads a pack's tables from its resources and binds the pack.
     * @param owner a class of the pack, whose class loader holds the resources
     * @param resourceRoot the absolute resource directory, for example "/colosseum"
     * @param binder the pack's binder
     * @return the bound tables
     */
    public static MechanicsData load(Class<?> owner, String resourceRoot, PackBinder binder) {
        JsonObject npcFile = readTree(owner, resourceRoot + "/npcs.json");
        JsonArray entries = npcFile.getAsJsonArray("npcs");
        NpcDef[] npcs = new NpcDef[entries.size()];
        for (int i = 0; i < npcs.length; i++) {
            JsonElement entry = entries.get(i);
            npcs[i] = GSON.fromJson(entry, NpcDef.class);
            npcs[i].source = entry.getAsJsonObject();
        }
        MechanicsData data = new MechanicsData(npcs,
                read(owner, resourceRoot + "/player.json", PlayerData.class),
                read(owner, resourceRoot + "/pipeline.json", PipelineFlags.class));
        PackRules rules = binder == null ? null : binder.bind(data, npcFile);
        data.rules = rules == null ? PackRules.NONE : rules;
        return data;
    }

    /**
     * Reads one JSON resource of a pack into a class.
     * @param owner a class of the pack
     * @param resource the absolute resource path
     * @param type the class to read into
     * @param <T> the class
     * @return the parsed value
     */
    public static <T> T read(Class<?> owner, String resource, Class<T> type) {
        return GSON.fromJson(readTree(owner, resource), type);
    }

    /**
     * Reads one JSON resource of a pack as a tree.
     * @param owner a class of the pack
     * @param resource the absolute resource path
     * @return the parsed object
     */
    public static JsonObject readTree(Class<?> owner, String resource) {
        try (InputStream in = owner.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + resource);
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                // Instance API instead of JsonParser.parseReader: the client bundled Gson predates it.
                return new JsonParser().parse(r).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to load " + resource, e);
        }
    }

    /**
     * The pack's world rules.
     * @return the rules, {@link PackRules#NONE} when the pack has none
     */
    public PackRules rules() {
        return rules;
    }

    /**
     * Replaces the world rules (tests and tools building tables in code).
     * @param rules the rules, or null for none
     */
    public void bindRules(PackRules rules) {
        this.rules = rules == null ? PackRules.NONE : rules;
    }

    /**
     * Binds a behaviour to a type and records which hooks it overrides, so the engine calls only those.
     * @param type the type index
     * @param behavior the behaviour, or null for the standard turn
     */
    public void bindBehavior(int type, NpcBehavior behavior) {
        NpcDef def = npcs[type];
        def.behaviorImpl = behavior;
        def.hookMask = behavior == null ? 0 : hookMask(behavior);
    }

    private static int hookMask(NpcBehavior b) {
        int mask = 0;
        if (overrides(b, "onDeadTurn")) {
            mask |= NpcBehavior.HOOK_DEAD_TURN;
        }
        if (overrides(b, "onTurnStart")) {
            mask |= NpcBehavior.HOOK_TURN_START;
        }
        if (overrides(b, "turn")) {
            mask |= NpcBehavior.HOOK_TURN;
        }
        if (overrides(b, "attack")) {
            mask |= NpcBehavior.HOOK_ATTACK;
        }
        if (overrides(b, "onDeath")) {
            mask |= NpcBehavior.HOOK_DEATH;
        }
        return mask;
    }

    private static boolean overrides(NpcBehavior b, String hook) {
        try {
            return b.getClass().getMethod(hook, TickContext.class, int.class).getDeclaringClass()
                    != NpcBehavior.class;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("NpcBehavior has no hook " + hook, e);
        } catch (SecurityException e) {
            throw new IllegalStateException("cannot read the hooks of " + b.getClass().getName(), e);
        }
    }

    /**
     * The type index for a key.
     * @param key the npcs.json key
     * @return the type index
     * @throws IllegalArgumentException for an unknown key
     */
    public int typeIndex(String key) {
        Integer i = typeIndexByKey.get(key);
        if (i == null) {
            throw new IllegalArgumentException("unknown npc key " + key);
        }
        return i;
    }

    /**
     * The type for a key.
     * @param key the npcs.json key
     * @return the definition
     * @throws IllegalArgumentException for an unknown key
     */
    public NpcDef def(String key) {
        return npcs[typeIndex(key)];
    }

    /**
     * Type index for a live NPC id.
     * @param npcId the live id
     * @return the type index, or -1 when the id is unknown
     */
    public int typeIndexForNpcId(int npcId) {
        Integer i = typeIndexById.get(npcId);
        return i == null ? -1 : i;
    }
}
