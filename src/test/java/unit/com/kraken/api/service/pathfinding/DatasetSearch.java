package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.walker.transport.PlayerStateReader;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The real search over everything that ships with the API, for a made up player on a members
 * world: the collision map, the transport dataset and the hand written rows are the bundled ones.
 * Only the client and the player's state are stand-ins.
 */
final class DatasetSearch {

    /** The registry the search plans with, holding the bundled dataset and hand written rows. */
    @Getter
    private final TransportRegistry registry = new TransportRegistry();

    @Getter
    private final GlobalPathfinder pathfinder = new GlobalPathfinder();

    private final PlayerStateReader playerStateReader = mock(PlayerStateReader.class);

    /**
     * Wires the search to the bundled data and starts it off with a new account.
     *
     * @throws Exception when the pathfinder's fields cannot be set
     */
    DatasetSearch() throws Exception {
        Context ctx = mock(Context.class);
        Client client = mock(Client.class);
        inject("ctx", ctx);
        inject("client", client);
        inject("transportPlanner",
                new TransportPlanner(registry, playerStateReader, mock(MagicService.class), client));
        when(ctx.runOnClientThread(any(Callable.class)))
                .thenAnswer(call -> ((Callable<?>) call.getArgument(0)).call());
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
        playing(newAccount().build());
    }

    /**
     * Sets the player every later search is planned for.
     *
     * @param state the levels, quests, items and variables the player has
     */
    void playing(TransportRequirements.PlayerState state) {
        when(playerStateReader.read(any(PlayerStateReader.Needs.class))).thenReturn(state);
    }

    /**
     * Starts the state of an account that has every skill at level 1 and carries nothing.
     *
     * @return a builder to add items, quests and variables to
     */
    static TransportRequirements.PlayerState.PlayerStateBuilder newAccount() {
        int[] levels = new int[Skill.values().length];
        Arrays.fill(levels, 1);
        return TransportRequirements.PlayerState.builder().skillLevels(levels).combatLevel(3).totalLevel(32);
    }

    private void inject(String name, Object value) throws Exception {
        Field field = GlobalPathfinder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(pathfinder, value);
    }
}
