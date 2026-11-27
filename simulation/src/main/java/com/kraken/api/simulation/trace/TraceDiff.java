package com.kraken.api.simulation.trace;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tick aligned comparison of two wave traces (typically a live recording against its engine replay). Channels a
 * replay mirrors from the live trace (the meta {@code forced} list) are skipped; estimate channels (cooldowns, sand,
 * damage magnitudes) report as advisory and never count toward the verdict. Live attack events whose animation is a
 * known defend anim (npcs.json {@code defendAnims}) are dropped before pairing, and two position mismatch classes
 * report as advisory rather than structural: ticks where the live player stands inside the NPC's footprint (the in
 * game escape walk is random), and one tick stale capture holds. The pack adds its own channels and advisory lines
 * through {@link TracePack}, using the public helpers here. The verdict counts structurally diverging channels: zero
 * means the model reproduced the wave.
 */
public final class TraceDiff {
    /** Most items a report line lists before the rest are summarised as a count. */
    public static final int MAX_DETAILS = 10;
    /** Shifted attack pairing window: a launch this close in ticks is a cadence drift, not a missing attack. */
    private static final int ATK_SHIFT_WINDOW = 2;
    private static final int HEAL_MATCH_WINDOW = 3;

    private final WaveTrace a;
    private final WaveTrace b;
    private final MechanicsData data;
    private final TracePack pack;
    private final PrintStream out;
    private final Set<String> forced = new LinkedHashSet<>();
    private final List<Integer> sharedTs = new ArrayList<>();
    private final List<String> divergingChannels = new ArrayList<>();
    private Set<Integer> roster = Collections.emptySet();
    private int firstDivergenceT = Integer.MAX_VALUE;
    private String firstDivergenceWhat;
    private int sumPosCompared;
    private int sumPosStructural;
    private int sumPosEscape;
    private int sumPosStale;
    private int sumAtkExact;
    private int sumAtkShifted;
    private int sumAtkAOnly;
    private int sumAtkBOnly;

    private TraceDiff(WaveTrace a, WaveTrace b, MechanicsData data, TracePack pack, PrintStream out) {
        this.a = a;
        this.b = b;
        this.data = data;
        this.pack = pack;
        this.out = out;
        if (a.meta.forced != null) {
            forced.addAll(a.meta.forced);
        }
        if (b.meta.forced != null) {
            forced.addAll(b.meta.forced);
        }
    }

    /**
     * Prints the report.
     * @param a the first trace, usually live
     * @param b the second trace, usually its replay
     * @param data the tables, or null when types are unresolvable
     * @param pack the pack's trace hooks
     * @param out where the report goes
     * @return the number of structurally diverging channels
     */
    public static int diff(WaveTrace a, WaveTrace b, MechanicsData data, TracePack pack, PrintStream out) {
        return new TraceDiff(a, b, data, pack, out).run();
    }

    private int run() {
        header();
        roster = buildRoster();
        npcPositions(roster);
        lineOfSight(roster);
        attacks(roster);
        pack.diffChannels(this);
        heals(roster);
        presence(roster);
        if (!forced.contains("npc.hp")) {
            npcHp(roster);
        }
        if (!forced.contains("player")) {
            playerPos();
        }
        playerDamage();
        advisory(roster);
        verdict();
        return divergingChannels.size();
    }

    /**
     * The first trace.
     * @return trace A
     */
    public WaveTrace a() {
        return a;
    }

    /**
     * The second trace.
     * @return trace B
     */
    public WaveTrace b() {
        return b;
    }

    /**
     * Where the report goes.
     * @return the stream
     */
    public PrintStream out() {
        return out;
    }

    /**
     * Channels either trace's meta lists as forced.
     * @return the channel names
     */
    public Set<String> forced() {
        return forced;
    }

    /**
     * Trace ids declared in both traces with the same key.
     * @return the ids, in declaration order
     */
    public Set<Integer> roster() {
        return roster;
    }

    /**
     * Ticks both traces recorded with state.
     * @return the ticks, ascending
     */
    public List<Integer> sharedTicks() {
        return sharedTs;
    }

    /**
     * The definition behind a trace id.
     * @param id the trace id
     * @return the type, or null when unresolvable
     */
    public NpcDef defOf(int id) {
        TraceNpc decl = a.npcById.get(id);
        if (decl == null) {
            decl = b.npcById.get(id);
        }
        if (decl == null || data == null) {
            return null;
        }
        int type = Traces.typeIndex(data, decl);
        return type >= 0 ? data.npcs[type] : null;
    }

    private void header() {
        out.println("== Wave trace diff ==");
        out.printf("A: %s wave %d, %d tick records (t %d..%d), end %s%n", a.meta.source,
                a.meta.wave, a.ticks.size(), a.firstT(), a.lastT(), endOf(a));
        out.printf("B: %s wave %d, %d tick records (t %d..%d), end %s%s%n", b.meta.source,
                b.meta.wave, b.ticks.size(), b.firstT(), b.lastT(), endOf(b),
                b.meta.replayOf != null ? " (replay of " + b.meta.replayOf + ")" : "");
        if (!forced.isEmpty()) {
            out.println("Forced channels (mirrored, skipped): " + String.join(", ", forced));
        }
        TreeSet<Integer> ts = new TreeSet<>();
        for (TraceTick t : a.ticks) {
            ts.add(t.t);
        }
        int gaps = 0;
        for (int t : ts) {
            TraceTick ta = a.tickByT.get(t);
            TraceTick tb = b.tickByT.get(t);
            if (tb == null) {
                continue;
            }
            if (WaveTrace.isGap(ta) || WaveTrace.isGap(tb)) {
                gaps++;
            } else {
                sharedTs.add(t);
            }
        }
        out.printf("Compared ticks: %d shared, %d gap/missing skipped%n", sharedTs.size(), gaps);
        if (a.meta.wave != b.meta.wave || !pack.sameContext(a.meta.columns, b.meta.columns)) {
            out.println("!! wave/modifier context differs between the traces");
        }
    }

    private static String endOf(WaveTrace t) {
        return t.end == null ? "(no end line)" : t.end.result;
    }

    private Set<Integer> buildRoster() {
        Set<Integer> shared = new LinkedHashSet<>();
        List<String> aOnly = new ArrayList<>();
        List<String> bOnly = new ArrayList<>();
        for (TraceNpc n : a.npcById.values()) {
            TraceNpc other = b.npcById.get(n.id);
            if (other == null) {
                aOnly.add(label(a, n.id));
            } else if (!String.valueOf(n.key).equals(String.valueOf(other.key))) {
                out.printf("!! id %d is %s in A but %s in B — skipping it%n", n.id, n.key, other.key);
            } else {
                shared.add(n.id);
            }
        }
        for (TraceNpc n : b.npcById.values()) {
            if (!a.npcById.containsKey(n.id)) {
                bOnly.add(label(b, n.id));
            }
        }
        out.printf("NPC roster: %d shared%s%s%n", shared.size(),
                aOnly.isEmpty() ? "" : "; A-only: " + aOnly,
                bOnly.isEmpty() ? "" : "; B-only: " + bOnly);
        if (!aOnly.isEmpty() || !bOnly.isEmpty()) {
            diverged("roster", a.firstT(), "NPC rosters differ");
        }
        return shared;
    }

    private void npcPositions(Set<Integer> ids) {
        out.println("-- npc positions (escape/stale mismatches are advisory) --");
        for (int id : ids) {
            int match = 0;
            int compared = 0;
            int escape = 0;
            int stale = 0;
            List<String> details = new ArrayList<>();
            int firstT = Integer.MIN_VALUE;
            for (int t : sharedTs) {
                TraceTick.Np na = WaveTrace.np(a.tickByT.get(t), id);
                TraceTick.Np nb = WaveTrace.np(b.tickByT.get(t), id);
                if (na == null || nb == null) {
                    continue;
                }
                compared++;
                if (Arrays.equals(na.xy, nb.xy)) {
                    match++;
                } else if (underfoot(t, id) || underfoot(t - 1, id)) {
                    // The in game escape walk while the player is inside the footprint is random: unpredictable
                    // by design, so not a structural miss. One extra tick after the overlap covers the resync step.
                    escape++;
                } else if (staleSingleton(id, t, na)) {
                    stale++;
                } else {
                    if (firstT == Integer.MIN_VALUE) {
                        firstT = t;
                    }
                    if (details.size() < MAX_DETAILS) {
                        details.add(String.format("t=%d A(%d,%d) B(%d,%d)", t,
                                na.xy[0], na.xy[1], nb.xy[0], nb.xy[1]));
                    }
                }
            }
            int structural = compared - match - escape - stale;
            sumPosCompared += compared;
            sumPosStructural += structural;
            sumPosEscape += escape;
            sumPosStale += stale;
            String advisory = (escape > 0 ? " (" + escape + " escape" : "")
                    + (stale > 0 ? (escape > 0 ? ", " : " (") + stale + " stale" : "")
                    + (escape + stale > 0 ? " advisory)" : "");
            if (structural == 0) {
                out.printf("%s: all %d ticks match%s%n", label(a, id), compared, advisory);
            } else {
                out.printf("%s: %d/%d ticks match%s; diverges from t=%d: %s%s%n", label(a, id),
                        match, compared, advisory, firstT, String.join(", ", details),
                        structural > details.size()
                                ? " (+" + (structural - details.size()) + " more)" : "");
                diverged("npc-pos", firstT, label(a, id) + " position");
            }
        }
    }

    /** True when the A side player tile lies inside this NPC's A side footprint at t. */
    private boolean underfoot(int t, int id) {
        TraceTick tick = a.tickByT.get(t);
        if (tick == null || WaveTrace.isGap(tick) || tick.p == null || tick.p.xy == null) {
            return false;
        }
        TraceTick.Np n = WaveTrace.np(tick, id);
        TraceNpc decl = a.npcById.get(id);
        if (n == null || decl == null) {
            return false;
        }
        int size = Math.max(1, decl.size);
        int px = tick.p.xy[0];
        int py = tick.p.xy[1];
        return px >= n.xy[0] && px < n.xy[0] + size && py >= n.xy[1] && py < n.xy[1] + size;
    }

    /**
     * One tick capture stale hold: A repeats its previous tile, then jumps 2 or more tiles (an impossible single
     * step: the recorder read the position one tick late), while both neighbouring ticks match. A genuine one tick
     * model wobble lacks the jump and stays structural.
     */
    private boolean staleSingleton(int id, int t, TraceTick.Np na) {
        TraceTick.Np prevA = WaveTrace.np(a.tickByT.get(t - 1), id);
        TraceTick.Np prevB = WaveTrace.np(b.tickByT.get(t - 1), id);
        TraceTick.Np nextA = WaveTrace.np(a.tickByT.get(t + 1), id);
        TraceTick.Np nextB = WaveTrace.np(b.tickByT.get(t + 1), id);
        if (prevA == null || prevB == null || nextA == null || nextB == null) {
            return false;
        }
        int jump = Math.max(Math.abs(nextA.xy[0] - na.xy[0]), Math.abs(nextA.xy[1] - na.xy[1]));
        return Arrays.equals(na.xy, prevA.xy)
                && Arrays.equals(prevA.xy, prevB.xy)
                && Arrays.equals(nextA.xy, nextB.xy)
                && jump >= 2;
    }

    /** Sight mismatches while positions matched: a pure sight model signal. */
    private void lineOfSight(Set<Integer> ids) {
        out.println("-- line of sight (only ticks where positions match) --");
        boolean any = false;
        for (int id : ids) {
            List<String> details = new ArrayList<>();
            int firstT = Integer.MIN_VALUE;
            for (int t : sharedTs) {
                TraceTick.Np na = WaveTrace.np(a.tickByT.get(t), id);
                TraceTick.Np nb = WaveTrace.np(b.tickByT.get(t), id);
                if (na == null || nb == null || !Arrays.equals(na.xy, nb.xy)) {
                    continue;
                }
                if (!forced.contains("player")
                        && !Arrays.equals(a.tickByT.get(t).p.xy, b.tickByT.get(t).p.xy)) {
                    continue;
                }
                if (na.los != nb.los) {
                    if (firstT == Integer.MIN_VALUE) {
                        firstT = t;
                    }
                    if (details.size() < MAX_DETAILS) {
                        details.add("t=" + t + " A=" + na.los + " B=" + nb.los);
                    }
                }
            }
            if (!details.isEmpty()) {
                any = true;
                out.printf("%s: %s%n", label(a, id), String.join(", ", details));
                diverged("los", firstT, label(a, id) + " line of sight");
            }
        }
        if (!any) {
            out.println("no mismatches");
        }
    }

    private void attacks(Set<Integer> ids) {
        out.println("-- attack launches (defend anims excluded) --");
        for (int id : ids) {
            NpcDef def = defOf(id);
            List<int[]> la = eventTicks(a, id, Traces.EV_ATK, def);
            List<int[]> lb = eventTicks(b, id, Traces.EV_ATK, def);
            if (la.isEmpty() && lb.isEmpty()) {
                continue;
            }
            // A pack may compare grouped launches (volley starts) so the channel measures cadence, not the per
            // launch event count.
            String group = pack.attackGroup(def, a.npcById.get(id));
            if (group != null) {
                la = pack.groupAttacks(def, la);
                lb = pack.groupAttacks(def, lb);
            }
            boolean[] usedB = new boolean[lb.size()];
            int matched = 0;
            List<String> shifted = new ArrayList<>();
            List<String> aOnly = new ArrayList<>();
            int firstT = Integer.MIN_VALUE;
            for (int[] ea : la) {
                int exact = find(lb, usedB, ea, 0);
                if (exact >= 0) {
                    usedB[exact] = true;
                    matched++;
                    continue;
                }
                int near = find(lb, usedB, ea, ATK_SHIFT_WINDOW);
                if (near >= 0) {
                    usedB[near] = true;
                    shifted.add(String.format("A t=%d -> B t=%d (%s)", ea[0], lb.get(near)[0],
                            Traces.styleName(ea[1] >= 0 ? ea[1] : lb.get(near)[1])));
                    firstT = firstT == Integer.MIN_VALUE ? Math.min(ea[0], lb.get(near)[0]) : firstT;
                } else {
                    aOnly.add("t=" + ea[0] + " " + Traces.styleName(ea[1]));
                    firstT = firstT == Integer.MIN_VALUE ? ea[0] : firstT;
                }
            }
            List<String> bOnly = new ArrayList<>();
            for (int i = 0; i < lb.size(); i++) {
                if (!usedB[i]) {
                    bOnly.add("t=" + lb.get(i)[0] + " " + Traces.styleName(lb.get(i)[1]));
                    firstT = firstT == Integer.MIN_VALUE ? lb.get(i)[0]
                            : Math.min(firstT, lb.get(i)[0]);
                }
            }
            sumAtkExact += matched;
            sumAtkShifted += shifted.size();
            sumAtkAOnly += aOnly.size();
            sumAtkBOnly += bOnly.size();
            String unit = group != null ? " " + group : " launches";
            if (shifted.isEmpty() && aOnly.isEmpty() && bOnly.isEmpty()) {
                out.printf("%s: %d%s, all matched%n", label(a, id), matched, unit);
            } else {
                out.printf("%s: A %d / B %d%s, %d exact%s%s%s%n", label(a, id),
                        la.size(), lb.size(), unit, matched,
                        shifted.isEmpty() ? "" : "; shifted: " + trim(shifted),
                        aOnly.isEmpty() ? "" : "; A-only: " + trim(aOnly),
                        bOnly.isEmpty() ? "" : "; B-only: " + trim(bOnly));
                diverged("attacks", firstT, label(a, id) + " attack cadence");
            }
        }
    }

    /** Live heal hitsplats against model heal casts, per beneficiary. */
    private void heals(Set<Integer> ids) {
        List<String> lines = new ArrayList<>();
        for (int id : ids) {
            List<Integer> healedA = healTicks(a, id);
            List<Integer> healedB = healTicks(b, id);
            if (healedA.isEmpty() && healedB.isEmpty()) {
                continue;
            }
            boolean[] used = new boolean[healedB.size()];
            int matched = 0;
            for (int ta : healedA) {
                for (int i = 0; i < healedB.size(); i++) {
                    if (!used[i] && Math.abs(healedB.get(i) - ta) <= HEAL_MATCH_WINDOW) {
                        used[i] = true;
                        matched++;
                        break;
                    }
                }
            }
            String line = String.format("target %s: A heals at %s, B at %s (%d matched +-%d ticks)",
                    label(a, id), healedA, healedB, matched, HEAL_MATCH_WINDOW);
            lines.add(line);
            if (matched < Math.max(healedA.size(), healedB.size())) {
                diverged("heals", healedA.isEmpty() ? healedB.get(0) : healedA.get(0),
                        label(a, id) + " heal timing");
            }
        }
        if (!lines.isEmpty()) {
            out.println("-- heals (A: nsplat heals / nheal; B: model heal casts) --");
            lines.forEach(out::println);
        }
    }

    /** Appearance and disappearance ticks per id (skipped when the replay forces them). */
    private void presence(Set<Integer> ids) {
        if (forced.contains("npc.alive")) {
            return;
        }
        out.println("-- presence (spawn/death ticks) --");
        for (int id : ids) {
            int inA = a.npcById.get(id).firstT;
            int inB = b.npcById.get(id).firstT;
            int outA = lastPresentT(a, id);
            int outB = lastPresentT(b, id);
            if (inA != inB || outA != outB) {
                out.printf("%s: A t %d..%d, B t %d..%d%n", label(a, id), inA, outA, inB, outB);
                diverged("presence", Math.min(inA, inB), label(a, id) + " presence window");
            }
        }
    }

    private void npcHp(Set<Integer> ids) {
        out.println("-- npc hp --");
        for (int id : ids) {
            int firstT = Integer.MIN_VALUE;
            int worst = 0;
            for (int t : sharedTs) {
                TraceTick.Np na = WaveTrace.np(a.tickByT.get(t), id);
                TraceTick.Np nb = WaveTrace.np(b.tickByT.get(t), id);
                if (na == null || nb == null) {
                    continue;
                }
                int d = Math.abs(na.hp - nb.hp);
                if (d > 0 && firstT == Integer.MIN_VALUE) {
                    firstT = t;
                }
                worst = Math.max(worst, d);
            }
            if (firstT != Integer.MIN_VALUE) {
                out.printf("%s: drifts from t=%d, worst |dhp| %d%n", label(a, id), firstT, worst);
                diverged("npc-hp", firstT, label(a, id) + " hp");
            }
        }
    }

    private void playerPos() {
        out.println("-- player position --");
        int firstT = Integer.MIN_VALUE;
        int mismatches = 0;
        for (int t : sharedTs) {
            if (!Arrays.equals(a.tickByT.get(t).p.xy, b.tickByT.get(t).p.xy)) {
                mismatches++;
                if (firstT == Integer.MIN_VALUE) {
                    firstT = t;
                }
            }
        }
        if (mismatches == 0) {
            out.println("all ticks match");
        } else {
            out.printf("%d mismatched ticks from t=%d%n", mismatches, firstT);
            diverged("player-pos", firstT, "player position");
        }
    }

    /**
     * Damage landing timing: live hitsplats (psplat) or model hit bookings (phit), whichever a side carries; counts
     * per tick are the signal, magnitudes advisory.
     */
    private void playerDamage() {
        out.println("-- player damage timing (psplat/phit per tick) --");
        List<String> mismatchTicks = new ArrayList<>();
        int[] count = new int[2];
        int[] total = new int[2];
        int firstT = Integer.MIN_VALUE;
        for (int t : sharedTs) {
            int[] perSide = new int[2];
            WaveTrace[] sides = {a, b};
            for (int s = 0; s < 2; s++) {
                for (TraceTick.Ev ev : WaveTrace.events(sides[s].tickByT.get(t), Traces.EV_PLAYER_SPLAT)) {
                    perSide[s]++;
                    total[s] += ev.d != null ? ev.d : 0;
                }
                for (TraceTick.Ev ev : WaveTrace.events(sides[s].tickByT.get(t), Traces.EV_PLAYER_HIT)) {
                    perSide[s]++;
                    total[s] += ev.exp != null ? ev.exp : 0;
                }
                count[s] += perSide[s];
            }
            if (perSide[0] != perSide[1]) {
                if (firstT == Integer.MIN_VALUE) {
                    firstT = t;
                }
                if (mismatchTicks.size() < MAX_DETAILS) {
                    mismatchTicks.add("t=" + t + " (A " + perSide[0] + ", B " + perSide[1] + ")");
                }
            }
        }
        out.printf("totals: A %d events / %d damage; B %d events / %d damage "
                        + "(A splat damage is rolled, B phit damage is expected-channel)%n",
                count[0], total[0], count[1], total[1]);
        if (mismatchTicks.isEmpty()) {
            out.println("landing ticks: all match");
        } else {
            out.println("count mismatches: " + trim(mismatchTicks));
            diverged("player-hits", firstT, "player damage landing ticks");
        }
    }

    private void advisory(Set<Integer> ids) {
        out.println("-- advisory (estimates; never counted as divergence) --");
        pack.diffAdvisory(this);
        int sandA = countEvents(a, Traces.EV_SAND);
        int sandB = countEvents(b, Traces.EV_SAND);
        if (sandA + sandB > 0) {
            out.printf("sand tiles: A %d, B %d%n", sandA, sandB);
        }
        StringBuilder cd = new StringBuilder();
        for (int id : ids) {
            long sum = 0;
            int n = 0;
            for (int t : sharedTs) {
                TraceTick.Np na = WaveTrace.np(a.tickByT.get(t), id);
                TraceTick.Np nb = WaveTrace.np(b.tickByT.get(t), id);
                if (na != null && nb != null) {
                    sum += Math.abs(na.cd - nb.cd);
                    n++;
                }
            }
            if (n > 0 && sum > 0) {
                if (cd.length() > 0) {
                    cd.append(", ");
                }
                cd.append(String.format("%s %.1f", label(a, id), (double) sum / n));
            }
        }
        if (cd.length() > 0) {
            out.println("mean |dcd| (A cooldowns are tracker estimates): " + cd);
        }
    }

    private void verdict() {
        out.println("== Verdict ==");
        out.printf("summary: pos=%d/%d escape=%d stale=%d atk[exact=%d shifted=%d aOnly=%d bOnly=%d] firstDiv=%s%n",
                sumPosStructural, sumPosCompared, sumPosEscape, sumPosStale,
                sumAtkExact, sumAtkShifted, sumAtkAOnly, sumAtkBOnly,
                divergingChannels.isEmpty() ? "none" : "t=" + firstDivergenceT);
        if (divergingChannels.isEmpty()) {
            out.println("PARITY: no structural divergence on the compared channels");
        } else {
            out.println("diverging channels: " + String.join(", ", new LinkedHashSet<>(divergingChannels)));
            out.printf("first divergence: t=%d (%s)%n", firstDivergenceT, firstDivergenceWhat);
        }
    }

    /**
     * Records a structurally diverging channel.
     * @param channel the channel name
     * @param t the first diverging tick, or Integer.MIN_VALUE when unknown
     * @param what what diverged, for the report
     */
    public void diverged(String channel, int t, String what) {
        divergingChannels.add(channel);
        if (t != Integer.MIN_VALUE && t < firstDivergenceT) {
            firstDivergenceT = t;
            firstDivergenceWhat = what;
        }
    }

    /**
     * A report label for a trace id.
     * @param trace the trace declaring it
     * @param id the trace id
     * @return "id N key"
     */
    public String label(WaveTrace trace, int id) {
        TraceNpc n = trace.npcById.get(id);
        return "id " + id + (n != null ? " " + n.key : "");
    }

    /**
     * A list for the report, cut to {@link #MAX_DETAILS} items.
     * @param items the items
     * @return the printed list
     */
    public static String trim(List<String> items) {
        if (items.size() <= MAX_DETAILS) {
            return items.toString();
        }
        return items.subList(0, MAX_DETAILS) + " (+" + (items.size() - MAX_DETAILS) + " more)";
    }

    /** (tick, styleCode, animId or -1) per event of this id, in tick order; defend animations are dropped. */
    private List<int[]> eventTicks(WaveTrace trace, int id, String kind, NpcDef def) {
        List<int[]> list = new ArrayList<>();
        for (TraceTick tick : trace.ticks) {
            for (TraceTick.Ev ev : WaveTrace.events(tick, kind)) {
                if (ev.id == null || ev.id != id) {
                    continue;
                }
                int anim = ev.anim != null ? ev.anim : -1;
                if (def != null && anim >= 0 && def.isDefendAnim(anim)) {
                    continue;
                }
                list.add(new int[]{tick.t, Traces.styleCode(ev.style), anim});
            }
        }
        return list;
    }

    /**
     * Every event of a kind about one NPC.
     * @param trace the trace
     * @param id the trace id
     * @param kind the event kind
     * @return the events, in tick order
     */
    public List<TraceTick.Ev> idEvents(WaveTrace trace, int id, String kind) {
        List<TraceTick.Ev> list = new ArrayList<>();
        for (TraceTick tick : trace.ticks) {
            for (TraceTick.Ev ev : WaveTrace.events(tick, kind)) {
                if (ev.id != null && ev.id == id) {
                    list.add(ev);
                }
            }
        }
        return list;
    }

    /**
     * The tick of the record holding an event.
     * @param trace the trace
     * @param target the event
     * @return the tick, 0 when not found
     */
    public int firstEventT(WaveTrace trace, TraceTick.Ev target) {
        for (TraceTick tick : trace.ticks) {
            if (tick.ev != null && tick.ev.contains(target)) {
                return tick.t;
            }
        }
        return 0;
    }

    /** Styles equal, or either side unknown ("?"). */
    private static boolean styleCompatible(int sa, int sb) {
        return sa < 0 || sb < 0 || sa == sb;
    }

    private static int find(List<int[]> list, boolean[] used, int[] target, int window) {
        for (int i = 0; i < list.size(); i++) {
            if (!used[i] && Math.abs(list.get(i)[0] - target[0]) <= window
                    && styleCompatible(list.get(i)[1], target[1])) {
                return i;
            }
        }
        return -1;
    }

    /** Heal landings on one NPC: live nheal splats, or model heal casts targeting it. */
    private List<Integer> healTicks(WaveTrace trace, int id) {
        List<Integer> list = new ArrayList<>();
        for (TraceTick tick : trace.ticks) {
            for (TraceTick.Ev ev : WaveTrace.events(tick, Traces.EV_NPC_HEAL_SPLAT)) {
                if (ev.id != null && ev.id == id) {
                    list.add(tick.t);
                }
            }
            for (TraceTick.Ev ev : WaveTrace.events(tick, Traces.EV_HEAL_CAST)) {
                if (ev.tgt != null && ev.tgt == id) {
                    list.add(tick.t);
                }
            }
        }
        return list;
    }

    private int lastPresentT(WaveTrace trace, int id) {
        int last = Integer.MIN_VALUE;
        for (TraceTick tick : trace.ticks) {
            if (WaveTrace.np(tick, id) != null) {
                last = tick.t;
            }
        }
        return last;
    }

    private int countEvents(WaveTrace trace, String kind) {
        int n = 0;
        for (TraceTick tick : trace.ticks) {
            n += WaveTrace.events(tick, kind).size();
        }
        return n;
    }
}
