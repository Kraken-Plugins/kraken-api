package com.kraken.api.query.projectile;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.core.Locatable;
import net.runelite.api.Actor;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;

/**
 * A projectile currently in flight: a boss attack, a spell, an arrow.
 *
 * <p>Projectiles are observations, not menu targets — {@link #interact(String)} always returns
 * {@code false}. Their value is spatial and temporal: where they are, where they will land, who they
 * are aimed at, and how long until impact, which is the raw material for dodge and prayer logic.</p>
 */
public class ProjectileEntity extends AbstractEntity<Projectile> implements Locatable {

    /** Client cycles per game tick: a cycle is 20ms, a tick 600ms. */
    private static final int CYCLES_PER_TICK = 30;

    public ProjectileEntity(Context ctx, Projectile raw) {
        super(ctx, raw);
    }

    @Override
    public int getId() {
        Projectile p = raw();
        return p != null ? p.getId() : -1;
    }

    /**
     * Projectiles have no name; this always returns {@code null}, so name-based filters never match
     * them. Filter by {@code withId} instead.
     * @return null, always.
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * Projectiles cannot be interacted with; nothing is ever dispatched.
     * @param action Ignored.
     * @return false, always.
     */
    @Override
    public boolean interact(String action) {
        return false;
    }

    /**
     * The projectile's current position in the world, updated as it travels.
     * @return The world point under the projectile, or {@code null} when it has expired.
     */
    @Override
    public WorldPoint getWorldLocation() {
        Projectile p = raw();
        if (p == null) return null;
        return WorldPoint.fromLocal(ctx.getClient(), (int) p.getX(), (int) p.getY(), p.getFloor());
    }

    /**
     * The tile the projectile was launched from.
     * @return The source world point, or {@code null} when unknown.
     */
    public WorldPoint getSourcePoint() {
        Projectile p = raw();
        return p != null ? p.getSourcePoint() : null;
    }

    /**
     * The tile the projectile will land on. For a projectile tracking an actor this follows the
     * actor; prefer {@link #getTargetActor()} to identify who is being attacked.
     * @return The target world point, or {@code null} when unknown.
     */
    public WorldPoint getTargetPoint() {
        Projectile p = raw();
        return p != null ? p.getTargetPoint() : null;
    }

    /**
     * The actor that launched the projectile.
     * @return The source actor, or {@code null} for point-launched projectiles.
     */
    public Actor getSourceActor() {
        Projectile p = raw();
        return p != null ? p.getSourceActor() : null;
    }

    /**
     * The actor the projectile is homing in on.
     * @return The target actor, or {@code null} for ground-targeted projectiles.
     */
    public Actor getTargetActor() {
        Projectile p = raw();
        return p != null ? p.getTargetActor() : null;
    }

    /**
     * Client cycles (20ms each) until the projectile lands.
     * @return The remaining cycles, or 0 when the projectile has expired.
     */
    public int getRemainingCycles() {
        Projectile p = raw();
        return p != null ? p.getRemainingCycles() : 0;
    }

    /**
     * Game ticks until the projectile lands, rounded up — the number a prayer flick or dodge has to
     * work with.
     * @return The remaining game ticks, 0 when landing within the current tick or already expired.
     */
    public int getRemainingTicks() {
        return (getRemainingCycles() + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK;
    }
}
