package plugins.colosseumv2.engine;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Per-NPC diagnostics computed once per tick for the scene debug overlays.
 */
@Value
@Builder
public class NpcDebugInfo {

    int npcIndex;
    String name;
    int sceneX;
    int sceneY;
    int size;

    boolean lineOfSight;
    boolean stuck;

    /** Ticks until this NPC may attack again, or -1 when unknown. */
    int readyIn;

    /** Ticks since the last observed attack, or -1 when never observed. */
    int lastAttackAgo;

    /** Next predicted attack tick, or -1. */
    int nextAttackTick;

    /** Predicted first attack tick from pathing, or -1. */
    int pathAttackTick;

    /** Manticore phase description, or {@code null} for non-manticores. */
    String manticorePhase;

    /** Predicted path (scene coords), possibly empty. */
    List<int[]> path;

    /** Tiles from which this NPC could attack the player's position. */
    List<int[]> losTiles;
}
