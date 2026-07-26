package unit.plugins.colosseumv2;

import plugins.colosseumv2.engine.EngineConfig;
import plugins.colosseumv2.engine.GridCollisionMap;
import plugins.colosseumv2.engine.NpcSnapshot;
import plugins.colosseumv2.engine.TickInput;

/**
 * Shared builders for {@code PrayerEngine} unit tests. Coordinates are plain scene coordinates
 * on an open 64x64 grid unless a test blocks tiles itself.
 */
final class EngineTestSupport {

    static final int SHAMAN_ID = 12811;
    static final int JAVELIN_ID = 12817;
    static final int JAGUAR_ID = 12810;
    static final int MANTICORE_ID = 12818;
    static final int BERSERKER_ID = 12816;

    static final int ANIM_SHAMAN = 10859;
    static final int ANIM_JAVELIN = 10892;
    static final int ANIM_JAGUAR = 10847;
    static final int ANIM_MANTICORE_THROW = 10869;

    private EngineTestSupport() {
    }

    static GridCollisionMap openMap() {
        return new GridCollisionMap(64, 64);
    }

    static TickInput.TickInputBuilder input(int tick, GridCollisionMap map, int playerX, int playerY) {
        return TickInput.builder()
                .tick(tick)
                .playerX(playerX)
                .playerY(playerY)
                .collisionMap(map)
                .inColosseum(true)
                .waveNumber(5)
                .waveStarted(true)
                .waveStartTick(50)
                .config(EngineConfig.builder().build());
    }

    static NpcSnapshot.NpcSnapshotBuilder shaman(int index, int x, int y) {
        return NpcSnapshot.builder()
                .index(index)
                .npcId(SHAMAN_ID)
                .name("Serpent shaman")
                .sceneX(x)
                .sceneY(y)
                .size(1)
                .animation(-1);
    }

    static NpcSnapshot.NpcSnapshotBuilder javelin(int index, int x, int y) {
        return NpcSnapshot.builder()
                .index(index)
                .npcId(JAVELIN_ID)
                .name("Javelin Colossus")
                .sceneX(x)
                .sceneY(y)
                .size(3)
                .animation(-1);
    }

    static NpcSnapshot.NpcSnapshotBuilder jaguar(int index, int x, int y) {
        return NpcSnapshot.builder()
                .index(index)
                .npcId(JAGUAR_ID)
                .name("Jaguar warrior")
                .sceneX(x)
                .sceneY(y)
                .size(2)
                .animation(-1);
    }

    static NpcSnapshot.NpcSnapshotBuilder manticore(int index, int x, int y) {
        return NpcSnapshot.builder()
                .index(index)
                .npcId(MANTICORE_ID)
                .name("Manticore")
                .sceneX(x)
                .sceneY(y)
                .size(3)
                .animation(-1);
    }

    static NpcSnapshot.NpcSnapshotBuilder berserker(int index, int x, int y) {
        return NpcSnapshot.builder()
                .index(index)
                .npcId(BERSERKER_ID)
                .name("Fremennik berserker")
                .sceneX(x)
                .sceneY(y)
                .size(1)
                .animation(-1);
    }
}
