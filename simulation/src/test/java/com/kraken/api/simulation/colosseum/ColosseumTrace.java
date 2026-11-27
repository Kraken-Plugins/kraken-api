package com.kraken.api.simulation.colosseum;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.trace.TraceDiff;
import com.kraken.api.simulation.trace.TraceNpc;
import com.kraken.api.simulation.trace.TracePack;
import com.kraken.api.simulation.trace.TraceTick;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTrace;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * The Colosseum's trace columns, written exactly as V2's schema 1 traces carry them so its recordings read unchanged:
 * meta {@code modifierMask}, {@code modifierTiers}, {@code modifiers}, {@code reinforcementTick}; tick {@code solar};
 * player {@code doom}; NPC {@code chg}, {@code pat}, {@code orbs} (manticore), {@code spc} (javelin) and {@code ben}
 * (totem). Its diff channels are the manticore charge and pattern and the javelin toss schedule, and its advisory
 * lines the Solarflare clock.
 */
public final class ColosseumTrace implements TracePack {
    public static final String EV_SOLAR_OBS = "solarobs";

    /** Names by modifier id (the script 4931 bitmask order {@link Mods} uses). */
    private static final String[] MODIFIER_NAMES = {
            "mantimayhem", "reentry", "bees", "volatility", "blasphemy", "relentless",
            "quartet", "totemic", "doom", "dynamic-duo", "solarflare", "myopia",
            "frailty", "red-flag",
    };
    private static final String[] ROMAN = {"", "I", "II", "III"};

    private final ColosseumData col;

    ColosseumTrace(ColosseumData col) {
        this.col = col;
    }

    public static List<String> modifierNames(int mask, int tiers) {
        List<String> out = new ArrayList<>();
        for (int id = 0; id < MODIFIER_NAMES.length; id++) {
            if ((mask >>> id & 1) == 0) {
                continue;
            }
            int tier = Math.max(1, tiers >>> (id * 2) & 3);
            out.add(MODIFIER_NAMES[id] + " " + ROMAN[tier]);
        }
        return out;
    }

    @Override
    public String packName() {
        return ColosseumPack.NAME;
    }

    @Override
    public void writeMeta(Frame frame, JsonObject meta) {
        ColosseumFrame cf = ColosseumFrame.of(frame);
        meta.addProperty("modifierMask", cf.modifierMask);
        meta.addProperty("modifierTiers", cf.modifierTiers);
        JsonArray names = new JsonArray();
        for (String name : modifierNames(cf.modifierMask, cf.modifierTiers)) {
            names.add(name);
        }
        meta.add("modifiers", names);
        meta.addProperty("reinforcementTick", col.waves.reinforcementTick);
    }

    @Override
    public void writeTick(Frame frame, SimState st, JsonObject tick) {
        int solar = Lanes.solar(st);
        if (solar >= 0) {
            tick.add("solar", xy(Solar.tile(col, solar, 0)));
        }
    }

    @Override
    public void writePlayer(Frame frame, SimState st, JsonObject player) {
        if (ColosseumFrame.mods(frame).doomCap > 0) {
            player.addProperty("doom", (int) st.hitStacksWorst);
        }
    }

    @Override
    public void writeNpc(Frame frame, SimState st, int slot, IntUnaryOperator traceIdOfSlot, JsonObject npc) {
        NpcDef def = frame.def(slot);
        if (def.behaviorImpl instanceof ManticoreBehavior) {
            npc.addProperty("chg", (st.npcFlags[slot] & Lanes.FLAG_CHARGED) != 0);
            int pattern = st.npcLane[Lanes.PATTERN][slot];
            if (pattern >= 0) {
                npc.addProperty("pat", col.patternNames[pattern]);
            }
            if (st.npcLane[Lanes.ORBS][slot] > 0) {
                npc.addProperty("orbs", (int) st.npcLane[Lanes.ORBS][slot]);
            }
        } else if (def.behaviorImpl instanceof JavelinBehavior) {
            npc.addProperty("spc", (int) st.npcLane[Lanes.JAVELIN_COUNT][slot]);
        } else if (def.behaviorImpl instanceof TotemBehavior) {
            int ben = st.npcLane[Lanes.TOTEM_BENEFICIARY][slot];
            if (ben >= 0 && traceIdOfSlot.applyAsInt(ben) >= 0) {
                npc.addProperty("ben", traceIdOfSlot.applyAsInt(ben));
            }
        }
    }

    @Override
    public void readMeta(JsonObject meta, Frame frame) {
        Mods.resolve(col.modifiers, intColumn(meta, "modifierMask"), intColumn(meta, "modifierTiers")).apply(frame);
    }

    @Override
    public boolean sameContext(JsonObject a, JsonObject b) {
        return intColumn(a, "modifierMask") == intColumn(b, "modifierMask");
    }

    @Override
    public void startReplay(WaveTrace live, SimState st) {
        Mods mods = ColosseumFrame.mods(st.frame);
        if (mods.solarTier > 0) {
            // The live adapter advances its solar clock once per capture, including the trace's first record; mirror
            // that so the orbits stay in lockstep.
            Lanes.setSolar(st, Solar.step(Solar.start(mods), mods));
        } else {
            st.packLane[Lanes.SOLAR] = 0;
        }
    }

    @Override
    public List<String> forcedChannels() {
        return Collections.singletonList("manticore.pattern");
    }

    @Override
    public void forcePlayer(SimState st, TraceTick.Pl p) {
        JsonElement doom = p.columns == null ? null : p.columns.get("doom");
        if (doom != null) {
            st.hitStacksWorst = (byte) Math.min(127, doom.getAsInt());
        }
    }

    @Override
    public void forceObserved(SimState st, int slot, TraceTick.Np np, Map<Integer, Integer> slotById) {
        NpcDef def = st.frame.def(slot);
        if (def.behaviorImpl instanceof ManticoreBehavior) {
            String pat = stringColumn(np, "pat");
            st.npcLane[Lanes.PATTERN][slot] = (byte) (pat != null ? col.patternId(pat) : -1);
        } else if (def.behaviorImpl instanceof TotemBehavior) {
            JsonElement ben = np.column("ben");
            Integer benSlot = ben != null ? slotById.get(ben.getAsInt()) : null;
            st.npcLane[Lanes.TOTEM_BENEFICIARY][slot] = (byte) (benSlot != null ? benSlot : -1);
        }
    }

    @Override
    public void forceSpawn(SimState st, int slot, TraceTick.Np np) {
        JsonElement chg = np.column("chg");
        if (chg != null && chg.getAsBoolean()) {
            st.npcFlags[slot] |= Lanes.FLAG_CHARGED;
        }
        NpcDef def = st.frame.def(slot);
        if (def.behaviorImpl instanceof ManticoreBehavior) {
            String pat = stringColumn(np, "pat");
            st.npcLane[Lanes.PATTERN][slot] = (byte) (pat != null ? col.patternId(pat) : -1);
            JsonElement orbs = np.column("orbs");
            if (orbs != null) {
                st.npcLane[Lanes.ORBS][slot] = (byte) orbs.getAsInt();
            }
        } else if (def.behaviorImpl instanceof JavelinBehavior) {
            JsonElement spc = np.column("spc");
            st.npcLane[Lanes.JAVELIN_COUNT][slot] = (byte) (spc != null ? spc.getAsInt() : -1);
        } else if (def.behaviorImpl instanceof TotemBehavior) {
            st.npcLane[Lanes.TOTEM_BENEFICIARY][slot] = -1;
            st.npcLane[Lanes.TOTEM_HEAL][slot] = (byte) Math.max(1, ColosseumFrame.mods(st.frame).totemHealEvery);
            st.packLane[Lanes.ALIVE_TOTEMS]++;
        }
    }

    @Override
    public String attackGroup(NpcDef def, TraceNpc decl) {
        boolean volley = def != null ? def.behaviorImpl instanceof ManticoreBehavior
                : decl != null && decl.key != null && decl.key.contains("manticore");
        return volley ? "volley starts" : null;
    }

    /**
     * Collapses a manticore's atk events into volley start ticks (style wildcarded: pattern parity is its own
     * channel). Live volleys anchor on the throw anim, played on the first orb's tick; a charge anim is not a volley
     * (a manticore that loses sight after it holds the volley until it sees the player again). Un animated events
     * (the sim's per orb emissions) collapse by consecutive tick runs. Anchors within 2 ticks are one volley (real
     * volleys sit a full attackSpeed apart).
     */
    @Override
    public List<int[]> groupAttacks(NpcDef def, List<int[]> atks) {
        ManticoreBehavior m = def != null ? ColosseumPack.manticore(def) : null;
        List<Integer> anchors = new ArrayList<>();
        int prevRawT = Integer.MIN_VALUE;
        for (int[] ev : atks) {
            int anim = ev.length > 2 ? ev[2] : -1;
            if (m != null && anim >= 0 && m.isVolleyTelegraph(anim)) {
                continue;
            } else if (m != null && anim >= 0 && m.isVolleyThrow(anim)) {
                anchors.add(ev[0]);
            } else {
                if (ev[0] != prevRawT && ev[0] != prevRawT + 1) {
                    anchors.add(ev[0]);
                }
                prevRawT = ev[0];
            }
        }
        anchors.sort(null);
        List<int[]> out = new ArrayList<>();
        int last = Integer.MIN_VALUE / 2;
        for (int t : anchors) {
            if (t - last > 2) {
                out.add(new int[]{t, -1, -1});
            }
            last = t;
        }
        return out;
    }

    @Override
    public void diffChannels(TraceDiff d) {
        manticores(d);
        javelins(d);
    }

    private void manticores(TraceDiff d) {
        WaveTrace a = d.a();
        WaveTrace b = d.b();
        boolean printed = false;
        for (int id : d.roster()) {
            List<String> parts = new ArrayList<>();
            int chgA = firstChargedT(a, id);
            int chgB = firstChargedT(b, id);
            String patA = lastPattern(a, id);
            String patB = lastPattern(b, id);
            if (chgA == Integer.MIN_VALUE && chgB == Integer.MIN_VALUE) {
                continue;
            }
            if (!printed) {
                d.out().println("-- manticore charge/pattern --");
                printed = true;
            }
            parts.add("charge locked A t=" + tOrNever(chgA) + " B t=" + tOrNever(chgB));
            if (chgA != chgB) {
                d.diverged("manticore-charge", Math.min(chgA, chgB), d.label(a, id) + " charge timing");
            }
            if (!d.forced().contains("manticore.pattern")) {
                parts.add("pattern A=" + patA + " B=" + patB);
                if (!String.valueOf(patA).equals(String.valueOf(patB))) {
                    d.diverged("manticore-pattern", Math.max(a.firstT(), 0), d.label(a, id) + " pattern");
                }
            }
            d.out().printf("%s: %s%n", d.label(a, id), String.join("; ", parts));
        }
    }

    private void javelins(TraceDiff d) {
        WaveTrace a = d.a();
        WaveTrace b = d.b();
        boolean printed = false;
        for (int id : d.roster()) {
            List<TraceTick.Ev> ja = d.idEvents(a, id, Traces.EV_TILE_ATTACK);
            List<TraceTick.Ev> jb = d.idEvents(b, id, Traces.EV_TILE_ATTACK);
            if (ja.isEmpty() && jb.isEmpty()) {
                continue;
            }
            if (!printed) {
                d.out().println("-- javelin sky specials --");
                printed = true;
            }
            String sa = specials(d, a, ja);
            String sb = specials(d, b, jb);
            d.out().printf("%s: A %s | B %s%n", d.label(a, id), sa, sb);
            if (!sa.equals(sb)) {
                int firstT = !ja.isEmpty() ? d.firstEventT(a, ja.get(0))
                        : !jb.isEmpty() ? d.firstEventT(b, jb.get(0)) : 0;
                d.diverged("javelin-special", firstT, d.label(a, id) + " special schedule");
            }
        }
    }

    private static String specials(TraceDiff d, WaveTrace trace, List<TraceTick.Ev> evs) {
        if (evs.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        for (TraceTick.Ev ev : evs) {
            parts.add(String.format("t=%d xy(%d,%d) land %s", d.firstEventT(trace, ev),
                    ev.xy[0], ev.xy[1], ev.land));
        }
        return parts.toString();
    }

    @Override
    public void diffAdvisory(TraceDiff d) {
        WaveTrace a = d.a();
        WaveTrace b = d.b();
        int solarCompared = 0;
        int solarMatch = 0;
        for (int t : d.sharedTicks()) {
            int[] sa = solarColumn(a.tickByT.get(t));
            int[] sb = solarColumn(b.tickByT.get(t));
            if (sa != null || sb != null) {
                solarCompared++;
                if (sa != null && sb != null && Arrays.equals(sa, sb)) {
                    solarMatch++;
                }
            }
        }
        if (solarCompared > 0) {
            d.out().printf("solarflare clock tiles: %d/%d match%n", solarMatch, solarCompared);
        }
        int orbCompared = 0;
        int orbMatch = 0;
        for (int t : d.sharedTicks()) {
            List<TraceTick.Ev> obs = WaveTrace.events(a.tickByT.get(t), EV_SOLAR_OBS);
            if (obs.isEmpty()) {
                continue;
            }
            int[] sb = solarColumn(b.tickByT.get(t));
            orbCompared++;
            if (sb != null && Arrays.equals(obs.get(0).xy, sb)) {
                orbMatch++;
            }
        }
        if (orbCompared > 0) {
            d.out().printf("observed orb (A solarobs) vs B model tile: %d/%d match%n", orbMatch, orbCompared);
        }
    }

    @Override
    public boolean skipRangeSample(NpcDef def, int anim) {
        ManticoreBehavior m = ColosseumPack.manticore(def);
        return m != null && m.isVolleyThrow(anim);
    }

    private static int firstChargedT(WaveTrace trace, int id) {
        for (TraceTick tick : trace.ticks) {
            TraceTick.Np n = WaveTrace.np(tick, id);
            JsonElement chg = n == null ? null : n.column("chg");
            if (chg != null && chg.getAsBoolean()) {
                return tick.t;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static String lastPattern(WaveTrace trace, int id) {
        String pat = null;
        for (TraceTick tick : trace.ticks) {
            TraceTick.Np n = WaveTrace.np(tick, id);
            String p = n == null ? null : stringColumn(n, "pat");
            if (p != null) {
                pat = p;
            }
        }
        return pat;
    }

    private static String tOrNever(int t) {
        return t == Integer.MIN_VALUE ? "never" : String.valueOf(t);
    }

    private static int[] solarColumn(TraceTick tick) {
        JsonElement e = tick == null || tick.columns == null ? null : tick.columns.get("solar");
        if (e == null || !e.isJsonArray()) {
            return null;
        }
        JsonArray arr = e.getAsJsonArray();
        return new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()};
    }

    private static String stringColumn(TraceTick.Np np, String name) {
        JsonElement e = np.column(name);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static int intColumn(JsonObject obj, String name) {
        JsonElement e = obj == null ? null : obj.get(name);
        return e == null || e.isJsonNull() ? 0 : e.getAsInt();
    }

    private static JsonArray xy(int pos) {
        int[] xy = Traces.xy(pos);
        JsonArray arr = new JsonArray();
        arr.add(xy[0]);
        arr.add(xy[1]);
        return arr;
    }
}
