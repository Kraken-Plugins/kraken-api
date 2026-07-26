package plugins.colosseumv2.engine;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Prayer;

import java.util.List;

/**
 * Everything the {@link PrayerEngine} needs for one game tick, captured from client state by
 * the plugin (or built directly by unit tests).
 */
@Value
@Builder(toBuilder = true)
public class TickInput {

    int tick;

    /** Player scene coordinates. */
    int playerX;
    int playerY;

    CollisionMap collisionMap;

    @Singular
    List<NpcSnapshot> npcs;

    boolean inColosseum;
    int waveNumber;
    boolean waveStarted;
    int waveStartTick;

    /** Generic prayer for the current wave's spawn window, or {@code null}. */
    Prayer prePrayPrayer;

    EngineConfig config;
}
