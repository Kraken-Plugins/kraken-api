package com.kraken.api.simulation.data;

import java.util.Map;

/**
 * Tick pipeline toggles from a pack's pipeline.json. Every uncertain sub order or contested mechanic is a named flag
 * here so the alternative encoding can be flipped without code changes.
 */
public final class PipelineFlags {
    /** A new click replaces the current interaction; attack and move are exclusive per tick. */
    public boolean allowSameTickMoveAttack;
    /** Ground hazard contact damage applies after the player's movement step. */
    public boolean hazardsAfterMove = true;
    /**
     * An NPC whose footprint covers the player walks one cardinal step per tick until clear (random in game;
     * modeled with a deterministic tick and slot hash).
     */
    public boolean underfootEscape = true;

    /**
     * Spawn relative action gates (ticks after an NPC's spawn visible tick): first step, first hold on sight, first
     * attack. Per type first step overrides live in npcs.json.
     */
    public int gateMoveTick = 2;
    /** Ticks after the spawn visible tick before an NPC may hold on sight of the player. */
    public int gateLosTick = 3;
    /** Ticks after the spawn visible tick before an NPC may attack. */
    public int gateAttackTick = 3;

    /**
     * Ticks between an NPC attack's real damage and prayer resolution and its visible attack animation; live
     * cadence stamps shift by it.
     */
    public int attackAnimationLagTicks;

    /** Share of an unprayed hit's max taken as its expected damage, x1000 (the expected value channel). */
    public int unprayedExpectedFactor1000 = 300;

    /** Provenance notes keyed by flag name. */
    public Map<String, String> src;
}
