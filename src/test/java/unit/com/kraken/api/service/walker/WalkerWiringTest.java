package unit.com.kraken.api.service.walker;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.input.mouse.VirtualMouse;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.shop.ShopService;
import com.kraken.api.service.walker.Walker;
import com.kraken.api.service.walker.transport.TransportExecutor;
import com.kraken.api.service.walker.transport.registry.ShortestPathBridge;
import com.kraken.api.service.walker.transport.registry.TransportRegistry;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Builds the walker and its planner through Guice the way the client does, with only the RuneLite
 * types supplied.
 *
 * <p>The transport bridge and the executor take their collaborators through their constructors,
 * and Guice cannot stand in for a class that is asked for again while its constructor is still
 * waiting on that request. This is the test that would fail if a collaborator ever led back to
 * them.</p>
 */
class WalkerWiringTest {

    @Test
    void theWalkerItsPlannerAndTheTransportBridgeAreBuiltWithoutACircularDependency() {
        Injector injector = clientInjector();

        Walker walker = injector.getInstance(Walker.class);
        GlobalPathfinder pathfinder = injector.getInstance(GlobalPathfinder.class);

        assertNotNull(walker);
        assertNotNull(pathfinder);
        assertNotNull(injector.getInstance(TransportExecutor.class));
        assertSame(injector.getInstance(ShortestPathBridge.class), injector.getInstance(ShortestPathBridge.class),
                "one bridge serves every search");
    }

    @Test
    void theSharedRegistryStartsWithTheBundledTransports() {
        Injector injector = clientInjector();

        TransportRegistry registry = injector.getInstance(TransportRegistry.class);

        assertSame(registry, injector.createChildInjector().getInstance(TransportRegistry.class),
                "a plugin's injector sees the registry of the root injector");
        assertFalse(registry.snapshot().getDefinitions().isEmpty());
    }

    private static Injector clientInjector() {
        Client client = mock(Client.class);
        when(client.isClientThread()).thenReturn(true);

        return Guice.createInjector(binder -> {
            binder.bind(Client.class).toProvider(Providers.of(client));
            binder.bind(ClientThread.class).toProvider(Providers.of(mock(ClientThread.class)));
            binder.bind(EventBus.class).toProvider(Providers.of(mock(EventBus.class)));
            binder.bind(ItemManager.class).toProvider(Providers.of(mock(ItemManager.class)));
            binder.bind(VirtualMouse.class).toProvider(Providers.of(mock(VirtualMouse.class)));
            binder.bind(BankService.class).toProvider(Providers.of(mock(BankService.class)));
            binder.bind(ShopService.class).toProvider(Providers.of(mock(ShopService.class)));
            binder.bind(InteractionManager.class).toProvider(Providers.of(mock(InteractionManager.class)));
        });
    }
}
