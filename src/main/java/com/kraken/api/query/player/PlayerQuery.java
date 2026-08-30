package com.kraken.api.query.player;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractSpatialQuery;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PlayerQuery extends AbstractSpatialQuery<PlayerEntity, PlayerQuery, Player> {

    public PlayerQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<PlayerEntity>> source() {
        return () -> {
            List<PlayerEntity> players = ctx.getClient().getTopLevelWorldView().players().stream()
                    .filter(Objects::nonNull)
                    // Do not include the local player by default
                    .filter(p -> ctx.runOnClientThread(() -> p.getName() != null && !p.getName().equalsIgnoreCase(ctx.getClient().getLocalPlayer().getName())))
                    .map(player -> new PlayerEntity(ctx, player))
                    .collect(Collectors.toList());
            return players.stream();
        };
    }

    /**
     * Filters the stream for players interacting with a specified actor
     * @param actor The actor to check for interacting with
     * @return PlayerQuery
     */
    public PlayerQuery interactingWith(Actor actor) {
        return filter(p -> p.raw().isInteracting() && p.raw().getInteracting() == actor);
    }

    /**
     * Filters the stream for players within a specified combat level (inclusive).
     * @param low The minimum combat level bound
     * @param high The maximum combat level bound
     * @return PlayerQuery
     */
    public PlayerQuery withinLevel(int low, int high) {
        return filter(p -> p.raw().getCombatLevel() >= low && p.raw().getCombatLevel() <= high);
    }

    /**
     * Filters the stream for players who can attack you within the wilderness. If this is called outside
     * the wilderness, the stream will contain no players, be warned.
     * @return PlayerQuery
     */
    public PlayerQuery withinAttackableWildernessLevel() {
        WildernessInfo wildernessInfo = ctx.players().local().getWildernessInfo();
        if(wildernessInfo == null || wildernessInfo.getLevel() == 0) {
            return empty();
        }

        return filter(p -> p.raw().getCombatLevel() >= wildernessInfo.getMinAttackableCombatLevel() && p.raw().getCombatLevel() <= wildernessInfo.getMaxAttackableCombatLevel());
    }

    /**
     * Finds players with a combat level strictly greater than the given argument
     * @param level Combat level
     * @return PlayerQuery
     */
    public PlayerQuery combatLevelGreaterThan(int level) {
        return filter(player -> player.raw().getCombatLevel() > level);
    }

    /**
     * Directly retrieves the local player wrapper.
     * Does not use the filter list.
     * @return PlayerEntity the {@code LocalPlayerEntity} object.
     */
    public LocalPlayerEntity local() {
        return ctx.getLocalPlayer();
    }
}
