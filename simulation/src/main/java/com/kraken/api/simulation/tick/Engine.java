package com.kraken.api.simulation.tick;

import com.kraken.api.simulation.data.EquipSet;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.data.PipelineFlags;
import com.kraken.api.simulation.data.PlayerData;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.move.MoveNaive;
import com.kraken.api.simulation.move.MoveSmart;
import com.kraken.api.simulation.npc.NpcBehavior;
import com.kraken.api.simulation.npc.PackRules;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.player.Consumables;
import com.kraken.api.simulation.player.PlayerTurn;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.Scratch;
import com.kraken.api.simulation.world.SimState;

/**
 * The forward model. {@link #advanceInPlace} simulates one server tick in the OSRS processing order (client input,
 * then NPCs, then the player, https://oldschool.runescape.wiki/w/Game_tick) as one documented sequence; ordering
 * assumptions live here and nowhere else. Deterministic: identical (frame, state, command) gives an identical
 * result, and the steady state tick allocates nothing. Per type mechanics come from the pack's
 * {@link NpcBehavior}s and world scope ones from its {@link PackRules}, each called at a fixed point below.
 *
 * <pre>
 * P0  tick setup: per tick channels reset, in flight hits age one tick, tile targeted hits due to pick their tile
 *     take the player's start of tick tile, then {@code rules.onTickStart} (moving hazards).
 * P1  client input: the command dispatched last real tick applies now: overhead prayer, gear set, consumables
 *     (food, then potion, then karambwan; heals apply now, which is what makes tick eating work), then movement
 *     click XOR attack click (a new click replaces the current interaction), then the spec arm.
 * P2  NPC turns. The player's in flight hits land on NPCs ({@code rules.onNpcDamaged} for survivors); an NPC
 *     killed now never gets its turn and its body blocks until deleted, which happens on its own turn (so in index
 *     order) the type's corpse ticks later ({@code rules.onBodyDeleted}). Then each NPC in NPC index order: a dead
 *     one counts its corpse down ({@code onDeadTurn}); a living one runs {@code onTurnStart}, ticks its cooldown
 *     down, reads its spawn gates (first step, sight hold, first attack), then either takes its own turn
 *     ({@code turn}) or moves with the core (an escape step while the player stands under it, else hold when in
 *     reach and sight, else naive or smart steps up to its move speed) and attacks ({@code attack}, by default the
 *     standard cadence attack). Prayer is read at launch; fully blocked launches never enter the ring. Then
 *     {@code rules.afterNpcTurns} when a hook set the tick's scratch.
 * P3  player turn: hits landing now apply (after P1 heals; tile targeted hits check their tile, so moving off
 *     dodges, after {@code rules.onTileHitLanded}), movement (1 walking or 2 running, along the clicked path or
 *     toward an attack target out of reach), ground hazard contact for the ending tile (molten sand, then
 *     {@code rules.contact}), then the player attack when engaged, in range with sight, and off cooldown.
 * P4  housekeeping: timers, prayer drain (the activation tick itself does not drain), spec regen, venom, the core's
 *     accumulators, the observer, the worst case HP floor, the wave clear check, then {@code rules.endOfTick} (the
 *     pack's accumulators and, while the wave is open, its scheduled spawns).
 * </pre>
 */
public final class Engine {
    private final Scratch scratch = new Scratch();
    private final TickContext ctx = new TickContext(this);
    /** Receives launches, heals and hits; null in production. */
    public AttackListener attackListener;
    /** A planner's per tick measures; null by default. */
    public TickObserver tickObserver;

    /**
     * The engine's working memory.
     * @return the scratch
     */
    public Scratch scratch() {
        return scratch;
    }

    /** Starts a search pass: resets the path cache, route cache and approach fields. */
    public void beginPass() {
        scratch.beginPass();
    }

    /**
     * Advances the state one tick in place.
     * @param st the state
     * @param command this tick's player command ({@link Command})
     */
    public void advanceInPlace(SimState st, long command) {
        Frame f = st.frame;
        MechanicsData data = f.data;
        PipelineFlags flags = data.flags;
        PackRules rules = data.rules();
        TickContext c = ctx;
        int t = st.tick;
        c.st = st;
        c.frame = f;
        c.data = data;
        c.tick = t;
        c.tickScratch = 0;
        c.canMove = false;
        c.canGainLos = false;
        c.canAttack = false;
        c.los = false;

        st.tickBurstWorst = 0;
        st.tickExpectedDamage = 0;
        st.tickStyleMask = 0;
        st.tickLosExposure = 0;
        st.prayerJustActivated = false;
        st.prayerSwitched = false;
        st.tickRangedWorst = 0;
        st.tickMagicWorst = 0;
        st.tickMeleeWorst = 0;
        Hits.ageAll(st);
        Hits.pickTiles(st);
        int packHooks = f.packHooks;
        if ((packHooks & PackRules.HOOK_TICK_START) != 0) {
            rules.onTickStart(c);
        }

        applyCommand(st, command);

        int deaths = landOnNpcs(st, c, rules);
        if (deaths != 0) {
            processDeaths(st, c, rules, deaths, -1);
        }

        // NPCs act before the player, so they see the player's start of tick position.
        int px = Coords.x(st.playerPos);
        int py = Coords.y(st.playerPos);
        c.playerX = px;
        c.playerY = py;
        for (int oi = 0; oi < f.npcOrderCount; oi++) {
            int i = f.npcOrder[oi];
            NpcDef def = data.npcs[f.npcTypeIdx[i]];
            int hooks = def.hookMask;
            if (!st.alive(i)) {
                if (st.npcCorpse[i] > 0 && --st.npcCorpse[i] == 0) {
                    int blast = deleteBody(st, c, rules, i, def);
                    if (blast != 0) {
                        processDeaths(st, c, rules, blast, oi);
                    }
                }
                if ((hooks & NpcBehavior.HOOK_DEAD_TURN) != 0) {
                    def.behaviorImpl.onDeadTurn(c, i);
                }
                continue;
            }
            if ((hooks & NpcBehavior.HOOK_TURN_START) != 0 && def.behaviorImpl.onTurnStart(c, i)) {
                continue;
            }
            st.npcFlags[i] &= ~SimState.FLAG_MOVED;
            // Cooldowns tick down every tick, attacking or not; clamped at -1 because only the sign matters and
            // long sight blocked stretches must not wrap the byte.
            if (st.npcCooldown[i] > -1) {
                st.npcCooldown[i]--;
            }
            // Spawn relative action gates; slots with no known anchor carry SPAWN_OPEN and pass every gate.
            int sinceSpawn = t - st.npcSpawnTick[i];
            boolean canMove = sinceSpawn >= def.moveGate(flags);
            boolean canGainLos = sinceSpawn >= flags.gateLosTick;
            boolean canAttack = sinceSpawn >= flags.gateAttackTick;
            c.canMove = canMove;
            c.canGainLos = canGainLos;
            c.canAttack = canAttack;
            c.los = false;
            if ((hooks & NpcBehavior.HOOK_TURN) != 0 && def.behaviorImpl.turn(c, i)) {
                continue;
            }
            {
                // The core's movement and sight. While the player stands inside the footprint there is no sight and
                // no chase step: the live game walks the NPC one cardinal step per tick until clear (random; a
                // deterministic hash stands in, flag underfootEscape). Otherwise, with its first step gate open and
                // not already held by reach and sight, the NPC steps naive or smart up to its move speed, stopping as
                // soon as it gains reach and sight. Kept inline: a call here costs measurable rollout throughput.
                int size = def.size;
                int pos = st.npcPos[i];
                boolean los = Los.npcCanAttack(f.grid, Coords.x(pos), Coords.y(pos), size, def.attackRange, px, py);
                boolean underfoot = px >= Coords.x(pos) && px <= Coords.x(pos) + size - 1
                        && py >= Coords.y(pos) && py <= Coords.y(pos) + size - 1;
                if (underfoot && flags.underfootEscape) {
                    if (canMove) {
                        escapeStep(st, i, size, t);
                    }
                } else if (canMove && !(canGainLos && los)) {
                    for (int sub = 0; sub < def.moveSpeed; sub++) {
                        st.clearFootprint(i, size);
                        if (f.smartPath(i)) {
                            MoveSmart.step(st, scratch, i, size, px, py);
                        } else {
                            MoveNaive.step(st, i, size, px, py);
                        }
                        st.setFootprint(i, size);
                        if (st.npcPos[i] == pos) {
                            break;
                        }
                        st.npcFlags[i] |= SimState.FLAG_MOVED;
                        pos = st.npcPos[i];
                        los = Los.npcCanAttack(f.grid, Coords.x(pos), Coords.y(pos), size, def.attackRange, px, py);
                        if (canGainLos && los) {
                            break;
                        }
                    }
                }
                // Range 1 NPCs are excluded: melee adjacency ignores pillars, so they can never be sight blocked;
                // the exposure count prices only blockable ranged and magic attackers. Counted on sight, not attack
                // readiness, so spawn gate ticks add the same baseline to every candidate plan.
                if (los && def.attackRange > 1 && def.waveClearing) {
                    st.tickLosExposure++;
                }
                c.los = los;
            }
            if ((hooks & NpcBehavior.HOOK_ATTACK) != 0) {
                def.behaviorImpl.attack(c, i);
            } else if (canAttack && c.los && st.npcCooldown[i] <= 0) {
                // The standard attack's own guard, checked here so the call happens only on launch ticks.
                c.standardAttack(i);
            }
        }
        if (c.tickScratch != 0) {
            rules.afterNpcTurns(c);
        }

        landOnPlayer(st, c, rules);
        if (!flags.hazardsAfterMove) {
            contact(st, c, rules);
        }
        PlayerTurn.run(st, scratch);
        if (flags.hazardsAfterMove) {
            contact(st, c, rules);
        }
        PlayerTurn.attack(st);

        housekeeping(st, f, data, c, rules, t);

        st.tick = t + 1;
    }

    /** Lands due player sourced hits (and heals) on NPCs; returns the death mask. */
    private int landOnNpcs(SimState st, TickContext c, PackRules rules) {
        int deaths = 0;
        int w = 0;
        MechanicsData data = st.frame.data;
        for (int i = 0; i < st.pendingCount; i++) {
            long h = st.pendingHits[i];
            if (!Hits.isVsNpc(h) || Hits.delayOf(h) > 0) {
                st.pendingHits[w++] = h;
                continue;
            }
            int slot = Hits.targetOf(h) & 0xf;
            if (!st.alive(slot)) {
                continue;
            }
            if (Hits.isHeal(h)) {
                int src = Hits.srcOf(h);
                if (src < SimState.MAX_NPCS && st.alive(src)) {
                    c.healNpc(slot, Hits.expectedOf(h));
                }
                continue;
            }
            int dmg = Math.min(Hits.expectedOf(h) * Hits.rollsOf(h), (int) st.npcHp[slot]);
            st.npcHp[slot] -= (short) dmg;
            st.accDamageDealt += dmg;
            st.accThreatDamage1000 += dmg * data.damageWeight1000[st.frame.npcTypeIdx[slot]];
            if (st.npcHp[slot] <= 0) {
                deaths |= 1 << slot;
            } else if ((st.frame.packHooks & PackRules.HOOK_NPC_DAMAGED) != 0) {
                rules.onNpcDamaged(c, slot, dmg);
            }
        }
        st.pendingCount = (byte) w;
        return deaths;
    }

    /**
     * Lands due hits on the player: tile check for tile targeted hits, prayer check for check at land hits, then
     * both damage channels and venom.
     */
    private void landOnPlayer(SimState st, TickContext c, PackRules rules) {
        int w = 0;
        for (int i = 0; i < st.pendingCount; i++) {
            long h = st.pendingHits[i];
            if (Hits.isVsNpc(h) || Hits.delayOf(h) > 0) {
                st.pendingHits[w++] = h;
                continue;
            }
            if (Hits.isTileTargeted(h)) {
                rules.onTileHitLanded(c, Hits.targetOf(h));
                if ((st.playerPos & 0xfff) != Hits.targetOf(h)) {
                    continue;
                }
            }
            int style = Hits.styleOf(h);
            if (Hits.isCheckAtLand(h)) {
                st.tickStyleMask |= (byte) (1 << style);
                if (st.overhead == Style.overheadFor(style)) {
                    continue;
                }
            }
            int rolls = Hits.rollsOf(h);
            Hits.damagePlayer(st, Hits.expectedOf(h) * rolls, Hits.worstOf(h) * rolls, rolls);
            if (attackListener != null) {
                attackListener.onPlayerHit(Hits.srcOf(h), style,
                        Hits.expectedOf(h) * rolls, Hits.worstOf(h) * rolls, st.tick);
            }
            if (Hits.isApplyVenom(h) && (st.venomState & 1) == 0) {
                PlayerData pd = st.frame.data.player;
                st.venomState = 1 | pd.venomStartDamage << 1 | pd.venomIntervalTicks << 7;
            }
        }
        st.pendingCount = (byte) w;
    }

    private void contact(SimState st, TickContext c, PackRules rules) {
        if (Grid.get(st.moltenSand, Coords.x(st.playerPos), Coords.y(st.playerPos))) {
            int sand = st.frame.sandDamage;
            Hits.damagePlayer(st, sand, sand, 1);
        }
        if ((st.frame.packHooks & PackRules.HOOK_CONTACT) != 0) {
            rules.contact(c);
        }
    }

    /**
     * Kills every slot in the mask, chaining deaths a body deletion causes. {@code turn} is the position in the
     * turn order being processed (-1 before the NPC loop): a body's deletion countdown ticks on its own turn, so a
     * slot whose turn already passed this tick starts one lower.
     */
    private void processDeaths(SimState st, TickContext c, PackRules rules, int mask, int turn) {
        while (mask != 0) {
            int slot = Integer.numberOfTrailingZeros(mask);
            mask &= mask - 1;
            if (st.alive(slot)) {
                mask |= killNpc(st, c, rules, slot, turnPassed(st.frame, slot, turn));
            }
        }
    }

    private static boolean turnPassed(Frame f, int slot, int turn) {
        for (int oi = 0; oi <= turn; oi++) {
            if (f.npcOrder[oi] == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Marks a death and credits a kill of an NPC that holds the wave open. The body stays and blocks until it is
     * deleted the type's corpse ticks later (counted down on its turn, from this tick's); a body less or instantly
     * removed NPC is deleted now. Returns the deaths a deletion now caused.
     */
    private int killNpc(SimState st, TickContext c, PackRules rules, int slot, boolean turnPassed) {
        Frame f = st.frame;
        NpcDef def = f.def(slot);
        st.npcFlags[slot] &= ~SimState.FLAG_ALIVE;
        st.npcHp[slot] = 0;
        if (st.targetSlot == slot) {
            st.targetSlot = -1;
        }
        if (def.waveClearing) {
            st.accKills++;
            st.accKillValue += f.data.killValue[f.npcTypeIdx[slot]];
        }
        if ((def.hookMask & NpcBehavior.HOOK_DEATH) != 0) {
            def.behaviorImpl.onDeath(c, slot);
        }
        if (!def.noBody && def.corpseTicks > 0) {
            st.npcCorpse[slot] = (byte) (turnPassed ? def.corpseTicks : def.corpseTicks + 1);
            return 0;
        }
        return deleteBody(st, c, rules, slot, def);
    }

    private int deleteBody(SimState st, TickContext c, PackRules rules, int slot, NpcDef def) {
        if (!def.noBody) {
            st.clearFootprint(slot, def.size);
        }
        return rules.onBodyDeleted(c, slot);
    }

    private static final int[] ESCAPE_DX = {-1, 1, 0, 0};
    private static final int[] ESCAPE_DY = {0, 0, -1, 1};

    /**
     * Escape walk while the player stands inside the footprint: one cardinal step per tick, standing still when the
     * picked direction is blocked (both observed live; the real pick is random, so trace replays mirror live
     * positions through these episodes and only the forward model uses this hash). Pure function of (tick, slot):
     * identical (state, plan) still advances identically.
     */
    private void escapeStep(SimState st, int i, int size, int t) {
        int mix = t * 0x9E3779B9 ^ (i + 1) * 0x85EBCA6B;
        mix ^= mix >>> 16;
        mix *= 0x45D9F3B;
        mix ^= mix >>> 16;
        int d = mix & 3;
        int x = Coords.x(st.npcPos[i]);
        int y = Coords.y(st.npcPos[i]);
        int nx = x + ESCAPE_DX[d];
        int ny = y + ESCAPE_DY[d];
        st.clearFootprint(i, size);
        if (MoveNaive.legal(st, st.frame.grid, nx, ny, size)) {
            st.npcPos[i] = Coords.pack(nx, ny);
            st.npcFlags[i] |= SimState.FLAG_MOVED;
        }
        st.setFootprint(i, size);
    }

    private void housekeeping(SimState st, Frame f, MechanicsData data, TickContext c, PackRules rules, int t) {
        PlayerData pd = data.player;
        if (st.attackCooldown > -1) {
            st.attackCooldown--;
        }
        if (st.foodDelay > 0) {
            st.foodDelay--;
        }
        if (st.potDelay > 0) {
            st.potDelay--;
        }
        if (st.karamDelay > 0) {
            st.karamDelay--;
        }

        if (st.overhead != 0 && !st.prayerJustActivated && st.prayerExpected > 0) {
            int prayerBonus = 0;
            int offensiveDrain = 0;
            if (f.equipSets != null && st.gearSet < f.equipSets.length && f.equipSets[st.gearSet] != null) {
                EquipSet set = f.equipSets[st.gearSet];
                prayerBonus = set.prayerBonus;
                offensiveDrain = set.offensiveDrain;
            }
            int effect = pd.prayerDrainEffect + offensiveDrain;
            int resistance = pd.prayerResistBonusMult * prayerBonus + pd.prayerResistBase;
            st.prayerDrainAcc += (short) effect;
            while (st.prayerDrainAcc >= resistance) {
                st.prayerDrainAcc -= (short) resistance;
                if (st.prayerExpected > 0) {
                    st.prayerExpected--;
                    st.accPrayerDrained++;
                }
            }
        }
        if (st.prayerExpected <= 0) {
            st.overhead = 0;
        }

        if (++st.specRegenCounter >= (f.lightbearer ? pd.specRegenTicksLightbearer : pd.specRegenTicks)) {
            st.specRegenCounter = 0;
            st.specEnergy10 = (short) Math.min(1000, st.specEnergy10 + pd.specRegenAmount10);
        }

        if ((st.venomState & 1) != 0) {
            int damage = st.venomState >>> 1 & 0x3f;
            int counter = (st.venomState >>> 7 & 0x3f) - 1;
            if (counter <= 0) {
                Hits.damagePlayer(st, damage, damage, 1);
                damage = Math.min(pd.venomMaxDamage, damage + pd.venomStepDamage);
                counter = pd.venomIntervalTicks;
            }
            st.venomState = 1 | damage << 1 | counter << 7;
        }

        if (Integer.bitCount(st.tickStyleMask) >= 2) {
            st.accAlignmentEvents++;
            int sum = st.tickRangedWorst + st.tickMagicWorst + st.tickMeleeWorst;
            st.accResidualWorst += sum - Math.max(st.tickRangedWorst, Math.max(st.tickMagicWorst, st.tickMeleeWorst));
        }
        st.accLosExposureTicks += st.tickLosExposure;
        if (tickObserver != null) {
            tickObserver.afterPlayerTurn(st);
        }
        int drainPct = f.damage.prayerDrainPctOfDamage;
        if (drainPct > 0) {
            int worstDrain = st.tickBurstWorst * drainPct / 100;
            st.prayerWorst = (short) Math.min(st.prayerWorst, st.prayerExpected - worstDrain);
        } else {
            st.prayerWorst = (short) Math.min(st.prayerWorst, st.prayerExpected);
        }
        int floorCandidate = st.hpExpected + st.tickExpectedDamage - st.tickBurstWorst;
        if (floorCandidate < st.hpWorstFloor) {
            st.hpWorstFloor = (short) floorCandidate;
        }

        if (st.waveClearTick < 0) {
            boolean cleared = true;
            for (int oi = 0; oi < f.npcOrderCount && cleared; oi++) {
                int i = f.npcOrder[oi];
                cleared = !(st.alive(i) && f.def(i).waveClearing);
            }
            if (cleared) {
                // Scheduled spawns arriving exactly as the wave clears never happen.
                st.waveClearTick = (short) t;
            }
        }
        if ((f.packHooks & PackRules.HOOK_END_OF_TICK) != 0) {
            rules.endOfTick(c);
        }
    }

    private void applyCommand(SimState st, long cmd) {
        if (cmd == Command.NONE) {
            return;
        }
        Frame f = st.frame;
        int oh = Command.overheadOf(cmd);
        if (oh == Command.OVERHEAD_OFF) {
            st.overhead = 0;
        } else if (oh >= Command.OVERHEAD_MELEE) {
            if (st.overhead == 0) {
                st.prayerJustActivated = true;
            }
            st.prayerSwitched = st.overhead != oh - 1;
            st.overhead = (byte) (oh - 1);
        }
        int gear = Command.gearSetOf(cmd);
        if (gear > 0) {
            st.gearSet = (byte) (gear - 1);
        }
        int cons = Command.consumablesOf(cmd);
        if (cons != 0) {
            Consumables.consume(st, f, cons);
        }
        if (Command.hasMove(cmd)) {
            int dest = Command.dest(cmd);
            st.targetSlot = -1;
            st.moveDest = (short) dest;
            st.pathKey = st.playerPos << 12 | dest;
            st.pathCursor = 0;
            st.pathId = (short) scratch.findOrComputePath(f.grid, st.playerPos, dest);
        }
        int atk = Command.attackOf(cmd);
        if (atk >= Command.ATTACK_SLOT_BASE) {
            int slot = atk - Command.ATTACK_SLOT_BASE;
            if (st.alive(slot)) {
                st.targetSlot = (byte) slot;
                st.moveDest = Coords.NONE;
                st.pathKey = -1;
                st.pathId = -1;
                st.pathCursor = 0;
            }
        }
        if (Command.specOf(cmd)) {
            st.specArmed = true;
        }
    }
}
