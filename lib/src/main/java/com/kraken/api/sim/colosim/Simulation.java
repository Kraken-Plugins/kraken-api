package com.kraken.api.sim.colosim;

import com.kraken.api.sim.colosim.model.Mob;
import com.kraken.api.sim.colosim.model.NpcInfo;
import com.kraken.api.sim.colosim.model.Tile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class Simulation {
    public static final int MAP_WIDTH = 34;
    public static final int MAP_HEIGHT = 34;

    public static final int MANTICORE_DELAY = 5;
    public static final int MANTICORE_CHARGE_TIME = 10;
    public static final int DELAY_FIRST_ATTACK_TICKS = 3;
    public static final int MINOTAUR_HEAL_RANGE = 7;

    public static final int MANTICORE = NpcType.MANTICORE.typeId;
    public static final int MINOTAUR = NpcType.MINOTAUR.typeId;

    private static final String[] STANDARD_PATTERNS = {"r", "m"};
    private static final String[] MM3_PATTERNS = {"r", "m", "Mrm", "Mmr", "rMm", "mMr"};
    private static final String[] MANTICORE_ATTACKS = {"range", "mage", "melee"};

    private static final int[][][] BLOCKED_TILE_RANGES = {
            {{0, 13}, {15, 19}, {21, 34}},
            {{0, 9}, {25, 34}},
            {{0, 7}, {27, 34}},
            {{0, 6}, {29, 34}},
            {{0, 5}, {29, 34}},
            {{0, 3}, {31, 34}},
            {{0, 3}, {31, 34}},
            {{0, 2}, {32, 34}},
            {{0, 2}, {32, 34}},
            {{0, 1}, {33, 34}},
            {{0, 1}, {33, 34}},
            {{0, 1}, {32, 34}},
            {{0, 1}, {31, 34}},
            {{31, 34}},
            {{31, 34}},
            {{0, 1}, {31, 34}},
            {{0, 1}, {31, 34}},
            {{0, 1}, {31, 34}},
            {{0, 1}, {31, 34}},
            {{31, 34}},
            {{31, 34}},
            {{0, 1}, {31, 34}},
            {{0, 1}, {32, 34}},
            {{0, 1}, {33, 34}},
            {{0, 1}, {33, 34}},
            {{0, 2}, {32, 34}},
            {{0, 2}, {32, 34}},
            {{0, 3}, {31, 34}},
            {{0, 3}, {31, 34}},
            {{0, 5}, {29, 34}},
            {{0, 5}, {29, 34}},
            {{0, 7}, {27, 34}},
            {{0, 9}, {25, 34}},
            {{0, 13}, {15, 19}, {21, 34}},
    };

    private static final Map<Integer, NpcInfo> NPC_INFO = new HashMap<>();
    private static final Map<String, int[]> MANTICORE_PATTERNS = new LinkedHashMap<>();

    static {
        NpcInfo shaman = new NpcInfo(1, 1, 10, 5, "cyan");
        NPC_INFO.put(NpcType.PLAYER.typeId, new NpcInfo(-1, 1, 10, 0, "red"));
        NPC_INFO.put(NpcType.SERPENT_SHAMAN.typeId, shaman);
        NPC_INFO.put(NpcType.JAVELIN_COLOSSUS.typeId, new NpcInfo(2, 3, 15, 5, "lime"));
        NPC_INFO.put(NpcType.JAGUAR_WARRIOR.typeId, new NpcInfo(4, 2, 1, 5, "orange"));
        NPC_INFO.put(NpcType.MANTICORE.typeId, new NpcInfo(0, 3, 15, 10, "purple"));
        NPC_INFO.put(NpcType.MINOTAUR.typeId, new NpcInfo(5, 3, 1, 5, "brown"));
        NPC_INFO.put(NpcType.SHOCKWAVE_COLOSSUS.typeId, new NpcInfo(3, 3, 15, 5, "blue"));
        NPC_INFO.put(NpcType.REINFORCEMENT_SHAMAN.typeId, new NpcInfo(6, 1, 10, 5, "cyan"));

        Map<String, int[]> base = new LinkedHashMap<>();
        base.put("r", new int[]{0, 1, 2});
        base.put("m", new int[]{1, 0, 2});
        base.put("Mrm", new int[]{2, 0, 1});
        base.put("Mmr", new int[]{2, 1, 0});
        base.put("rMm", new int[]{0, 2, 1});
        base.put("mMr", new int[]{1, 2, 0});
        MANTICORE_PATTERNS.putAll(base);
        for (Map.Entry<String, int[]> entry : base.entrySet()) {
            MANTICORE_PATTERNS.put("u" + entry.getKey(), entry.getValue());
        }
    }

    @AllArgsConstructor
    public static final class StepResult {
        public final int tick;
        public final int[] line;
    }

    @Getter
    private final int[][] pillars = {
            {8, 10},
            {23, 10},
            {8, 25},
            {23, 25},
    };
    private final boolean[] filters = {true, true, true, true};
    private final Random random = new Random();

    @Getter
    private final List<Mob> mobs = new ArrayList<>();
    @Getter
    private final List<int[]> tape = new ArrayList<>();
    private final List<Tile> playerTape = new ArrayList<>();
    private final Map<Integer, Integer> manticoreTicksRemaining = new HashMap<Integer, Integer>();

    private final Tile b5Tile = new Tile(7, 15);
    @Getter
    private Tile player = b5Tile.copy();
    private Tile stepStartPosition;

    @Setter
    @Getter
    private boolean fromWaveStart;

    @Getter
    @Setter
    private boolean mantimayhem3;

    @Getter
    private int tickCount;

    public int[][][] getBlockedTileRanges() {
        return BLOCKED_TILE_RANGES;
    }

    public boolean[] getPillarFilters() {
        return filters;
    }

    public void setPlayer(int x, int y) {
        player = new Tile(x, y);
    }

    public NpcInfo getNpcInfo(int type) {
        return NPC_INFO.get(type);
    }

    public String getNpcName(int type) {
        return NpcType.fromTypeId(type).name();
    }

    public String decodeManticoreAttack(int styleIndex) {
        if (styleIndex < 0 || styleIndex >= MANTICORE_ATTACKS.length) {
            return "?";
        }
        return MANTICORE_ATTACKS[styleIndex];
    }

    public int[] getManticorePattern(String name) {
        return MANTICORE_PATTERNS.get(name);
    }

    public int getMobCenterOffset(int type) {
        int size = getNpcInfo(type).getSize();
        return (size - 1) / 2;
    }

    public boolean placeMob(int x, int y, NpcType type, String extra) {
        if (type == NpcType.PLAYER) {
            return false;
        }
        for (Mob mob : mobs) {
            if (mob.getSpawnX() == x && mob.getSpawnY() == y) {
                return false;
            }
        }
        String originalExtra = null;
        if (type == NpcType.MANTICORE && extra != null) {
            originalExtra = extra;
        }
        mobs.add(new Mob(x, y, type.typeId, x, y, 0, extra, originalExtra));
        sortMobs();
        return true;
    }

    public void clear() {
        mobs.clear();
        tape.clear();
        playerTape.clear();
        manticoreTicksRemaining.clear();
        tickCount = 0;
        stepStartPosition = null;
        player = b5Tile.copy();
    }

    public void reset() {
        for (Mob mob : mobs) {
            mob.setX(mob.getSpawnX());
            mob.setY(mob.getSpawnY());
            mob.setCooldown(0);
            if (mob.getType() == MANTICORE && mob.getOriginalExtra() != null) {
                mob.setExtra(mob.getOriginalExtra());
            }
        }
        manticoreTicksRemaining.clear();
        tape.clear();
        playerTape.clear();
        tickCount = 0;
        if (stepStartPosition != null) {
            player = stepStartPosition.copy();
        }
    }

    public int findMobIndexAtTile(int x, int y) {
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            int size = getNpcInfo(mob.getType()).getSize();
            if (doesCollide(x, y, 1, mob.getX(), mob.getY(), size)) {
                return i;
            }
        }
        return -1;
    }

    public void removeMobAtTile(int x, int y) {
        int idx = findMobIndexAtTile(x, y);
        if (idx >= 0) {
            removeMob(idx);
        }
    }

    public StepResult step() {
        if (tickCount == 0) {
            stepStartPosition = player.copy();
        }

        int[] line = null;
        if (!mobs.isEmpty()) {
            boolean canAttack = !fromWaveStart || tickCount >= DELAY_FIRST_ATTACK_TICKS;
            boolean canMove = !fromWaveStart || tickCount > 0;
            boolean canGainLos = !fromWaveStart || tickCount > 1;

            moveMobs(canMove, canGainLos);
            handleManticoreCharging(canAttack);
            ProcessResult processResult = processAttacks(canAttack);
            line = processResult.line;
            recordManticoreOrbSequence(line);
            if (processResult.manticoreFired) {
                delayAllReadyMantis();
            }

            playerTape.add(player.copy());
            tape.add(line);
        }
        tickCount++;
        return new StepResult(tickCount, line);
    }

    public boolean hasLOS(int x1, int y1, int x2, int y2, int s, int r, boolean isNPC) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        if (isPillar(x1, y1) || isPillar(x2, y2) || doesCollide(x1, y1, s, x2, y2, 1)) {
            return false;
        }
        if (r == 1) {
            return (dx < s && dx >= 0 && (dy == 1 || dy == -s))
                    || (dy > -s && dy <= 0 && (dx == -1 || dx == s));
        }
        if (isNPC) {
            int tx = Math.max(x1, Math.min(x1 + s - 1, x2));
            int ty = Math.max(y1 - s + 1, Math.min(y1, y2));
            return hasLOS(x2, y2, tx, ty, 1, r, false);
        }
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);
        if (dxAbs > r || dyAbs > r) {
            return false;
        }
        if (dxAbs > dyAbs) {
            int xTile = x1;
            int y = (y1 << 16) + 0x8000;
            int slope = (dy << 16) / dxAbs;
            int xInc = dx > 0 ? 1 : -1;
            if (dy < 0) {
                y -= 1;
            }
            while (xTile != x2) {
                xTile += xInc;
                int yTile = y >>> 16;
                if (isPillar(xTile, yTile)) {
                    return false;
                }
                y += slope;
                int newYTile = y >>> 16;
                if (newYTile != yTile && isPillar(xTile, newYTile)) {
                    return false;
                }
            }
        } else {
            int yTile = y1;
            int x = (x1 << 16) + 0x8000;
            int slope = (dx << 16) / dyAbs;
            int yInc = dy > 0 ? 1 : -1;
            if (dx < 0) {
                x -= 1;
            }
            while (yTile != y2) {
                yTile += yInc;
                int xTile = x >>> 16;
                if (isPillar(xTile, yTile)) {
                    return false;
                }
                x += slope;
                int newXTile = x >>> 16;
                if (newXTile != xTile && isPillar(newXTile, yTile)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canAttackPlayer(Mob mob) {
        NpcInfo info = getNpcInfo(mob.getType());
        return hasLOS(mob.getX(), mob.getY(), player.getX(), player.getY(), info.getSize(), info.getRange(), true);
    }

    private static final class ProcessResult {
        private final int[] line;
        private final boolean manticoreFired;

        private ProcessResult(int[] line, boolean manticoreFired) {
            this.line = line;
            this.manticoreFired = manticoreFired;
        }
    }

    private void sortMobs() {
        mobs.sort((a, b) -> {
            int aId = getNpcInfo(a.getType()).getNpcId();
            int bId = getNpcInfo(b.getType()).getNpcId();
            return Integer.compare(aId, bId);
        });
    }

    private void removeMob(int index) {
        mobs.remove(index);
        for (int i = 0; i < tape.size(); i++) {
            int[] row = tape.get(i);
            if (index >= 0 && index < row.length) {
                int[] next = new int[row.length - 1];
                int dst = 0;
                for (int src = 0; src < row.length; src++) {
                    if (src == index) {
                        continue;
                    }
                    next[dst++] = row[src];
                }
                tape.set(i, next);
            }
        }
        Map<Integer, Integer> shifted = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : manticoreTicksRemaining.entrySet()) {
            int key = entry.getKey();
            if (key == index) {
                continue;
            }
            shifted.put(key > index ? key - 1 : key, entry.getValue());
        }
        manticoreTicksRemaining.clear();
        manticoreTicksRemaining.putAll(shifted);
    }

    private boolean isPillar(int x, int y) {
        boolean isPillar = false;
        for (int j = 0; j < pillars.length; j++) {
            if (filters[j]) {
                isPillar = doesCollide(x, y, 1, pillars[j][0], pillars[j][1], 3) || isPillar;
            }
        }
        if (y >= 0 && y < BLOCKED_TILE_RANGES.length) {
            int[][] ranges = BLOCKED_TILE_RANGES[y];
            for (int j = 0; j < ranges.length; j++) {
                int[] range = ranges[j];
                if (x >= range[0] && x < range[1]) {
                    return true;
                }
            }
        }
        return isPillar;
    }

    private boolean doesCollide(int x, int y, int s, int x2, int y2, int s2) {
        return !(x > x2 + s2 - 1 || x + s - 1 < x2 || y - s + 1 > y2 || y < y2 - s2 + 1);
    }

    private boolean legalPosition(int x, int y, int size, int index) {
        if (y - (size - 1) < 0 || x + (size - 1) > MAP_WIDTH) {
            return false;
        }
        for (int i = 0; i < pillars.length; i++) {
            if (filters[i] && doesCollide(x, y, size, pillars[i][0], pillars[i][1], 3)) {
                return false;
            }
        }
        for (int yy = y - size + 1; yy <= y; yy++) {
            if (yy < 0 || yy >= BLOCKED_TILE_RANGES.length) {
                return false;
            }
            int[][] ranges = BLOCKED_TILE_RANGES[yy];
            for (int j = 0; j < ranges.length; j++) {
                int[] range = ranges[j];
                if (x + size > range[0] && x < range[1]) {
                    return false;
                }
            }
        }
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            if (mob.getType() < 8) {
                int otherSize = getNpcInfo(mob.getType()).getSize();
                if (i != index && doesCollide(x, y, size, mob.getX(), mob.getY(), otherSize)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void moveMobs(boolean canMove, boolean canGainLos) {
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            if (mob.getType() < 8) {
                mob.setCooldown(mob.getCooldown() - 1);
                int x = mob.getX();
                int y = mob.getY();
                NpcInfo info = getNpcInfo(mob.getType());
                int s = info.getSize();
                int r = info.getRange();

                if (canMove && !(canGainLos && hasLOS(x, y, player.getX(), player.getY(), s, r, true))) {
                    int dx = x + Integer.signum(player.getX() - x);
                    int dy = y + Integer.signum(player.getY() - y);
                    if (doesCollide(dx, dy, s, player.getX(), player.getY(), 1)) {
                        dy = mob.getY();
                    }
                    if (legalPosition(dx, dy, s, i) && (s > 1 || (legalPosition(dx, y, s, i) && legalPosition(x, dy, s, i)))) {
                        mob.setX(dx);
                        mob.setY(dy);
                    } else if (legalPosition(dx, y, s, i)) {
                        mob.setX(dx);
                    } else if (legalPosition(x, dy, s, i)) {
                        mob.setY(dy);
                    }
                }
            }
        }
    }

    private void handleManticoreCharging(boolean canAttack) {
        List<Integer> manticoresStartingToCharge = new ArrayList<Integer>();
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            if (mob.getType() == MANTICORE) {
                String currentExtra = mob.getExtra();
                boolean isUncharged = currentExtra != null && currentExtra.startsWith("u");
                if (isUncharged && canAttack && hasLOS(mob.getX(), mob.getY(), player.getX(), player.getY(), getNpcInfo(MANTICORE).getSize(), getNpcInfo(MANTICORE).getRange(), true)) {
                    manticoresStartingToCharge.add(i);
                }
            }
        }
        if (manticoresStartingToCharge.isEmpty()) {
            return;
        }

        String establishedStyle = null;
        for (int i = 0; i < mobs.size(); i++) {
            if (manticoresStartingToCharge.contains(i)) {
                continue;
            }
            Mob mob = mobs.get(i);
            if (mob.getType() == MANTICORE) {
                String currentExtra = mob.getExtra();
                boolean isChargedOrCharging = currentExtra != null && !currentExtra.startsWith("u");
                if (isChargedOrCharging) {
                    establishedStyle = currentExtra;
                    break;
                }
            }
        }

        List<String> knownStyles = new ArrayList<>();
        if (establishedStyle == null) {
            for (Integer idx : manticoresStartingToCharge) {
                String originalExtra = mobs.get(idx).getOriginalExtra();
                if (originalExtra != null && !"u".equals(originalExtra)) {
                    String baseStyle = originalExtra.startsWith("u") ? originalExtra.substring(1) : originalExtra;
                    if (!knownStyles.contains(baseStyle)) {
                        knownStyles.add(baseStyle);
                    }
                }
            }
        }

        String groupSelectedStyle = null;
        if (knownStyles.size() > 1) {
            groupSelectedStyle = knownStyles.get(random.nextInt(knownStyles.size()));
        }
        String randomStyleForUnknowns = null;

        for (Integer idx : manticoresStartingToCharge) {
            Mob mob = mobs.get(idx);
            String originalExtra = mob.getOriginalExtra();
            String currentExtra = mob.getExtra();
            String chargedStyle = null;

            if (establishedStyle != null) {
                chargedStyle = establishedStyle;
            } else if (groupSelectedStyle != null) {
                chargedStyle = groupSelectedStyle;
            } else if (currentExtra != null && currentExtra.startsWith("u") && currentExtra.length() > 1) {
                chargedStyle = currentExtra.substring(1);
            } else if ("u".equals(currentExtra)) {
                if (knownStyles.size() == 1) {
                    chargedStyle = knownStyles.get(0);
                } else if (knownStyles.size() > 1) {
                    chargedStyle = groupSelectedStyle;
                } else {
                    if (randomStyleForUnknowns == null) {
                        String[] patterns = mantimayhem3 ? MM3_PATTERNS : STANDARD_PATTERNS;
                        randomStyleForUnknowns = patterns[random.nextInt(patterns.length)];
                    }
                    chargedStyle = randomStyleForUnknowns;
                }
            }

            if (chargedStyle != null) {
                mob.setExtra(chargedStyle);
                mob.setCooldown(MANTICORE_CHARGE_TIME);
            }

            if ("u".equals(originalExtra) && chargedStyle != null && establishedStyle == null && knownStyles.isEmpty()) {
                mob.setOriginalExtra("u" + chargedStyle);
            }
        }
    }

    private ProcessResult processAttacks(boolean canAttack) {
        int[] line = new int[mobs.size()];
        boolean manticoreFiredThisTick = false;

        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            if (mob.getType() < 8) {
                int x = mob.getX();
                int y = mob.getY();
                int t = mob.getType();
                NpcInfo info = getNpcInfo(t);
                int attacked = 0;

                if (canAttack && hasLOS(x, y, player.getX(), player.getY(), info.getSize(), info.getRange(), true)) {
                    if (t == MANTICORE) {
                        String currentExtra = mob.getExtra();
                        boolean isCharged = currentExtra != null && !currentExtra.startsWith("u");
                        if (isCharged && mob.getCooldown() <= 0 && !manticoreFiredThisTick) {
                            manticoreTicksRemaining.put(i, 3);
                            attacked = 1;
                            mob.setCooldown(info.getCooldown());
                            manticoreFiredThisTick = true;
                        }
                    } else if (mob.getCooldown() <= 0) {
                        attacked = 1;
                        mob.setCooldown(info.getCooldown());
                    }
                }
                line[i] = attacked | ((x & 0xff) << 16) | ((y & 0xff) << 24);
            }
        }

        return new ProcessResult(line, manticoreFiredThisTick);
    }

    private void recordManticoreOrbSequence(int[] line) {
        Iterator<Map.Entry<Integer, Integer>> iterator = manticoreTicksRemaining.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int index = entry.getKey();
            int ticks = entry.getValue();
            if (ticks > 0 && index >= 0 && index < mobs.size()) {
                String manticoreMode = mobs.get(index).getExtra();
                int[] manticoreStyles = MANTICORE_PATTERNS.get(manticoreMode);
                if (manticoreStyles == null) {
                    iterator.remove();
                    continue;
                }
                int currentStyle = manticoreStyles[3 - ticks];
                int prevLine = line[index];
                line[index] = 1 | (currentStyle << 8) | (prevLine & 0xffff0000);
                entry.setValue(ticks - 1);
            } else {
                iterator.remove();
            }
        }
    }

    private void delayAllReadyMantis() {
        for (Mob mob : mobs) {
            if (mob.getType() != MANTICORE || mob.getCooldown() > 0) {
                continue;
            }
            String currentExtra = mob.getExtra();
            if (currentExtra != null && !currentExtra.startsWith("u")) {
                mob.setCooldown(MANTICORE_DELAY);
            }
        }
    }
}

