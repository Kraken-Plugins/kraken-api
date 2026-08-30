package com.kraken.api.query.projectile;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractSpatialQuery;
import net.runelite.api.Actor;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A fluent query over the projectiles currently in flight.
 *
 * <p>Spatial filters measure against the projectile's current position as it travels; use
 * {@link #landingWithin(int)} to reason about where it will land instead. Usage:</p>
 *
 * <pre>{@code
 * ctx.projectiles().withId(BOSS_MAGIC_ATTACK).targetingMe().first();
 * ctx.projectiles().landingWithin(1).isPresent();  // AOE about to hit my tile?
 * }</pre>
 */
public class ProjectileQuery extends AbstractSpatialQuery<ProjectileEntity, ProjectileQuery, Projectile> {

    public ProjectileQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<ProjectileEntity>> source() {
        // Expired projectiles linger in the client's deque until cleanup; a projectile with no
        // remaining cycles has already landed and is excluded here.
        return () -> StreamSupport.stream(ctx.getClient().getProjectiles().spliterator(), false)
                .filter(Objects::nonNull)
                .filter(p -> p.getRemainingCycles() > 0)
                .map(p -> new ProjectileEntity(ctx, p));
    }

    /**
     * Filters for projectiles homing in on a specific actor.
     * @param actor The actor being targeted.
     * @return ProjectileQuery projectiles targeting the actor.
     */
    public ProjectileQuery targeting(Actor actor) {
        return filter(p -> p.getTargetActor() != null && p.getTargetActor() == actor);
    }

    /**
     * Filters for projectiles homing in on the local player.
     * @return ProjectileQuery projectiles targeting the local player.
     */
    public ProjectileQuery targetingMe() {
        return filter(p -> {
            Actor target = p.getTargetActor();
            return target != null && target == ctx.getClient().getLocalPlayer();
        });
    }

    /**
     * Filters for projectiles launched by a specific actor.
     * @param actor The actor that fired the projectile.
     * @return ProjectileQuery projectiles fired by the actor.
     */
    public ProjectileQuery firedBy(Actor actor) {
        return filter(p -> p.getSourceActor() != null && p.getSourceActor() == actor);
    }

    /**
     * Filters for projectiles whose landing tile is within the given distance of the local player —
     * the "is an AOE about to land on or near me" question. Yields nothing when there is no local
     * player.
     * @param distance The maximum distance in tiles from the local player, inclusive.
     * @return ProjectileQuery projectiles landing near the local player.
     */
    public ProjectileQuery landingWithin(int distance) {
        WorldPoint anchor = localPlayerLocation();
        if (anchor == null) {
            return empty();
        }
        return filter(p -> {
            WorldPoint target = p.getTargetPoint();
            return target != null && target.distanceTo(anchor) <= distance;
        });
    }

    /**
     * Filters for projectiles landing within the given number of game ticks, for timing a dodge or a
     * prayer switch.
     * @param ticks The maximum remaining flight time in game ticks, inclusive.
     * @return ProjectileQuery projectiles about to land.
     */
    public ProjectileQuery landingWithinTicks(int ticks) {
        return filter(p -> p.getRemainingTicks() <= ticks);
    }
}
