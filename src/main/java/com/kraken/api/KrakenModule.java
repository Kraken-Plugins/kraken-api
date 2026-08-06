package com.kraken.api;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.kraken.api.core.interaction.InteractionDispatcher;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.script.breakhandler.BreakManager;
import com.kraken.api.input.KeyboardService;
import com.kraken.api.input.mouse.VirtualMouse;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.bank.DepositBoxService;
import com.kraken.api.service.camera.CameraService;
import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.grandexchange.GrandExchangeService;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.prayer.PrayerService;
import com.kraken.api.service.shop.ShopService;
import com.kraken.api.service.tile.TileService;

/**
 * Guice bindings for the Kraken API.
 *
 * <p>Install this in a plugin's module to make the API's singletons explicit rather than
 * just-in-time. Two things follow from that. First, a consumer can override any binding — supplying a
 * stub {@code InteractionManager} in a test, for example — which is not possible for a just-in-time
 * binding. Second, the bindings are declared in one place, so the set of objects the API expects to be
 * singletons is visible rather than implied.</p>
 *
 * <pre>
 * {@literal @}Override
 * public void configure(Binder binder) {
 *     binder.install(new KrakenModule());
 * }
 * </pre>
 *
 * <p>Installing this is optional; the API works without it because Guice will create these types
 * on demand. Note that RuneLite gives each plugin its own child injector, so with or without this
 * module a singleton is one instance <em>per plugin</em>, not one per client. Anything that must be
 * shared client-wide has to be bound in the parent injector.</p>
 */
public class KrakenModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Context.class).in(Scopes.SINGLETON);

        bind(InteractionManager.class).in(Scopes.SINGLETON);
        bind(InteractionDispatcher.class).in(Scopes.SINGLETON);
        bind(PacketClient.class).in(Scopes.SINGLETON);

        bind(VirtualMouse.class).in(Scopes.SINGLETON);
        bind(KeyboardService.class).in(Scopes.SINGLETON);

        bind(BankService.class).in(Scopes.SINGLETON);
        bind(DepositBoxService.class).in(Scopes.SINGLETON);
        bind(ShopService.class).in(Scopes.SINGLETON);
        bind(GrandExchangeService.class).in(Scopes.SINGLETON);
        bind(DialogueService.class).in(Scopes.SINGLETON);
        bind(PrayerService.class).in(Scopes.SINGLETON);
        bind(MagicService.class).in(Scopes.SINGLETON);
        bind(MovementService.class).in(Scopes.SINGLETON);
        bind(TileService.class).in(Scopes.SINGLETON);
        bind(CameraService.class).in(Scopes.SINGLETON);
        bind(BreakManager.class).in(Scopes.SINGLETON);
    }
}
