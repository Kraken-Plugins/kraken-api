package unit.com.kraken.api.query;

import com.kraken.api.Context;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds a mocked {@link Context} whose client-thread helpers execute inline, so query classes can be
 * exercised without a live client.
 */
final class QueryTestSupport {

    private QueryTestSupport() {
    }

    /**
     * A Context whose runOnClientThread variants run the callable on the calling thread, backed by a
     * mocked client whose local player stands at the given location.
     *
     * @param playerLocation The local player's world location; null simulates no local player.
     * @return The mocked context.
     */
    @SuppressWarnings("unchecked")
    static Context contextWithPlayerAt(WorldPoint playerLocation) {
        Context ctx = mock(Context.class);
        Client client = mock(Client.class);
        when(ctx.getClient()).thenReturn(client);

        if (playerLocation != null) {
            Player player = mock(Player.class);
            when(player.getWorldLocation()).thenReturn(playerLocation);
            when(client.getLocalPlayer()).thenReturn(player);
        }

        when(ctx.runOnClientThread(any(Callable.class)))
                .thenAnswer(invocation -> ((Callable<Object>) invocation.getArgument(0)).call());
        when(ctx.runOnClientThread(any(Callable.class), any()))
                .thenAnswer(invocation -> {
                    try {
                        return ((Callable<Object>) invocation.getArgument(0)).call();
                    } catch (Exception e) {
                        return invocation.getArgument(1);
                    }
                });
        return ctx;
    }
}
