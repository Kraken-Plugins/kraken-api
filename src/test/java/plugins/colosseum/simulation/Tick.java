package plugins.colosseum.simulation;

/**
 * The colosseum tick engine: advances a {@link State} by exactly one game tick given a
 * {@link PlayerCommand}.
 *
 * <p>Phase order per tick follows the real engine (client input, then NPC turns, then the
 * player turn) as documented by the community engine references, and NPC behaviour is
 * parity-modelled on the community wave simulator that has been validated against the live
 * arena:</p>
 *
 * <ol>
 *   <li><b>Client input</b> - the command's prayer/gear/consumable/click intents apply.
 *   Because the real client processes clicks at the start of the next tick, a decision made
 *   while watching tick T-1 takes effect here on tick T - exactly how flicking works.</li>
 *   <li><b>NPC turns</b> - player-targeted hits were rolled on previous ticks; now each NPC
 *   (in processing order) counts down its cooldown, moves if it has no line of sight
 *   (route-finders descend a BFS approach field, everything else uses the naive
 *   diagonal-then-cardinal chase step), manticores charge as a group, and attackers with
 *   line of sight and an expired cooldown launch attacks. Protection prayers are evaluated
 *   at launch (the prayer active at the start of the attack tick), so launched hits carry
 *   final damage; only the minotaur's delayed melee re-checks prayer on landing.</li>
 *   <li><b>Player turn</b> - queued hits landing this tick apply (tick-eating works because
 *   phase 1 healing already happened), then movement (walk 1 / run 2 tiles along a BFS
 *   field toward the clicked destination), then the post-movement attack attempt.</li>
 *   <li><b>Housekeeping</b> - cooldown decrements, prayer drain, run energy and special
 *   attack regeneration.</li>
 * </ol>
 *
 * <p>Damage is tracked as expected value (accuracy x mean roll) for optimisation plus a
 * parallel worst-case burst floor for lethality checks; both live on the state.</p>
 */
public final class Tick {
    /** Hit style: typeless (prayer never applies). */
    public static final int STYLE_TYPELESS = 0;
    /** Hit style: melee. */
    public static final int STYLE_MELEE = 1;
    /** Hit style: ranged. */
    public static final int STYLE_RANGED = 2;
    /** Hit style: magic. */
    public static final int STYLE_MAGIC = 3;

    /** Manticore pattern: not yet known. */
    public static final int PATTERN_UNKNOWN = 0;
    /** Manticore pattern: ranged, magic, melee. */
    public static final int PATTERN_RANGED_FIRST = 1;
    /** Manticore pattern: magic, ranged, melee. */
    public static final int PATTERN_MAGIC_FIRST = 2;
    /** Mantimayhem III pattern: melee, ranged, magic. */
    public static final int PATTERN_MELEE_RANGED_MAGIC = 3;
    /** Mantimayhem III pattern: melee, magic, ranged. */
    public static final int PATTERN_MELEE_MAGIC_RANGED = 4;
    /** Mantimayhem III pattern: ranged, melee, magic. */
    public static final int PATTERN_RANGED_MELEE_MAGIC = 5;
    /** Mantimayhem III pattern: magic, melee, ranged. */
    public static final int PATTERN_MAGIC_MELEE_RANGED = 6;

    /** Per-orb style lookup indexed by [patternCode][orbIndex]; pattern 0 is unknown. */
    public static final int[][] ORB_STYLES = {
            {STYLE_TYPELESS, STYLE_TYPELESS, STYLE_TYPELESS},
            {STYLE_RANGED, STYLE_MAGIC, STYLE_MELEE},
            {STYLE_MAGIC, STYLE_RANGED, STYLE_MELEE},
            {STYLE_MELEE, STYLE_RANGED, STYLE_MAGIC},
            {STYLE_MELEE, STYLE_MAGIC, STYLE_RANGED},
            {STYLE_RANGED, STYLE_MELEE, STYLE_MAGIC},
            {STYLE_MAGIC, STYLE_MELEE, STYLE_RANGED},
    };

    /** Manticore per-style max hits (wiki: melee 31, ranged 36, magic 31). */
    public static final int MANTICORE_MAX_HIT_MELEE = 31;
    public static final int MANTICORE_MAX_HIT_RANGED = 36;
    public static final int MANTICORE_MAX_HIT_MAGIC = 31;

    /** Jaguar warriors roll three independent melee hits per attack. */
    public static final int JAGUAR_HITS_PER_ATTACK = 3;

    /** Sky javelins always land when stood on; expected damage is half the max roll. */
    private static final double TYPELESS_EXPECTED_FACTOR = 0.5;

    // ----- pending hit encoding -----
    private static final long HIT_LAND_MASK = 0xFFFFL;
    private static final int HIT_DMG_SHIFT = 16;
    private static final int HIT_STYLE_SHIFT = 24;
    private static final int HIT_SLOT_SHIFT = 26;
    private static final int HIT_TILE_SHIFT = 32;
    private static final long HIT_PRAYER_RESOLVED = 1L << 44;
    private static final int HIT_EXPECTED_SHIFT = 45;
    private static final long HIT_TARGET_NPC = 1L << 53;
    private static final int HIT_TILE_NONE = 0xFFF;

    /**
     * Observer for NPC attack launches, wired through {@link Scratch#attackListener}.
     */
    public interface AttackListener {
        /**
         * @param slot attacking npc slot.
         * @param styleCode launched style (STYLE_*; typeless for sky javelins/unknown orbs).
         * @param tick engine tick of the launch.
         * @param special true for manticore orbs and sky javelins.
         */
        void onAttack(int slot, int styleCode, int tick, boolean special);
    }

    private Tick() {
    }

    /**
     * Advances the state by one tick.
     *
     * @param s mutable state (modified in place).
     * @param cmd packed player command for this tick (see {@link PlayerCommand}).
     * @param scratch reusable working memory.
     */
    public static void advance(State s, long cmd, Scratch scratch) {
        Frame frame = s.frame;
        int tick = s.tick;

        boolean overheadActivated = applyCommand(s, cmd);

        boolean canMove = !frame.isWaveStartGates() || tick >= Constants.WAVE_START_MOVE_GATE_TICKS;
        boolean canGainLos = !frame.isWaveStartGates() || tick >= Constants.WAVE_START_LOS_GATE_TICKS;
        boolean canAttack = !frame.isWaveStartGates() || tick >= Constants.WAVE_START_ATTACK_GATE_TICKS;

        resolveNpcTargetedHits(s);
        npcMovementPhase(s, scratch, canMove, canGainLos);
        manticoreChargingPhase(s, canAttack);
        npcAttackPhase(s, scratch, canAttack);

        resolvePlayerTargetedHits(s);
        boolean ranThisTick = movePlayer(s, scratch);
        playerAttack(s, cmd);

        housekeeping(s, overheadActivated, ranThisTick);
        s.tick = tick + 1;
    }

    // ------------------------------------------------------------------
    // Phase 1: client input
    // ------------------------------------------------------------------

    private static boolean applyCommand(State s, long cmd) {
        Frame frame = s.frame;
        LoadoutConfig loadout = frame.getLoadout();
        boolean overheadActivated = false;

        int overheadCmd = PlayerCommand.overhead(cmd);
        if (overheadCmd == PlayerCommand.OVERHEAD_OFF) {
            s.overhead = State.OVERHEAD_NONE;
        } else if (overheadCmd >= PlayerCommand.OVERHEAD_MELEE && overheadCmd <= PlayerCommand.OVERHEAD_MAGIC) {
            byte target = (byte) (overheadCmd - 1);
            if (s.prayerPoints > 0 && s.overhead != target) {
                s.overhead = target;
                overheadActivated = true;
            }
        }

        int gearSet = PlayerCommand.gearSet(cmd);
        if (gearSet >= 0 && gearSet < loadout.gearSetCount()) {
            s.gearSet = (byte) gearSet;
        }

        if (PlayerCommand.eatFood(cmd) && s.foodDelay <= 0 && s.foodCount > 0) {
            s.playerHp = Math.min(frame.getPlayerMaxHp(), s.playerHp + loadout.getFoodHealAmount());
            s.foodCount--;
            s.foodDelay = 3;
            s.attackDelay = (byte) Math.min(127, s.attackDelay + 3);
            s.suppliesUsed++;
        }
        if (PlayerCommand.eatCombo(cmd) && s.comboDelay <= 0 && s.comboCount > 0) {
            s.playerHp = Math.min(frame.getPlayerMaxHp(), s.playerHp + loadout.getComboHealAmount());
            s.comboCount--;
            s.comboDelay = 3;
            s.attackDelay = (byte) Math.min(127, s.attackDelay + 2);
            s.suppliesUsed++;
        }
        if (PlayerCommand.sipBrew(cmd) && s.potionDelay <= 0 && s.brewDoses > 0) {
            s.playerHp = Math.min(frame.getPlayerMaxHp(), s.playerHp + loadout.getBrewHealPerDose());
            s.brewDoses--;
            s.potionDelay = 3;
            s.suppliesUsed++;
        }
        if (PlayerCommand.sipRestore(cmd) && s.potionDelay <= 0 && s.restoreDoses > 0) {
            s.prayerPoints = Math.min(frame.getPrayerPointCap(), s.prayerPoints + loadout.getRestorePrayerPerDose());
            s.restoreDoses--;
            s.potionDelay = 3;
            s.suppliesUsed++;
        }

        if (PlayerCommand.stop(cmd)) {
            s.moveTarget = Coords.NONE;
        }
        short moveTarget = PlayerCommand.moveTarget(cmd);
        if (Coords.isPresent(moveTarget)) {
            s.moveTarget = moveTarget;
            s.moveRun = PlayerCommand.run(cmd);
            // A ground click cancels the current interaction; an attack in the same command
            // (applied below) re-targets afterwards.
            s.attackTargetSlot = -1;
        }

        int attackTarget = PlayerCommand.attackTarget(cmd);
        if (attackTarget == -2) {
            s.attackTargetSlot = -1;
        } else if (attackTarget >= 0 && attackTarget < frame.getNpcSlotCount()) {
            // An attack click replaces the current interaction, cancelling any ground path
            // (the player attacks from where they stand when in range).
            s.attackTargetSlot = (byte) attackTarget;
            s.moveTarget = Coords.NONE;
        }
        return overheadActivated;
    }

    // ------------------------------------------------------------------
    // Phase 2: NPC turns
    // ------------------------------------------------------------------

    private static void npcMovementPhase(State s, Scratch scratch, boolean canMove, boolean canGainLos) {
        Frame frame = s.frame;
        byte[] order = frame.getProcessingOrder();
        java.util.Arrays.fill(scratch.npcMoved, false);

        for (byte slot : order) {
            if (!s.npcActive(slot)) {
                continue;
            }
            if (s.npcCooldown[slot] > -100) {
                s.npcCooldown[slot]--;
            }
            if (!canMove) {
                continue;
            }
            NpcType type = frame.type(slot);
            boolean holdsPosition = canGainLos && npcHasLosToPlayer(s, slot);
            if (holdsPosition) {
                continue;
            }
            short next = type.isSmartPathing()
                    ? chooseSmartStep(s, scratch, slot, type)
                    : chooseDumbStep(s, slot, type);
            if (Coords.isPresent(next)) {
                s.npcPos[slot] = next;
                scratch.npcMoved[slot] = true;
            }
        }
    }

    /**
     * Predicts the tile an NPC would step to this tick if it moved (ignoring wave-start
     * gating and the has-line-of-sight hold). Used by the planner's prayer policy to
     * anticipate attackers that step into line of sight and attack on the same tick.
     *
     * @param s state.
     * @param scratch scratch memory (for route-finder approach fields).
     * @param slot npc slot.
     * @return packed next anchor, or {@link Coords#NONE} when the npc would not move.
     */
    public static short predictNextNpcPos(State s, Scratch scratch, int slot) {
        NpcType type = s.frame.type(slot);
        return type.isSmartPathing()
                ? chooseSmartStep(s, scratch, slot, type)
                : chooseDumbStep(s, slot, type);
    }

    private static short chooseDumbStep(State s, int slot, NpcType type) {
        int size = type.getSize();
        int nx = Coords.x(s.npcPos[slot]);
        int ny = Coords.y(s.npcPos[slot]);
        int px = Coords.x(s.playerPos);
        int py = Coords.y(s.playerPos);

        int dxs = Integer.signum(px - nx);
        int dys = Integer.signum(py - ny);
        if (dxs == 0 && dys == 0) {
            return Coords.NONE;
        }
        // Cancel the diagonal when it would step onto the player (community-sim rule).
        if (LineOfSight.overlaps(nx + dxs, ny + dys, size, px, py)) {
            dys = 0;
        }

        if (isNpcPlacementLegal(s, slot, nx + dxs, ny + dys, size)
                && (size > 1 || dxs == 0 || dys == 0
                || (isNpcPlacementLegal(s, slot, nx + dxs, ny, size) && isNpcPlacementLegal(s, slot, nx, ny + dys, size)))) {
            return Coords.pack(nx + dxs, ny + dys);
        }
        if (dxs != 0 && isNpcPlacementLegal(s, slot, nx + dxs, ny, size)) {
            return Coords.pack(nx + dxs, ny);
        }
        if (dys != 0 && isNpcPlacementLegal(s, slot, nx, ny + dys, size)) {
            return Coords.pack(nx, ny + dys);
        }
        return Coords.NONE;
    }

    private static short chooseSmartStep(State s, Scratch scratch, int slot, NpcType type) {
        Grid grid = s.frame.getGrid();
        int size = type.getSize();
        byte[] field = scratch.approachField(grid, s.playerPos, size);
        int nx = Coords.x(s.npcPos[slot]);
        int ny = Coords.y(s.npcPos[slot]);
        int px = Coords.x(s.playerPos);
        int py = Coords.y(s.playerPos);
        int current = field[(ny << 6) | nx] & 0xFF;
        if (current == 0 || current >= Scratch.UNREACHABLE) {
            return Coords.NONE;
        }

        int dxs = Integer.signum(px - nx);
        int dys = Integer.signum(py - ny);
        int bestDist = current;
        int bestX = nx;
        int bestY = ny;
        // Preference order mirrors the naive chase: diagonal toward the player, then the
        // x-axis, then the y-axis, then the remaining directions.
        for (int i = 0; i < 8; i++) {
            int dx;
            int dy;
            switch (i) {
                case 0: dx = dxs; dy = dys; break;
                case 1: dx = dxs; dy = 0; break;
                case 2: dx = 0; dy = dys; break;
                default:
                    dx = Scratch.DIR_X[i - 3];
                    dy = Scratch.DIR_Y[i - 3];
                    break;
            }
            if (dx == 0 && dy == 0) {
                continue;
            }
            int cx = nx + dx;
            int cy = ny + dy;
            if (cx < 0 || cy < 0 || cx >= grid.width() || cy >= grid.height()) {
                continue;
            }
            int dist = field[(cy << 6) | cx] & 0xFF;
            if (dist >= bestDist) {
                continue;
            }
            if (size == 1 && dx != 0 && dy != 0
                    && (!grid.isFootprintFree(nx + dx, ny, 1) || !grid.isFootprintFree(nx, ny + dy, 1))) {
                continue;
            }
            if (!isNpcPlacementLegal(s, slot, cx, cy, size)) {
                continue;
            }
            if (LineOfSight.overlaps(cx, cy, size, px, py)) {
                continue;
            }
            bestDist = dist;
            bestX = cx;
            bestY = cy;
        }
        if (bestX == nx && bestY == ny) {
            return Coords.NONE;
        }
        return Coords.pack(bestX, bestY);
    }

    private static boolean isNpcPlacementLegal(State s, int movingSlot, int x, int y, int size) {
        if (!s.frame.getGrid().isFootprintFree(x, y, size)) {
            return false;
        }
        for (int other = 0; other < s.frame.getNpcSlotCount(); other++) {
            if (other == movingSlot || !s.npcActive(other)) {
                continue;
            }
            int ox = Coords.x(s.npcPos[other]);
            int oy = Coords.y(s.npcPos[other]);
            int otherSize = s.frame.size(other);
            if (x <= ox + otherSize - 1 && x + size - 1 >= ox && y <= oy + otherSize - 1 && y + size - 1 >= oy) {
                return false;
            }
        }
        return true;
    }

    private static void manticoreChargingPhase(State s, boolean canAttack) {
        if (!canAttack) {
            return;
        }
        Frame frame = s.frame;

        // Established pattern from manticores already charging or charged.
        int establishedPattern = PATTERN_UNKNOWN;
        for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
            if (s.npcActive(slot) && frame.type(slot) == NpcType.MANTICORE && s.npcChargingStarted(slot)) {
                int pattern = s.npcAux[slot] & 7;
                if (pattern != PATTERN_UNKNOWN) {
                    establishedPattern = pattern;
                    break;
                }
            }
        }
        // Manticores that begin charging on the same tick pick one shared pattern; if any of
        // them has a known pattern (from live tracking) the whole group copies it.
        if (establishedPattern == PATTERN_UNKNOWN) {
            for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
                if (!s.npcActive(slot) || frame.type(slot) != NpcType.MANTICORE || s.npcChargingStarted(slot)) {
                    continue;
                }
                if (!npcHasLosToPlayer(s, slot)) {
                    continue;
                }
                int ownPattern = s.npcAux[slot] & 7;
                if (ownPattern != PATTERN_UNKNOWN) {
                    establishedPattern = ownPattern;
                    break;
                }
            }
        }
        for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
            if (!s.npcActive(slot) || frame.type(slot) != NpcType.MANTICORE || s.npcChargingStarted(slot)) {
                continue;
            }
            if (!npcHasLosToPlayer(s, slot)) {
                continue;
            }
            int ownPattern = s.npcAux[slot] & 7;
            int pattern = establishedPattern != PATTERN_UNKNOWN ? establishedPattern : ownPattern;
            s.npcAux[slot] = (byte) ((s.npcAux[slot] & ~7) | (pattern & 7));
            s.npcFlags[slot] |= State.NPC_FLAG_CHARGING_STARTED;
            s.npcCooldown[slot] = (byte) Constants.MANTICORE_CHARGE_TICKS;
        }
    }

    private static void npcAttackPhase(State s, Scratch scratch, boolean canAttack) {
        Frame frame = s.frame;
        boolean manticoreFired = false;

        if (canAttack) {
            for (byte slot : frame.getProcessingOrder()) {
                if (!s.npcActive(slot)) {
                    continue;
                }
                NpcType type = frame.type(slot);
                if (!npcHasLosToPlayer(s, slot)) {
                    continue;
                }
                switch (type.getSpecialKind()) {
                    case MANTICORE:
                        if (s.npcChargingStarted(slot) && s.npcCooldown[slot] <= 0 && !manticoreFired && orbsRemaining(s, slot) == 0) {
                            setOrbsRemaining(s, slot, Constants.MANTICORE_ORB_COUNT);
                            s.npcCooldown[slot] = (byte) type.getAttackSpeedTicks();
                            manticoreFired = true;
                        }
                        break;
                    case WARBAND:
                        if (s.tick % Constants.WARBAND_CYCLE_TICKS == frame.getWarbandCyclePhase()
                                && !scratch.npcMoved[slot]) {
                            emitStandardAttack(s, scratch, slot, type);
                        }
                        break;
                    case MINOTAUR_HEAL:
                        if (s.npcCooldown[slot] <= 0) {
                            emitMinotaurMelee(s, scratch, slot, type);
                            s.npcCooldown[slot] = (byte) type.getAttackSpeedTicks();
                        }
                        break;
                    case JAVELIN_SKY:
                        if (s.npcCooldown[slot] <= 0) {
                            emitJavelinAttack(s, scratch, slot, type);
                            s.npcCooldown[slot] = (byte) type.getAttackSpeedTicks();
                        }
                        break;
                    case NONE:
                    default:
                        if (s.npcCooldown[slot] <= 0) {
                            emitStandardAttack(s, scratch, slot, type);
                            s.npcCooldown[slot] = (byte) type.getAttackSpeedTicks();
                        }
                        break;
                }
            }

            // Minotaurs with no player in melee reach spend their timer healing instead.
            for (byte slot : frame.getProcessingOrder()) {
                if (!s.npcActive(slot) || frame.type(slot).getSpecialKind() != NpcType.SpecialKind.MINOTAUR_HEAL) {
                    continue;
                }
                if (s.npcCooldown[slot] <= 0 && !npcHasLosToPlayer(s, slot)) {
                    if (tryMinotaurHeal(s, slot)) {
                        s.npcCooldown[slot] = (byte) frame.type(slot).getAttackSpeedTicks();
                    }
                }
            }
        }

        if (manticoreFired) {
            for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
                if (s.npcActive(slot)
                        && frame.type(slot) == NpcType.MANTICORE
                        && s.npcChargingStarted(slot)
                        && s.npcCooldown[slot] <= 0
                        && orbsRemaining(s, slot) == 0) {
                    s.npcCooldown[slot] = (byte) Constants.MANTICORE_STAGGER_TICKS;
                }
            }
        }

        // Orb emission: once a triple starts, one orb launches per tick regardless of
        // line of sight, including the tick the triple began.
        for (int slot = 0; slot < frame.getNpcSlotCount(); slot++) {
            if (!s.npcActive(slot) || frame.type(slot) != NpcType.MANTICORE) {
                continue;
            }
            int remaining = orbsRemaining(s, slot);
            if (remaining <= 0) {
                continue;
            }
            emitManticoreOrb(s, scratch, slot, Constants.MANTICORE_ORB_COUNT - remaining);
            setOrbsRemaining(s, slot, remaining - 1);
        }
    }

    private static void emitStandardAttack(State s, Scratch scratch, int slot, NpcType type) {
        int style = styleCode(type);
        int maxHit = type.getMaxHit();
        if (type == NpcType.JAGUAR_WARRIOR) {
            maxHit *= JAGUAR_HITS_PER_ATTACK;
        }
        boolean blocked = style != STYLE_TYPELESS && s.overhead == style;
        int maxAfter = blocked ? 0 : maxHit;
        int expected = blocked ? 0 : (int) Math.round(maxHit * s.frame.getLoadout().getNpcExpectedDamageFactor());
        int delay = hitDelay(style, npcDistanceToPlayer(s, slot));
        queueHit(s, encodeHit(s.tick + delay, maxAfter, style, slot, HIT_TILE_NONE, true, expected, false));
        if (scratch.attackListener != null) {
            scratch.attackListener.onAttack(slot, style, s.tick, false);
        }
    }

    private static void emitJavelinAttack(State s, Scratch scratch, int slot, NpcType type) {
        int autos = s.npcAux[slot] & 7;
        if (autos >= Constants.JAVELIN_SPECIAL_EVERY_N_ATTACKS - 1) {
            s.npcAux[slot] = (byte) (s.npcAux[slot] & ~7);
            int max = Constants.JAVELIN_SKY_MAX_DAMAGE;
            int expected = (int) Math.round(max * TYPELESS_EXPECTED_FACTOR);
            queueHit(s, encodeHit(
                    s.tick + Constants.JAVELIN_SKY_LAND_DELAY_TICKS,
                    max,
                    STYLE_TYPELESS,
                    slot,
                    s.playerPos & 0xFFF,
                    true,
                    expected,
                    false
            ));
            if (scratch.attackListener != null) {
                scratch.attackListener.onAttack(slot, STYLE_TYPELESS, s.tick, true);
            }
        } else {
            s.npcAux[slot] = (byte) ((s.npcAux[slot] & ~7) | (autos + 1));
            emitStandardAttack(s, scratch, slot, type);
        }
    }

    private static void emitMinotaurMelee(State s, Scratch scratch, int slot, NpcType type) {
        // Damage applies one tick after the attack and prayer is evaluated on the landing
        // tick (the minotaur's delayed hit is both tick-eatable and late-prayable).
        int expected = (int) Math.round(type.getMaxHit() * s.frame.getLoadout().getNpcExpectedDamageFactor());
        queueHit(s, encodeHit(
                s.tick + Constants.MINOTAUR_HIT_DELAY_TICKS,
                type.getMaxHit(),
                STYLE_MELEE,
                slot,
                HIT_TILE_NONE,
                false,
                expected,
                false
        ));
        if (scratch.attackListener != null) {
            scratch.attackListener.onAttack(slot, STYLE_MELEE, s.tick, false);
        }
    }

    private static boolean tryMinotaurHeal(State s, int slot) {
        Frame frame = s.frame;
        int size = frame.size(slot);
        int centerX = Coords.x(s.npcPos[slot]) + (size - 1) / 2;
        int centerY = Coords.y(s.npcPos[slot]) + (size - 1) / 2;

        int bestTarget = -1;
        double lowestFraction = Constants.MINOTAUR_HEAL_THRESHOLD;
        for (int other = 0; other < frame.getNpcSlotCount(); other++) {
            if (other == slot || !s.npcActive(other)) {
                continue;
            }
            if (frame.type(other).getSpecialKind() == NpcType.SpecialKind.MINOTAUR_HEAL) {
                continue;
            }
            int otherSize = frame.size(other);
            int otherCenterX = Coords.x(s.npcPos[other]) + (otherSize - 1) / 2;
            int otherCenterY = Coords.y(s.npcPos[other]) + (otherSize - 1) / 2;
            int distance = Math.max(Math.abs(otherCenterX - centerX), Math.abs(otherCenterY - centerY));
            if (distance > Constants.MINOTAUR_HEAL_RANGE) {
                continue;
            }
            double fraction = s.npcHp[other] / (double) frame.getNpcMaxHp()[other];
            if (fraction >= lowestFraction) {
                continue;
            }
            if (!LineOfSight.tileToTile(frame.getGrid(), centerX, centerY, otherCenterX, otherCenterY)) {
                continue;
            }
            lowestFraction = fraction;
            bestTarget = other;
        }
        if (bestTarget < 0) {
            return false;
        }
        int healed = Math.min(frame.getNpcMaxHp()[bestTarget], s.npcHp[bestTarget] + Constants.MINOTAUR_HEAL_PER_CYCLE);
        s.npcHp[bestTarget] = (short) healed;
        return true;
    }

    private static void emitManticoreOrb(State s, Scratch scratch, int slot, int orbIndex) {
        int pattern = s.npcAux[slot] & 7;
        LoadoutConfig loadout = s.frame.getLoadout();
        double factor = loadout.getNpcExpectedDamageFactor();
        int maxAfter;
        int expected;
        int styleForRecord;

        if (pattern != PATTERN_UNKNOWN) {
            int style = ORB_STYLES[pattern][orbIndex];
            boolean blocked = s.overhead == style;
            int maxHit = manticoreMaxHit(style);
            maxAfter = blocked ? 0 : maxHit;
            expected = blocked ? 0 : (int) Math.round(maxHit * factor);
            styleForRecord = style;
        } else {
            // Unknown pattern: the last orb is always melee below Mantimayhem III; the first
            // two are ranged or magic with equal probability. Expected damage averages over
            // the possibilities given the current overhead; max damage assumes the worst.
            boolean lastOrbKnownMelee = s.frame.getMantimayhemTier() < 3 && orbIndex == 2;
            if (lastOrbKnownMelee) {
                boolean blocked = s.overhead == STYLE_MELEE;
                maxAfter = blocked ? 0 : MANTICORE_MAX_HIT_MELEE;
                expected = blocked ? 0 : (int) Math.round(MANTICORE_MAX_HIT_MELEE * factor);
                styleForRecord = STYLE_MELEE;
            } else {
                boolean blocksRanged = s.overhead == STYLE_RANGED;
                boolean blocksMagic = s.overhead == STYLE_MAGIC;
                int maxRanged = blocksRanged ? 0 : MANTICORE_MAX_HIT_RANGED;
                int maxMagic = blocksMagic ? 0 : MANTICORE_MAX_HIT_MAGIC;
                maxAfter = Math.max(maxRanged, maxMagic);
                expected = (int) Math.round((maxRanged * factor + maxMagic * factor) / 2.0);
                styleForRecord = STYLE_TYPELESS;
            }
        }
        queueHit(s, encodeHit(s.tick, maxAfter, styleForRecord, slot, HIT_TILE_NONE, true, expected, false));
        if (scratch.attackListener != null) {
            scratch.attackListener.onAttack(slot, styleForRecord, s.tick, true);
        }
    }

    private static int manticoreMaxHit(int style) {
        switch (style) {
            case STYLE_MELEE:
                return MANTICORE_MAX_HIT_MELEE;
            case STYLE_MAGIC:
                return MANTICORE_MAX_HIT_MAGIC;
            case STYLE_RANGED:
            default:
                return MANTICORE_MAX_HIT_RANGED;
        }
    }

    // ------------------------------------------------------------------
    // Phase 3: player turn
    // ------------------------------------------------------------------

    private static void resolveNpcTargetedHits(State s) {
        int i = 0;
        while (i < s.pendingHitCount) {
            long hit = s.pendingHits[i];
            if ((hit & HIT_TARGET_NPC) == 0 || (int) (hit & HIT_LAND_MASK) != s.tick) {
                i++;
                continue;
            }
            int slot = (int) ((hit >> HIT_SLOT_SHIFT) & 63);
            int damage = (int) ((hit >> HIT_EXPECTED_SHIFT) & 0xFF);
            if (slot < s.frame.getNpcSlotCount() && s.npcActive(slot)) {
                int remaining = s.npcHp[slot] - damage;
                s.damageDealt += Math.min(damage, s.npcHp[slot]);
                if (remaining <= 0) {
                    s.npcHp[slot] = 0;
                    s.npcFlags[slot] = 0;
                    s.npcAux[slot] = 0;
                    s.kills++;
                } else {
                    s.npcHp[slot] = (short) remaining;
                }
            }
            s.pendingHits[i] = s.pendingHits[--s.pendingHitCount];
        }
    }

    private static void resolvePlayerTargetedHits(State s) {
        int burstMax = 0;
        int expectedSum = 0;
        int i = 0;
        while (i < s.pendingHitCount) {
            long hit = s.pendingHits[i];
            if ((hit & HIT_TARGET_NPC) != 0 || (int) (hit & HIT_LAND_MASK) != s.tick) {
                i++;
                continue;
            }
            int targetTile = (int) ((hit >> HIT_TILE_SHIFT) & 0xFFF);
            boolean applies = targetTile == HIT_TILE_NONE || targetTile == (s.playerPos & 0xFFF);
            if (applies && (hit & HIT_PRAYER_RESOLVED) == 0) {
                int style = (int) ((hit >> HIT_STYLE_SHIFT) & 3);
                if (style != STYLE_TYPELESS && s.overhead == style) {
                    applies = false;
                }
            }
            if (applies) {
                burstMax += (int) ((hit >> HIT_DMG_SHIFT) & 0xFF);
                expectedSum += (int) ((hit >> HIT_EXPECTED_SHIFT) & 0xFF);
            }
            s.pendingHits[i] = s.pendingHits[--s.pendingHitCount];
        }
        if (burstMax > 0) {
            s.worstCaseHpFloor = Math.min(s.worstCaseHpFloor, s.playerHp - burstMax);
        }
        if (expectedSum > 0) {
            s.expectedDamageTaken += expectedSum;
            s.playerHp -= expectedSum;
            if (s.playerHp <= 0) {
                s.playerHp = 0;
                s.playerDied = true;
            }
        }
    }

    private static boolean movePlayer(State s, Scratch scratch) {
        if (!Coords.isPresent(s.moveTarget) || s.playerDied) {
            return false;
        }
        if (s.moveTarget == s.playerPos) {
            s.moveTarget = Coords.NONE;
            return false;
        }
        Grid grid = s.frame.getGrid();
        byte[] field = scratch.playerField(grid, s.moveTarget);
        boolean wantRun = s.moveRun && s.runEnergyUnits > 0;
        int steps = wantRun ? 2 : 1;
        int taken = 0;
        for (int i = 0; i < steps && s.playerPos != s.moveTarget; i++) {
            int cx = Coords.x(s.playerPos);
            int cy = Coords.y(s.playerPos);
            int current = field[(cy << 6) | cx] & 0xFF;
            if (current >= Scratch.UNREACHABLE) {
                s.moveTarget = Coords.NONE;
                break;
            }
            int px = Coords.x(s.moveTarget);
            int py = Coords.y(s.moveTarget);
            int dxs = Integer.signum(px - cx);
            int dys = Integer.signum(py - cy);
            int bestDist = current;
            int bestX = cx;
            int bestY = cy;
            for (int d = 0; d < 8; d++) {
                int dx;
                int dy;
                switch (d) {
                    case 0: dx = dxs; dy = dys; break;
                    case 1: dx = dxs; dy = 0; break;
                    case 2: dx = 0; dy = dys; break;
                    default:
                        dx = Scratch.DIR_X[d - 3];
                        dy = Scratch.DIR_Y[d - 3];
                        break;
                }
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = cx + dx;
                int ny = cy + dy;
                if (!grid.inBounds(nx, ny)) {
                    continue;
                }
                int dist = field[(ny << 6) | nx] & 0xFF;
                if (dist >= bestDist) {
                    continue;
                }
                if (!Scratch.playerCanStep(grid, cx, cy, dx, dy)) {
                    continue;
                }
                bestDist = dist;
                bestX = nx;
                bestY = ny;
            }
            if (bestX == cx && bestY == cy) {
                break;
            }
            s.playerPos = Coords.pack(bestX, bestY);
            taken++;
        }
        if (s.playerPos == s.moveTarget) {
            s.moveTarget = Coords.NONE;
        }
        boolean ran = wantRun && taken > 0;
        if (ran) {
            LoadoutConfig loadout = s.frame.getLoadout();
            int weight = Math.max(0, Math.min(64, loadout.getWeightKg()));
            int base = 60 + (67 * weight) / 64;
            int drain = base * (300 - Math.min(299, loadout.getAgilityLevel())) / 300;
            if (loadout.isStaminaActive()) {
                drain = (drain * 3) / 10;
            }
            s.runEnergyUnits = Math.max(0, s.runEnergyUnits - drain);
        }
        return ran;
    }

    private static void playerAttack(State s, long cmd) {
        if (s.playerDied || s.attackTargetSlot < 0 || s.attackDelay > 0) {
            return;
        }
        int slot = s.attackTargetSlot;
        if (slot >= s.frame.getNpcSlotCount() || !s.npcActive(slot)) {
            return;
        }
        LoadoutConfig.GearSet gear = s.frame.getLoadout().gearSet(s.gearSet);
        if (!LineOfSight.tileHasLosToFootprint(
                s.frame.getGrid(),
                s.playerPos,
                gear.getAttackRange(),
                s.npcPos[slot],
                s.frame.size(slot)
        )) {
            return;
        }
        double damage = gear.expectedDamageVs(s.frame.type(slot));
        if (PlayerCommand.useSpec(cmd) && s.specEnergy >= 50) {
            s.specEnergy -= 50;
            damage *= 2.0;
        }
        int rounded = Math.max(0, (int) Math.round(damage));
        int style = styleCodeForPlayer(gear);
        int distance = Coords.chebyshev(s.playerPos, nearestNpcTile(s, slot));
        // +1: NPCs process before players, so player hits land one tick later than the
        // formula (receiver-processed-earlier rule).
        int landTick = s.tick + hitDelay(style, distance) + 1;
        queueHit(s, encodeHit(landTick, Math.min(255, rounded), style, slot, HIT_TILE_NONE, true, Math.min(255, rounded), true));
        s.attackDelay = (byte) gear.getAttackSpeedTicks();
    }

    private static void housekeeping(State s, boolean overheadActivated, boolean ranThisTick) {
        if (s.foodDelay > 0) {
            s.foodDelay--;
        }
        if (s.comboDelay > 0) {
            s.comboDelay--;
        }
        if (s.potionDelay > 0) {
            s.potionDelay--;
        }
        if (s.attackDelay > 0) {
            s.attackDelay--;
        }

        LoadoutConfig loadout = s.frame.getLoadout();
        if (s.overhead != State.OVERHEAD_NONE && !overheadActivated) {
            s.prayerDrainCounter += Constants.PROTECTION_PRAYER_DRAIN_EFFECT;
            int resistance = 2 * loadout.getPrayerBonus() + 60;
            if (s.prayerDrainCounter > resistance) {
                s.prayerDrainCounter -= resistance;
                s.prayerPoints--;
                if (s.prayerPoints <= 0) {
                    s.prayerPoints = 0;
                    s.overhead = State.OVERHEAD_NONE;
                }
            }
        }

        if (!ranThisTick) {
            s.runEnergyUnits = Math.min(10_000, s.runEnergyUnits + loadout.getAgilityLevel() / 10 + 15);
        }

        s.specRegenCounter++;
        int interval = loadout.isLightbearer()
                ? Constants.SPEC_REGEN_INTERVAL_TICKS / 2
                : Constants.SPEC_REGEN_INTERVAL_TICKS;
        if (s.specRegenCounter >= interval) {
            s.specRegenCounter = 0;
            s.specEnergy = Math.min(100, s.specEnergy + 10);
        }
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    /**
     * Whether an NPC currently has line of sight (and range) to the player.
     *
     * @param s state.
     * @param slot npc slot.
     * @return true when the npc could hit the player from where it stands.
     */
    public static boolean npcHasLosToPlayer(State s, int slot) {
        NpcType type = s.frame.type(slot);
        return LineOfSight.footprintHasLosTo(
                s.frame.getGrid(),
                s.npcPos[slot],
                type.getSize(),
                type.getAttackRange(),
                s.playerPos
        );
    }

    /**
     * Whether an NPC would have line of sight (and range) to a hypothetical player tile.
     *
     * @param s state.
     * @param slot npc slot.
     * @param tile packed tile to test.
     * @return true when the npc could hit that tile from where it stands.
     */
    public static boolean npcHasLosToTile(State s, int slot, short tile) {
        NpcType type = s.frame.type(slot);
        return LineOfSight.footprintHasLosTo(
                s.frame.getGrid(),
                s.npcPos[slot],
                type.getSize(),
                type.getAttackRange(),
                tile
        );
    }

    /**
     * Hit delay in ticks for a style at a Chebyshev distance (standard engine formulas;
     * manticore orbs bypass this with travel time zero).
     *
     * @param style style code.
     * @param distance Chebyshev distance.
     * @return ticks until the hit lands.
     */
    static int hitDelay(int style, int distance) {
        switch (style) {
            case STYLE_RANGED:
                return 1 + (3 + distance) / 6;
            case STYLE_MAGIC:
                return 1 + (1 + distance) / 3;
            case STYLE_MELEE:
            case STYLE_TYPELESS:
            default:
                return 0;
        }
    }

    private static int styleCode(NpcType type) {
        switch (type.getAttackStyle()) {
            case MELEE:
                return STYLE_MELEE;
            case RANGED:
                return STYLE_RANGED;
            case MAGIC:
                return STYLE_MAGIC;
            default:
                return STYLE_TYPELESS;
        }
    }

    private static int styleCodeForPlayer(LoadoutConfig.GearSet gear) {
        switch (gear.getStyle()) {
            case MELEE:
                return STYLE_MELEE;
            case RANGED:
                return STYLE_RANGED;
            case MAGIC:
                return STYLE_MAGIC;
            default:
                return STYLE_TYPELESS;
        }
    }

    private static int npcDistanceToPlayer(State s, int slot) {
        return Coords.chebyshev(nearestNpcTile(s, slot), s.playerPos);
    }

    private static short nearestNpcTile(State s, int slot) {
        int size = s.frame.size(slot);
        int nx = Coords.x(s.npcPos[slot]);
        int ny = Coords.y(s.npcPos[slot]);
        int px = Coords.x(s.playerPos);
        int py = Coords.y(s.playerPos);
        int cx = Math.max(nx, Math.min(nx + size - 1, px));
        int cy = Math.max(ny, Math.min(ny + size - 1, py));
        return Coords.pack(cx, cy);
    }

    private static int orbsRemaining(State s, int slot) {
        return (s.npcAux[slot] >> 3) & 3;
    }

    private static void setOrbsRemaining(State s, int slot, int value) {
        s.npcAux[slot] = (byte) ((s.npcAux[slot] & ~(3 << 3)) | ((value & 3) << 3));
    }

    private static long encodeHit(
            int landTick,
            int maxDamage,
            int style,
            int slot,
            int targetTile,
            boolean prayerResolved,
            int expectedDamage,
            boolean targetIsNpc
    ) {
        long hit = landTick & HIT_LAND_MASK;
        hit |= (long) Math.min(255, Math.max(0, maxDamage)) << HIT_DMG_SHIFT;
        hit |= (long) (style & 3) << HIT_STYLE_SHIFT;
        hit |= (long) (slot & 63) << HIT_SLOT_SHIFT;
        hit |= (long) (targetTile & 0xFFF) << HIT_TILE_SHIFT;
        if (prayerResolved) {
            hit |= HIT_PRAYER_RESOLVED;
        }
        hit |= (long) Math.min(255, Math.max(0, expectedDamage)) << HIT_EXPECTED_SHIFT;
        if (targetIsNpc) {
            hit |= HIT_TARGET_NPC;
        }
        return hit;
    }

    private static void queueHit(State s, long hit) {
        int maxAfter = (int) ((hit >> HIT_DMG_SHIFT) & 0xFF);
        int expected = (int) ((hit >> HIT_EXPECTED_SHIFT) & 0xFF);
        if (maxAfter <= 0 && expected <= 0) {
            return;
        }
        if (s.pendingHitCount < s.pendingHits.length) {
            s.pendingHits[s.pendingHitCount++] = hit;
            return;
        }
        // Ring is full; replace the least urgent (latest-landing) entry.
        int latestIndex = 0;
        long latestLand = -1;
        for (int i = 0; i < s.pendingHitCount; i++) {
            long land = s.pendingHits[i] & HIT_LAND_MASK;
            if (land > latestLand) {
                latestLand = land;
                latestIndex = i;
            }
        }
        s.pendingHits[latestIndex] = hit;
    }

    /**
     * Decodes a pending hit's landing tick, used by overlays and the scorer.
     *
     * @param hit packed hit.
     * @return absolute landing tick.
     */
    public static int hitLandTick(long hit) {
        return (int) (hit & HIT_LAND_MASK);
    }

    /** @param hit packed hit. @return worst-case damage.
     *             @return  maxHitDamage */
    public static int hitMaxDamage(long hit) {
        return (int) ((hit >> HIT_DMG_SHIFT) & 0xFF);
    }

    /** @param hit packed hit. @return expected damage.
     * @return hitExpectedDamage  */
    public static int hitExpectedDamage(long hit) {
        return (int) ((hit >> HIT_EXPECTED_SHIFT) & 0xFF);
    }

    /** @param hit packed hit. @return style code.
     *             @return hitStyle */
    public static int hitStyle(long hit) {
        return (int) ((hit >> HIT_STYLE_SHIFT) & 3);
    }

    /** @param hit packed hit. @return source (or target, for player hits) npc slot.
     *             @return hitSlot */
    public static int hitSlot(long hit) {
        return (int) ((hit >> HIT_SLOT_SHIFT) & 63);
    }

    /** @param hit packed hit. @return true when the hit targets an NPC rather than the player.
     *             @return hitTargetsNpc */
    public static boolean hitTargetsNpc(long hit) {
        return (hit & HIT_TARGET_NPC) != 0;
    }
}
