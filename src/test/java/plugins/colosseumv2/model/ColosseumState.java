package plugins.colosseumv2.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.runelite.api.Client;
import plugins.colosseum.model.Modifier;
import plugins.colosseum.model.spawns.WaveSpawns;

import java.util.List;

@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class ColosseumState {

    @Getter
    private final boolean inLobby;

    @Getter
    private final boolean inColosseum;

    @Getter
    private final int waveNumber;

    @Getter
    private final boolean waveStarted;

    @Getter
    private final int waveStartTick;

    @Getter
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final List<Modifier> modifiers;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private WaveSpawns waveSpawns;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private WaveSpawns nextWaveSpawns;

    public WaveSpawns getWaveSpawns(Client client) {
        if (this.waveSpawns != null) {
            return this.waveSpawns;
        }
        return this.waveSpawns = WaveSpawns.forWave(client, this, false);
    }

    public WaveSpawns getNextWaveSpawns(Client client) {
        if (this.nextWaveSpawns != null) {
            return this.nextWaveSpawns;
        }
        return this.nextWaveSpawns = WaveSpawns.forWave(client, this, true);
    }
}
