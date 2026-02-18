package com.kraken.api.sim.colosim;

import java.util.*;

public class LineOfSight {
    public static class NpcType {
        public static final int PLAYER = 0;
        public static final int SERPENT_SHAMAN = 1;
        public static final int JAVELIN_COLOSSUS = 2;
        public static final int JAGUAR_WARRIOR = 3;
        public static final int MANTICORE = 4;
        public static final int MINOTAUR = 5;
        public static final int SHOCKWAVE_COLOSSUS = 6;
        public static final int REINFORCEMENT_SHAMAN = 7;
    }

    public static class NpcInfo {
        public int id;
        public int size;
        public int range;
        public int cd;
        public String img;
        public String color;

        public NpcInfo(int id, int size, int range, int cd, String img, String color) {
            this.id = id;
            this.size = size;
            this.range = range;
            this.cd = cd;
            this.img = img;
            this.color = color;
        }
    }

    private static final NpcInfo _SHAMAN = new NpcInfo(1, 1, 10, 5, "serpent_shaman.png", "cyan");
    public static final Map<Integer, NpcInfo> NPC_INFO = new HashMap<>();

    static {
        NPC_INFO.put(NpcType.PLAYER, new NpcInfo(-1, 1, 10, 0, "player.png", "red"));
        NPC_INFO.put(NpcType.SERPENT_SHAMAN, _SHAMAN);
        NPC_INFO.put(NpcType.JAVELIN_COLOSSUS, new NpcInfo(2, 3, 15, 5, "javelin_colossus.png", "lime"));
        NPC_INFO.put(NpcType.JAGUAR_WARRIOR, new NpcInfo(4, 2, 1, 5, "jaguar_warrior.png", "orange"));
        NPC_INFO.put(NpcType.MANTICORE, new NpcInfo(0, 3, 15, 10, "manticore.png", "purple"));
        NPC_INFO.put(NpcType.MINOTAUR, new NpcInfo(5, 3, 1, 5, "minotaur.png", "brown"));
        NPC_INFO.put(NpcType.SHOCKWAVE_COLOSSUS, new NpcInfo(3, 3, 15, 5, "shockwave_colossus.png", "blue"));
        NPC_INFO.put(NpcType.REINFORCEMENT_SHAMAN, new NpcInfo(6, 1, 10, 5, "serpent_shaman.png", "cyan"));
    }

    public static final int MANTICORE = NpcType.MANTICORE;
    public static final String[] MANTICORE_ATTACKS = {"lime", "blue", "red"};

    public static final Map<String, int[]> BASE_MANTICORE_PATTERNS = new HashMap<>();
    static {
        BASE_MANTICORE_PATTERNS.put("r", new int[]{0, 1, 2});
        BASE_MANTICORE_PATTERNS.put("m", new int[]{1, 0, 2});
        BASE_MANTICORE_PATTERNS.put("Mrm", new int[]{2, 0, 1});
        BASE_MANTICORE_PATTERNS.put("Mmr", new int[]{2, 1, 0});
        BASE_MANTICORE_PATTERNS.put("rMm", new int[]{0, 2, 1});
        BASE_MANTICORE_PATTERNS.put("mMr", new int[]{1, 2, 0});
    }

    public static final Map<String, int[]> MANTICORE_PATTERNS = new HashMap<>(BASE_MANTICORE_PATTERNS);
    static {
        for (Map.Entry<String, int[]> entry : BASE_MANTICORE_PATTERNS.entrySet()) {
            MANTICORE_PATTERNS.put("u" + entry.getKey(), entry.getValue());
        }
    }

    public static final int MANTICORE_DELAY = 5;
    public static final int MANTICORE_CHARGE_TIME = 10;
    public static final String[] MM3_PATTERNS = {"r", "m", "Mrm", "Mmr", "rMm", "mMr"};
    public static final String[] STANDARD_PATTERNS = {"r", "m"};

    public static final int MINOTAUR_HEAL_RANGE = 7;
    public static final String MINOTAUR_HEAL_COLOR = "purple";
    public static final int DELAY_FIRST_ATTACK_TICKS = 3;

    public static List<int[]> pillars = new ArrayList<>();
    static {
        pillars.add(new int[]{8, 10});
        pillars.add(new int[]{23, 10});
        pillars.add(new int[]{8, 25});
        pillars.add(new int[]{23, 25});
    }
    public static boolean[] filters = {true, true, true, true};

    public static List<Types.Coordinates> spawns = new ArrayList<>();
    static {
        spawns.add(new Types.Coordinates(3, 19));
        spawns.add(new Types.Coordinates(9, 17));
        spawns.add(new Types.Coordinates(3, 14));
        spawns.add(new Types.Coordinates(13, 14));
        spawns.add(new Types.Coordinates(16, 13));
        spawns.add(new Types.Coordinates(19, 14));
        spawns.add(new Types.Coordinates(17, 9));
        spawns.add(new Types.Coordinates(13, 20));
        spawns.add(new Types.Coordinates(19, 20));
        spawns.add(new Types.Coordinates(16, 24));
        spawns.add(new Types.Coordinates(24, 16));
        spawns.add(new Types.Coordinates(28, 14));
        spawns.add(new Types.Coordinates(28, 19));
    }

    public static int mode = 0;
    public static String modeExtra = null;
    public static boolean degen = false;
    public static final int[] b5Tile = {7, 15};
    public static Types.Coordinates cursorLocation = null;
    public static Types.Coordinates selected = new Types.Coordinates(b5Tile[0], b5Tile[1]);
    public static Types.Coordinates stepStartPosition = null;
    public static List<Types.Mob> mobs = new ArrayList<>();
    public static boolean showSpawns = true;
    public static boolean showPlayerLoS = true;
    public static boolean checker = true;
    public static Integer mousedOverNpc = null;

    public static List<List<Integer>> tape = new ArrayList<>();
    public static List<Types.Coordinates> playerTape = new ArrayList<>();
    public static int[] tapeSelectionRange = null;

    public static int tickCount = 0;

    public static List<Types.Coordinates> replay = null;
    public static Integer replayTick = null;

    public static Integer draggingNpcIndex = null;
    public static Types.Coordinates draggingNpcOffset = null;

    public static final int MAX_EXPORT_LENGTH = 128;

    public static Map<Integer, Integer> manticoreTicksRemaining = new HashMap<>();

    public static boolean fromWaveStart = false;
    public static boolean mantimayhem3 = false;
    public static boolean showVenatorBounce = false;

    public static int TILE_SIZE = 20;
    public static final int MAP_WIDTH = 34;
    public static final int MAP_HEIGHT = 34;
    public static final int TICKER_WIDTH = 9;
    public static final int CANVAS_WIDTH = TILE_SIZE * MAP_WIDTH + TICKER_WIDTH * TILE_SIZE;
    public static final int CANVAS_HEIGHT = TILE_SIZE * MAP_HEIGHT;

    public static void setFromWaveStart(boolean val) {
        fromWaveStart = val;
    }

    public static void setMantimayhem3(boolean val) {
        mantimayhem3 = val;
    }

    public static void setShowVenatorBounce(boolean show) {
        showVenatorBounce = show;
    }

    public static boolean isPillar(int x, int y) {
        boolean isPillar = false;
        for (int j = 0; j < pillars.size(); j++) {
            if (filters[j]) {
                isPillar = doesCollide(x, y, 1, pillars.get(j)[0], pillars.get(j)[1], 3) || isPillar;
            }
        }
        if (y >= 0 && y < Constants.blockedTileRanges.size()) {
            List<int[]> ranges = Constants.blockedTileRanges.get(y);
            for (int j = 0; j < ranges.size(); ++j) {
                int[] range = ranges.get(j);
                if (x >= range[0] && x < range[1]) {
                    return true;
                }
            }
        }
        return isPillar;
    }

    public static void removeMob(int index) {
        mobs.remove(index);
        for (List<Integer> entries : tape) {
            entries.remove(index);
        }
    }

    public static boolean hasLOS(int x1, int y1, int x2, int y2, int s, int r, boolean isNPC) {
        if (s == 0) s = 1;
        if (r == 0) r = 1;

        int dx = x2 - x1;
        int dy = y2 - y1;
        if (isPillar(x1, y1) || isPillar(x2, y2) || doesCollide(x1, y1, s, x2, y2, 1)) {
            return false;
        }
        if (r == 1) {
            return (dx < s && dx >= 0 && (dy == 1 || dy == -s)) ||
                    (dy > -s && dy <= 0 && (dx == -1 || dx == s));
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

    public static boolean doesCollide(int x, int y, int s, int x2, int y2, int s2) {
        if (x > x2 + s2 - 1 || x + s - 1 < x2 || y - s + 1 > y2 || y < y2 - s2 + 1) {
            return false;
        }
        return true;
    }

    public static boolean legalPosition(int x, int y, int size, int index) {
        if (y - (size - 1) < 0 || x + (size - 1) > MAP_WIDTH) {
            return false;
        }
        boolean collision = false;
        for (int i = 0; i < pillars.size(); i++) {
            if (filters[i] && doesCollide(x, y, size, pillars.get(i)[0], pillars.get(i)[1], 3)) {
                return false;
            }
        }
        for (int yy = y - size + 1; yy <= y; yy++) {
            List<int[]> ranges = Constants.blockedTileRanges.get(yy);
            for (int j = 0; j < ranges.size(); ++j) {
                int[] range = ranges.get(j);
                if (x + size > range[0] && x < range[1]) {
                    return false;
                }
            }
        }
        for (int i = 0; i < mobs.size(); i++) {
            if (mobs.get(i).type < 8) {
                if (i != index && doesCollide(x, y, size, mobs.get(i).x, mobs.get(i).y, NPC_INFO.get(mobs.get(i).type).size)) {
                    return false;
                }
            }
        }
        return !collision;
    }

    public static void sortMobs() {
        mobs.sort(Comparator.comparingInt(a -> NPC_INFO.get(a.type).id));
    }

    public static void place() {
        if (cursorLocation != null) {
            if (mode > 0) {
                for (int i = 0; i < mobs.size(); i++) {
                    if (mobs.get(i).spawnX == cursorLocation.x && mobs.get(i).spawnY == cursorLocation.y) {
                        return;
                    }
                }
                Types.Mob newMob = new Types.Mob(
                        cursorLocation.x,
                        cursorLocation.y,
                        mode,
                        cursorLocation.x,
                        cursorLocation.y,
                        0,
                        modeExtra,
                        null
                );

                if (mode == MANTICORE && modeExtra != null) {
                    newMob.originalExtra = modeExtra;
                }

                mobs.add(newMob);
                sortMobs();
                mode = 0;
                modeExtra = null;
            } else {
                selected = new Types.Coordinates(cursorLocation.x, cursorLocation.y);
            }
            cursorLocation = null;
        }
    }

    public static void advanceReplay() {
        if (replay != null && replayTick != null) {
            if (replayTick < replay.size()) {
                selected = replay.get(replayTick);
            } else {
                reset();
            }
            replayTick++;
        }
    }

    public static void moveMobs(boolean canMove, boolean canGainLos) {
        for (int i = 0; i < mobs.size(); i++) {
            if (mobs.get(i).type < 8) {
                Types.Mob mob = mobs.get(i);
                mob.cooldown--;
                int x = mob.x;
                int y = mob.y;
                int t = mob.type;
                int s = NPC_INFO.get(t).size;
                int r = NPC_INFO.get(t).range;

                if (canMove && !(canGainLos && hasLOS(x, y, selected.x, selected.y, s, r, true))) {
                    int dx = x + Integer.signum(selected.x - x);
                    int dy = y + Integer.signum(selected.y - y);
                    if (doesCollide(dx, dy, s, selected.x, selected.y, 1)) {
                        dy = mob.y;
                    }
                    if (legalPosition(dx, dy, s, i) && (s > 1 || (legalPosition(dx, y, s, i) && legalPosition(x, dy, s, i)))) {
                        mob.x = dx;
                        mob.y = dy;
                    } else if (legalPosition(dx, y, s, i)) {
                        mob.x = dx;
                    } else if (legalPosition(x, dy, s, i)) {
                        mob.y = dy;
                    }
                }
            }
        }
    }

    public static void handleManticoreCharging(boolean canAttack) {
        List<Integer> manticoresStartingToCharge = new ArrayList<>();
        for (int i = 0; i < mobs.size(); i++) {
            if (mobs.get(i).type == MANTICORE) {
                Types.Mob mob = mobs.get(i);
                String currentExtra = mob.extra;
                int x = mob.x;
                int y = mob.y;

                boolean isUncharged = currentExtra != null && currentExtra.startsWith("u");

                if (isUncharged && canAttack && hasLOS(x, y, selected.x, selected.y, NPC_INFO.get(MANTICORE).size, NPC_INFO.get(MANTICORE).range, true)) {
                    manticoresStartingToCharge.add(i);
                }
            }
        }

        if (manticoresStartingToCharge.isEmpty()) {
            return;
        }

        String establishedStyle = null;
        for (int i = 0; i < mobs.size(); i++) {
            if (mobs.get(i).type == MANTICORE && !manticoresStartingToCharge.contains(i)) {
                Types.Mob mob = mobs.get(i);
                String currentExtra = mob.extra;

                boolean isChargedOrCharging = currentExtra != null && !currentExtra.startsWith("u");

                if (isChargedOrCharging) {
                    establishedStyle = currentExtra;
                    break;
                }
            }
        }

        List<String> knownStyles = new ArrayList<>();
        if (establishedStyle == null) {
            for (int idx : manticoresStartingToCharge) {
                String originalExtra = mobs.get(idx).originalExtra;
                if (originalExtra != null && !originalExtra.equals("u")) {
                    String baseStyle = originalExtra.startsWith("u") ? originalExtra.substring(1) : originalExtra;
                    if (!knownStyles.contains(baseStyle)) {
                        knownStyles.add(baseStyle);
                    }
                }
            }
        }

        String groupSelectedStyle = null;
        if (knownStyles.size() > 1) {
            groupSelectedStyle = knownStyles.get(new Random().nextInt(knownStyles.size()));
        }

        String randomStyleForUnknowns = null;

        for (int idx : manticoresStartingToCharge) {
            Types.Mob mob = mobs.get(idx);
            String originalExtra = mob.originalExtra;
            String currentExtra = mob.extra;

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
                        randomStyleForUnknowns = patterns[new Random().nextInt(patterns.length)];
                    }
                    chargedStyle = randomStyleForUnknowns;
                }
            }

            if (chargedStyle != null) {
                mob.extra = chargedStyle;
                mob.cooldown = MANTICORE_CHARGE_TIME;
            }

            if ("u".equals(originalExtra) && chargedStyle != null && establishedStyle == null && knownStyles.isEmpty()) {
                mob.originalExtra = "u" + chargedStyle;
            }
        }
    }

    public static class ProcessAttacksResult {
        public List<Integer> line;
        public boolean manticoreFired;

        public ProcessAttacksResult(List<Integer> line, boolean manticoreFired) {
            this.line = line;
            this.manticoreFired = manticoreFired;
        }
    }

    public static ProcessAttacksResult processAttacks(boolean canAttack) {
        List<Integer> line = new ArrayList<>();
        boolean manticoreFiredThisTick = false;

        for (int i = 0; i < mobs.size(); i++) {
            if (mobs.get(i).type < 8) {
                Types.Mob mob = mobs.get(i);
                int x = mob.x;
                int y = mob.y;
                int t = mob.type;
                int s = NPC_INFO.get(t).size;
                int r = NPC_INFO.get(t).range;
                int attacked = 0;

                if (canAttack && hasLOS(x, y, selected.x, selected.y, s, r, true)) {
                    if (mob.type == MANTICORE) {
                        String currentExtra = mob.extra;
                        boolean isCharged = currentExtra != null && !currentExtra.startsWith("u");

                        if (isCharged && mob.cooldown <= 0 && !manticoreFiredThisTick) {
                            manticoreTicksRemaining.put(i, 3);
                            attacked = 1;
                            mob.cooldown = NPC_INFO.get(t).cd;
                            manticoreFiredThisTick = true;
                        }
                    } else {
                        if (mob.cooldown <= 0) {
                            attacked = 1;
                            mob.cooldown = NPC_INFO.get(t).cd;
                        }
                    }
                }
                int value = attacked | ((x & 0xff) << 16) | ((y & 0xff) << 24);
                line.add(value);
            }
        }

        return new ProcessAttacksResult(line, manticoreFiredThisTick);
    }

    public static void recordManticoreOrbSequence(List<Integer> line) {
        Iterator<Map.Entry<Integer, Integer>> iterator = manticoreTicksRemaining.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int index = entry.getKey();
            int ticks = entry.getValue();

            if (ticks > 0 && index < mobs.size()) {
                String manticoreMode = mobs.get(index).extra;
                int[] manticoreStyles = MANTICORE_PATTERNS.get(manticoreMode);
                int currentStyle = manticoreStyles[3 - ticks];
                int prevLine = line.get(index);
                line.set(index, 1 | (currentStyle << 8) | (prevLine & 0xffff0000));
                manticoreTicksRemaining.put(index, ticks - 1);
            } else {
                iterator.remove();
            }
        }
    }

    public static void step(boolean draw) {
        if (tickCount == 0 && replay == null) {
            stepStartPosition = new Types.Coordinates(selected.x, selected.y);
        }

        advanceReplay();

        if (mode == 0 && !mobs.isEmpty()) {
            boolean canAttack = !fromWaveStart || tickCount >= DELAY_FIRST_ATTACK_TICKS;
            boolean canMove = !fromWaveStart || tickCount > 0;
            boolean canGainLos = !fromWaveStart || tickCount > 1;

            moveMobs(canMove, canGainLos);
            handleManticoreCharging(canAttack);
            ProcessAttacksResult result = processAttacks(canAttack);
            recordManticoreOrbSequence(result.line);

            if (result.manticoreFired) {
                delayAllReadyMantis();
            }

            playerTape.add(new Types.Coordinates(selected.x, selected.y));
            tape.add(result.line);
        }
        tickCount++;
        if (draw) {
            drawWave();
        }
    }

    public static void delayAllReadyMantis() {
        for (Types.Mob mob : mobs) {
            if (mob.type == MANTICORE && mob.cooldown <= 0) {
                String currentExtra = mob.extra;
                if (currentExtra != null && !currentExtra.startsWith("u")) {
                    mob.cooldown = MANTICORE_DELAY;
                }
            }
        }
    }

    public static void stopReplay() {
        replay = null;
        replayTick = null;
    }

    public static void remove() {
        mobs.clear();
        stopReplay();
        selected = new Types.Coordinates(b5Tile[0], b5Tile[1]);
        stepStartPosition = null;
        reset();
        drawWave();
    }

    public static void reset() {
        for (Types.Mob mob : mobs) {
            mob.x = mob.spawnX;
            mob.y = mob.spawnY;
            mob.cooldown = 0;

            if (mob.type == MANTICORE) {
                String originalExtra = mob.originalExtra;
                if (originalExtra != null) {
                    mob.extra = originalExtra;
                }
            }
        }
        manticoreTicksRemaining.clear();
        tape.clear();
        playerTape.clear();
        tapeSelectionRange = null;
        tickCount = 0;
        if (replay != null && !replay.isEmpty()) {
            replayTick = 0;
            selected = replay.get(0);
        } else if (stepStartPosition != null) {
            selected = new Types.Coordinates(stepStartPosition.x, stepStartPosition.y);
        }
        draggingNpcIndex = null;
        draggingNpcOffset = null;
        cursorLocation = null;
        drawWave();
    }

    public static void setMode(int m, String extra, boolean initPosition) {
        if (initPosition && cursorLocation == null) {
            cursorLocation = new Types.Coordinates(selected.x, selected.y);
        }
        mode = m;
        modeExtra = extra;
        drawWave();
    }

    public static void _setSelected(Types.Coordinates s, int _mode, String _extra) {
        selected = s;
        cursorLocation = s;
        mode = _mode;
        modeExtra = _extra;
    }

    public static List<Types.Mob> _getMobs() {
        return mobs;
    }

    public static void drawWave() {
        // Stub for drawing
    }

    public static String getReplayURL(List<Types.MobSpec> mobSpecs, List<Types.Coordinates> playerTicks, boolean fromWaveStart) {
        StringBuilder url = new StringBuilder(getSpawnUrl(mobSpecs));
        url.append("#");
        List<Integer> playerLocations = new ArrayList<>();
        for (Types.Coordinates coords : playerTicks) {
            playerLocations.add(encodeCoordinate(coords));
        }

        // run-length encoding
        if (playerLocations.isEmpty()) {
            return url.toString();
        }
        int last = playerLocations.get(0);
        int runLength = 1;
        for (int i = 1; i < playerLocations.size(); i++) {
            if (playerLocations.get(i) != last) {
                url.append(last);
                if (runLength > 1) {
                    url.append("x").append(runLength);
                }
                url.append(".");
                runLength = 1;
            } else {
                runLength++;
            }
            last = playerLocations.get(i);
        }
        url.append(last);
        if (runLength > 1) {
            url.append("x").append(runLength);
        }
        if (fromWaveStart) {
            url.append("_ws");
        }
        if (mantimayhem3) {
            url.append("_mm3");
        }
        return url.toString();
    }

    public static String getSpawnUrl(List<Types.MobSpec> mobSpecs) {
        StringBuilder url = new StringBuilder("http://localhost/?"); // Placeholder base URL
        for (Types.MobSpec spec : mobSpecs) {
            url.append(String.format("%02d", spec.spawnX));
            url.append(String.format("%02d", spec.spawnY));
            url.append(spec.type);
            if (spec.type == MANTICORE && spec.extra != null) {
                url.append(spec.extra);
            }
            url.append(".");
        }
        if (degen) {
            url.append(".degeN");
        }
        return url.toString();
    }

    public static int encodeCoordinate(Types.Coordinates coords) {
        return (coords.x & 0xff) | ((coords.y & 0xff) << 8);
    }

    public static Types.Coordinates decodeCoordinates(int coords) {
        return new Types.Coordinates(coords & 0xff, (coords >> 8) & 0xff);
    }
}
