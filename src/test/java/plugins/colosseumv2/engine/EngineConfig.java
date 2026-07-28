package plugins.colosseumv2.engine;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable snapshot of the plugin configuration consumed by the {@link PrayerEngine}. Keeps
 * the engine independent of the RuneLite config interface so it can be unit tested.
 */
@Value
@Builder(toBuilder = true)
public class EngineConfig {

    @Builder.Default
    int queueLookaheadTicks = 20;

    @Builder.Default
    boolean cancelQueuedOnLosBreak = true;

    @Builder.Default
    boolean removeQueuedOnNpcDeath = true;

    @Builder.Default
    boolean prayJaguarOnPath = true;

    @Builder.Default
    boolean enableWavePrePray = true;

    /** Ticks after wave start during which the generic wave prayer is held. */
    @Builder.Default
    int prePrayWindowTicks = 12;

    @Builder.Default
    boolean debugLosTiles = false;

    @Builder.Default
    boolean debugPaths = false;

    @Builder.Default
    boolean debugStopPathOnLos = true;

    @Builder.Default
    int debugMaxNpcs = 8;

    @Builder.Default
    int debugPathLength = 12;
}
