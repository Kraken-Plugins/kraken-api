package unit.com.kraken.api.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.input.mouse.VirtualMouse;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.shop.ShopService;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the scope contract: the root injector owns the one Context, and every plugin's child injector
 * resolves that same instance without any explicit binding.
 */
class ContextScopeTest {

    @Test
    void pluginChildInjectorsShareTheRootContext() {
        Client client = mock(Client.class);
        when(client.isClientThread()).thenReturn(true);
        ClientThread clientThread = mock(ClientThread.class);
        EventBus eventBus = mock(EventBus.class);
        ItemManager itemManager = mock(ItemManager.class);
        VirtualMouse mouse = mock(VirtualMouse.class);
        BankService bankService = mock(BankService.class);
        ShopService shopService = mock(ShopService.class);
        InteractionManager interactionManager = mock(InteractionManager.class);

        Injector root = Guice.createInjector(binder -> {
            binder.bind(Client.class).toProvider(Providers.of(client));
            binder.bind(ClientThread.class).toProvider(Providers.of(clientThread));
            binder.bind(EventBus.class).toProvider(Providers.of(eventBus));
            binder.bind(ItemManager.class).toProvider(Providers.of(itemManager));
            binder.bind(VirtualMouse.class).toProvider(Providers.of(mouse));
            binder.bind(BankService.class).toProvider(Providers.of(bankService));
            binder.bind(ShopService.class).toProvider(Providers.of(shopService));
            binder.bind(InteractionManager.class).toProvider(Providers.of(interactionManager));
        });
        Injector pluginA = root.createChildInjector();
        Injector pluginB = root.createChildInjector();

        Context shared = pluginA.getInstance(Context.class);
        assertSame(shared, pluginB.getInstance(Context.class), "two plugins must see one Context");
        assertSame(shared, root.getInstance(Context.class), "the root injector owns the Context");
    }
}
