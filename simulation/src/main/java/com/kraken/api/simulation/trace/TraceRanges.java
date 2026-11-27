package com.kraken.api.simulation.trace;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Footprints;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.geom.Los;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Empirical attack range and sight validation from live traces. Per NPC type, over every real attack event (defend
 * anims and the animations the pack skips excluded): the clamped footprint Chebyshev distance at launch, plus two
 * violation classes: "attacked without model sight" (a real launch the sight model would forbid: a ray or range
 * error) and "should have attacked" (stationary, cooldown ready, in modeled range and sight, gates passed, yet no
 * launch: range or sight too permissive). The tool only reports; npcs.json changes stay manual.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceRanges {
    private static final int SAMPLES = 5;

    private static final class Stat {
        NpcDef def;
        int attacks;
        int minDist = Integer.MAX_VALUE;
        int maxDist = -1;
        final List<String> losViolations = new ArrayList<>();
        int losViolationCount;
        final List<String> shouldHave = new ArrayList<>();
        int shouldHaveCount;
    }

    /**
     * Validates ranges and sight over a set of live traces and prints the findings.
     * @param traces the live traces
     * @param data the tables
     * @param pack the pack's trace hooks
     * @param out where the report goes
     */
    public static void run(List<WaveTrace> traces, MechanicsData data, TracePack pack, PrintStream out) {
        Map<String, Stat> stats = new LinkedHashMap<>();
        for (WaveTrace trace : traces) {
            Grid grid = trace.grid();
            Map<Integer, Integer> lastAtk = new LinkedHashMap<>();
            for (TraceTick cur : trace.ticks) {
                TraceTick prev = trace.tickByT.get(cur.t - 1);
                if (WaveTrace.isGap(prev) || WaveTrace.isGap(cur)) {
                    continue;
                }
                int px = prev.p.xy[0];
                int py = prev.p.xy[1];
                for (TraceNpc decl : trace.npcById.values()) {
                    TraceTick.Np np = WaveTrace.np(cur, decl.id);
                    TraceTick.Np before = WaveTrace.np(prev, decl.id);
                    NpcDef def = defOf(data, decl);
                    if (np == null || def == null || def.attackSpeed <= 0) {
                        continue;
                    }
                    Stat stat = stats.computeIfAbsent(def.key, k -> new Stat());
                    stat.def = def;
                    boolean attacked = false;
                    for (TraceTick.Ev ev : WaveTrace.events(cur, Traces.EV_ATK)) {
                        if (ev.id == null || ev.id != decl.id) {
                            continue;
                        }
                        int anim = ev.anim != null ? ev.anim : -1;
                        if (anim >= 0 && (def.isDefendAnim(anim) || pack.skipRangeSample(def, anim))) {
                            continue;
                        }
                        attacked = true;
                        stat.attacks++;
                        int dist = Footprints.toTile(np.xy[0], np.xy[1], def.size, px, py);
                        stat.minDist = Math.min(stat.minDist, dist);
                        stat.maxDist = Math.max(stat.maxDist, dist);
                        if (!Los.npcCanAttack(grid, np.xy[0], np.xy[1], def.size, def.attackRange, px, py)) {
                            stat.losViolationCount++;
                            if (stat.losViolations.size() < SAMPLES) {
                                stat.losViolations.add(String.format("w%d t=%d npc(%d,%d) player(%d,%d) dist=%d",
                                        trace.meta.wave, cur.t, np.xy[0], np.xy[1], px, py, dist));
                            }
                        }
                    }
                    if (attacked) {
                        lastAtk.put(decl.id, cur.t);
                    } else if (before != null
                            && np.xy[0] == before.xy[0] && np.xy[1] == before.xy[1]
                            && cur.t - Math.max(0, decl.firstT) >= data.flags.gateAttackTick
                            && ready(lastAtk.get(decl.id), cur.t, def.attackSpeed)
                            && Los.npcCanAttack(grid, np.xy[0], np.xy[1], def.size, def.attackRange, px, py)) {
                        stat.shouldHaveCount++;
                        if (stat.shouldHave.size() < SAMPLES) {
                            stat.shouldHave.add(String.format("w%d t=%d npc(%d,%d) player(%d,%d) dist=%d",
                                    trace.meta.wave, cur.t, np.xy[0], np.xy[1], px, py,
                                    Footprints.toTile(np.xy[0], np.xy[1], def.size, px, py)));
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, Stat> e : stats.entrySet()) {
            Stat s = e.getValue();
            if (s.attacks == 0 && s.shouldHaveCount == 0) {
                continue;
            }
            out.printf("%s (range %d in npcs.json): %d real attacks, launch dist %s..%s%n",
                    e.getKey(), s.def.attackRange, s.attacks,
                    s.minDist == Integer.MAX_VALUE ? "-" : String.valueOf(s.minDist),
                    s.maxDist < 0 ? "-" : String.valueOf(s.maxDist));
            if (s.losViolationCount > 0) {
                out.printf("  attacked WITHOUT model LOS: %d  %s%n", s.losViolationCount, s.losViolations);
            }
            if (s.shouldHaveCount > 0) {
                out.printf("  ready+sighted but did NOT attack: %d  %s%n", s.shouldHaveCount, s.shouldHave);
            }
        }
    }

    private static boolean ready(Integer last, int t, int attackSpeed) {
        return last == null || t - last >= attackSpeed;
    }

    private static NpcDef defOf(MechanicsData data, TraceNpc decl) {
        int type = Traces.typeIndex(data, decl);
        return type >= 0 ? data.npcs[type] : null;
    }
}
