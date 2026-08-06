package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.Provider;
import com.kraken.api.core.ClientThreadException;
import com.kraken.api.core.Services;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.input.mouse.VirtualMouse;
import com.kraken.api.query.container.bank.BankInventoryQuery;
import com.kraken.api.query.container.bank.BankQuery;
import com.kraken.api.query.container.bank.DepositBoxQuery;
import com.kraken.api.query.container.inventory.InventoryQuery;
import com.kraken.api.query.container.shop.ShopInventoryQuery;
import com.kraken.api.query.container.shop.ShopQuery;
import com.kraken.api.query.equipment.EquipmentQuery;
import com.kraken.api.query.gameobject.GameObjectQuery;
import com.kraken.api.query.groundobject.GroundObjectQuery;
import com.kraken.api.query.npc.NpcQuery;
import com.kraken.api.query.player.LocalPlayerEntity;
import com.kraken.api.query.player.PlayerQuery;
import com.kraken.api.query.widget.WidgetQuery;
import com.kraken.api.query.world.WorldQuery;
import com.kraken.api.service.camera.CameraService;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.shop.ShopService;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Singleton
public class Context {

    /**
     * How long a caller will wait for the client thread before giving up.
     *
     * <p>The client thread services this queue once per frame, so anything approaching this budget
     * means it is blocked or the client is loading — not that the work is slow.</p>
     */
    public static final long CLIENT_THREAD_TIMEOUT_MS = 2_000L;

    @Setter
    @Getter
    private VirtualMouse mouse;

    @Getter
    private final Client client;

    @Getter
    private final ClientThread clientThread;

    @Getter
    private final LocalPlayerEntity localPlayer;

    @Getter
    private final ItemManager itemManager;

    @Getter
    private final InteractionManager interactionManager;

    private final EventBus eventBus;
    private final BankService bankService;
    private final ShopService shopService;

    /**
     * Held as a Provider so that resolving the camera service does not pull it into Context's own
     * construction, and so that shutdown releases the instance from this Context's injector rather
     * than whatever the root injector holds.
     */
    private final Provider<CameraService> cameraServiceProvider;

    private volatile boolean shutdown = false;

    @Inject
    public Context(final Client client, final ClientThread clientThread, final VirtualMouse mouse, final EventBus eventBus,
                   final ItemManager itemManager, final BankService bankService, final ShopService shopService,
                   final InteractionManager interactionManager, final Provider<CameraService> cameraServiceProvider) {
        this.cameraServiceProvider = cameraServiceProvider;
        this.client = client;
        this.clientThread = clientThread;
        this.mouse = mouse;
        this.itemManager = itemManager;
        this.interactionManager = interactionManager;
        this.eventBus = eventBus;
        this.bankService = bankService;
        this.shopService = shopService;
        this.localPlayer = new LocalPlayerEntity(this);
        eventBus.register(this.localPlayer);
        eventBus.register(bankService);

        // ShopService learns shop prices from the game messages the "Value" action produces, which is
        // the only place the client ever sees them.
        eventBus.register(shopService);

        // RuneLite injects some logging into doAction when a menu action can't be found by the client but is still being
        // invoked with coordinates where the menu action should appear. This simply mutes those verbose logs.
        try {
            Field loggerField = client.getClass().getDeclaredField(HooksLoader.getSecurityHooks().getClientLogFieldName());
            loggerField.setAccessible(true);
            Object loggerInstance = loggerField.get(null);

            if (loggerInstance instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) loggerInstance).setLevel(ch.qos.logback.classic.Level.ERROR);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Failed modify log level for RuneLite doAction method. You may encounter more verbose logging.", e);
        }

        log.info("Game context initialized successfully, loaded {} packet definitions", HooksLoader.getPackets().size());
    }

    /**
     * Releases everything this context registered or started.
     *
     * <p>Call this from your plugin's {@code shutDown()}. RuneLite gives each plugin its own child
     * injector, so each plugin holds its own {@code Context} with its own EventBus subscriptions,
     * schedulers, and mouse listener — all attached to objects that outlive the plugin. Without this
     * call they survive a disable and accumulate across enable/disable cycles.</p>
     *
     * <p>Safe to call more than once; subsequent calls do nothing.</p>
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        unregisterQuietly(localPlayer);
        unregisterQuietly(bankService);
        unregisterQuietly(shopService);

        closeQuietly("local player", localPlayer::shutdown);
        closeQuietly("camera service", () -> cameraServiceProvider.get().shutdown());
        closeQuietly("virtual mouse", mouse::shutdown);

        log.info("Game context shut down");
    }

    /**
     * Unregisters an EventBus subscriber, tolerating one that was never registered.
     *
     * @param subscriber The object to unregister.
     */
    private void unregisterQuietly(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        try {
            eventBus.unregister(subscriber);
        } catch (Exception e) {
            log.debug("Failed to unregister {}: {}", subscriber.getClass().getSimpleName(), e.toString());
        }
    }

    /**
     * Runs one teardown step, logging and continuing if it fails.
     *
     * <p>Teardown steps are independent, so one failing must not strand the others.</p>
     *
     * @param what Human-readable name of the resource being released, used for logging.
     * @param step The teardown action.
     */
    private void closeQuietly(String what, Runnable step) {
        try {
            step.run();
        } catch (Exception e) {
            log.warn("Failed to release {}: {}", what, e.toString());
        }
    }

    /**
     * Wraps the RuneLite client's run script method scheduling the run on the client thread.
     * This is a convenience method for {@code ctx.runOnClientThread(() -> ctx.getClient().runScript(...));}
     * @param id The CS2 script id to run.
     */
    public void runScript(int id) {
        runOnClientThread(() -> client.runScript(id));
    }

    /**
     * Returns a varbit value from the RuneLite client. This method is
     *  thread-safe and runs on the client thread to retrieve the value.
     * @param varbit The varbit value to retrieve.
     * @return The varbit value (either 0 for false/unset or 1 for true/set).
     */
    public int getVarbitValue(int varbit) {
        return runOnClientThread(() -> client.getVarbitValue(varbit));
    }

    /**
     * Returns a var player value from the RuneLite client. These are values that the server controls. The client can
     * pre-emptively update these values for the next server tick but will not be able to coerce the server into reconciling to a specific
     * state. I.e. Client cannot change these values permanently. This method is
     * thread-safe and runs on the client thread to retrieve the value.
     * @param varp The varp value to retrieve.
     * @return The varp value (either 0 for false/unset or 1 for true/set).
     */
    public int getVarpValue(int varp) {
        return runOnClientThread(() -> client.getVarpValue(varp));
    }

    /**
     * Retrieves a Widget from the RuneLite client. This method is
     *  thread-safe and will run on the client thread to retrieve the Widget.
     * @param widgetId int The widget id
     * @return Widget
     */
    public Widget getWidget(int widgetId) {
        return runOnClientThread(() -> client.getWidget(widgetId));
    }


    /**
     * Retrieves an enum composition from the RuneLite client thread. This method is
     * thread-safe and will run on the client thread to retrieve the EnumComposition.
     * @param enumId The enum id
     * @return EnumComposition
     */
    public EnumComposition getEnum(int enumId) {
        return runOnClientThread(() -> client.getEnum(enumId));
    }

    /**
     * Run a method on the client thread, returning the result directly.
     *
     * <p>A {@code null} return means the work genuinely produced {@code null} (an absent widget, for
     * example). If the work could not be performed at all — the client thread did not answer within
     * {@link #CLIENT_THREAD_TIMEOUT_MS}, the callable threw, or this thread was interrupted — a
     * {@link ClientThreadException} is thrown instead. Both execution paths behave identically in this
     * respect, so a call does not change its failure mode depending on which thread issued it.</p>
     *
     * <p>Use {@link #runOnClientThread(Callable, Object)} or {@link #runOnClientThreadOptional(Callable)}
     * when degrading is preferable to failing.</p>
     *
     * @param method The method to call
     * @param <T> The type of the method's return value
     * @return The result from the called method, which may be {@code null}
     * @throws ClientThreadException if the work could not be completed on the client thread
     */
    public <T> T runOnClientThread(Callable<T> method) {
        if (method == null) {
            throw new IllegalArgumentException("Callable passed to runOnClientThread must not be null");
        }

        if (client.isClientThread()) {
            try {
                return method.call();
            } catch (Exception e) {
                throw new ClientThreadException("Client-thread work threw an exception", e);
            }
        }

        final CompletableFuture<T> future = new CompletableFuture<>();

        clientThread.invoke(() -> {
            try {
                future.complete(method.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get(CLIENT_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ClientThreadException("Client thread did not respond within "
                    + CLIENT_THREAD_TIMEOUT_MS + "ms; it is likely blocked or the client is loading", e);
        } catch (ExecutionException e) {
            throw new ClientThreadException("Client-thread work threw an exception", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClientThreadException("Interrupted while waiting on the client thread", e);
        }
    }

    /**
     * Run a method on the client thread, falling back to a supplied value when the work could not be
     * completed.
     *
     * <p>This never throws {@link ClientThreadException}; it is the explicit "I would rather degrade
     * than fail" form. A genuine {@code null} result is returned as {@code null}, not replaced by the
     * fallback — the fallback stands in only for a failed hand-off.</p>
     *
     * @param method The method to call
     * @param fallback The value to return if the client thread could not complete the work
     * @param <T> The type of the method's return value
     * @return The result from the called method, or {@code fallback} on failure
     */
    public <T> T runOnClientThread(Callable<T> method, T fallback) {
        try {
            return runOnClientThread(method);
        } catch (ClientThreadException e) {
            log.debug("Falling back after client-thread failure: {}", e.getMessage());
            return fallback;
        }
    }

    /**
     * Runs a method on the client thread without returning a result.
     * @param method Runnable method to execute
     */
    public void runOnClientThread(Runnable method) {
        if (client.isClientThread()) {
            method.run();
            return;
        }

        clientThread.invoke(method);
    }

    /**
     * Run a method on the client thread, returning an optional of the result.
     *
     * <p>Never throws: a failed hand-off and a genuine {@code null} result both yield an empty
     * {@link Optional}. Use {@link #runOnClientThread(Callable)} when you need to tell those apart.</p>
     *
     * @param method The method to call
     * @param <T> The type of the method's return value
     * @return The result from the called method, or empty if it was null or could not be obtained
     */
    public <T> Optional<T> runOnClientThreadOptional(Callable<T> method) {
        try {
            return Optional.ofNullable(runOnClientThread(method));
        } catch (ClientThreadException e) {
            log.debug("Client-thread work did not complete: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves an instance of a specified service class.
     *
     * <p>Resolves against RuneLite's root injector. Because each plugin gets its own child injector,
     * a service obtained here is not necessarily the same instance as one injected into your plugin.
     * Prefer injecting the service directly; use this only where injection is not available.</p>
     *
     * @param serviceClass The class of the service to retrieve.
     * @param <T>          The type of the service.
     * @return The instance of the service.
     * @throws IllegalStateException if RuneLite's injector is not available yet.
     */
    public <T> T getService(Class<T> serviceClass) {
        return Services.get(serviceClass);
    }

    /**
     * Creates a new query builder for NPCs.
     * Usage: {@code ctx.npcs().withName("Goblin").first().interact("Attack");}
     *
     * @return NpcQuery object used to chain together predicates to select specific NPC's within the scene.
     */
    public NpcQuery npcs() {
        return new NpcQuery(this);
    }

    /**
     * Creates a new query builder for Players. This will also include the local player as well which can be
     * grabbed with {@code .local()}.
     * Usage: {@code ctx.players().withName("Zezima").first().interact("Follow");}
     * {@code ctx.players().local().getName();}
     *
     * @return PlayerQuery object used to chain together predicates to select specific Players's within the scene.
     */
    public PlayerQuery players() {
        return new PlayerQuery(this);
    }

    /**
     * Creates a new query builder for the standard Backpack Inventory. This is only for finding items in a players inventory and
     * should not be used when the Bank is open to deposit items. Instead, use {@code BankInventoryQuery} for depositing items.
     * Usage: {@code ctx.inventory().withId(1234).count();}
     *
     * @return InventoryQuery object used to chain together predicates to select specific items or groups of items within
     * the players inventory.
     */
    public InventoryQuery inventory() {
        return new InventoryQuery(this);
    }

    /**
     * Creates a new query builder for a Bank Inventory. This should only be used when the bank is open in order to
     * deposit items from the players inventory into the bank. A different parent widget is used for the players inventory
     * while the bank is open compared to the normal players inventory. For querying the players inventory to eat food,
     * interact with objects, or perform general actions without a bank use: {@code InventoryQuery}.
     * Usage: {@code ctx.bankInventory().withId(1234).count();}
     *
     * @return BankInventoryQuery object used to chain together predicates to select specific items or groups of items within
     * the players inventory while the bank interface is open.
     */
    public BankInventoryQuery bankInventory() {
        return new BankInventoryQuery(this);
    }

    /**
     * Creates a new query builder for the Deposit box.
     * Usage: {@code ctx.depositBox().withId(1234).depositOne()}
     * @return DepositBoxQuery used to chain together deposit box operations.
     */
    public DepositBoxQuery depositBox() {
        return new DepositBoxQuery(this);
    }

    /**
     * Creates a new query builder for the Bank interface.
     * Usage: {@code ctx.bank().withId(1234).interact("Withdraw-X");}
     * @return BankQuery object used to chain together predicates to select specific items or groups of items within the players
     * bank.
     */
    public BankQuery bank() {
        return new BankQuery(this);
    }

    /**
     * Creates a new query builder for the items on an open shop's shelves. This returns nothing when no
     * shop is open. For opening a shop, and for buying under price or coin limits, use {@code ShopService}.
     * Usage: {@code ctx.shop().withName("Iron ore").first().buy(50);}
     *
     * @return ShopQuery object used to chain together predicates to select specific items the shop sells.
     */
    public ShopQuery shop() {
        return new ShopQuery(this);
    }

    /**
     * Creates a new query builder for the player's inventory as it is drawn beside an open shop. This should
     * only be used while the shop interface is open and you are selling items to the shop, since the shop
     * renders the inventory with its own widgets. For ordinary inventory work use {@code InventoryQuery}.
     * Usage: {@code ctx.shopInventory().withName("Bones").first().sell(10);}
     *
     * @return ShopInventoryQuery object used to chain together predicates to select specific items or groups
     * of items within the players inventory while the shop interface is open.
     */
    public ShopInventoryQuery shopInventory() {
        return new ShopInventoryQuery(this);
    }

    /**
     * Creates a new query builder for the equipment interface.
     * Usage: {@code ctx.equipment().inSlot(EquipmentInventorySlot.HEAD).interact("Remove");}
     * {@code ctx.equipment().withId(1234).interact("Wield");}
     *
     * @return EquipmentQuery object used to chain together predicates to select specific items or groups of items within the players
     * equipment or inventory interface. Only items with the action "wield" or "wear" will be interactable using this query from the inventory.
     */
    public EquipmentQuery equipment() {
        return new EquipmentQuery(this);
    }

    /**
     * Creates a new query builder for game objects. Game objects are objects in the game world like: Trees, ore, or fishing
     * spots which exist on tiles, can be interacted with, but cannot be picked up by the player. Usage:
     * {@code ctx.gameObjects().withName("Oak Tree").nearest().interact("Chop");}
     *
     * @return GameObjectQuery used to chain together predicates to select specific game objects within the scene.
     */
    public GameObjectQuery gameObjects() {
        return new GameObjectQuery(this);
    }

    /**
     * Creates a new query builder for Ground Items. GroundItems are items that exist on a tile that the player can pick up
     * and store in their inventory. Examples include: bones dropped from an NPC or loot dropped by another player on a tile.
     * Usage: {@code ctx.groundObjects().withName("Twisted Bow").nearest().interact("Take");}
     *
     * @return GroundObjectQuery used to chain together predicates to select specific ground items within the scene.
     */
    public GroundObjectQuery groundItems() {
        return new GroundObjectQuery(this);
    }

    /**
     * Creates a new query builder for Widgets. Usage: {@code ctx.widgets().withText("Log Out").interact();}
     * @return WidgetQuery used to chain together predicates to select specific widgets within the client.
     */
    public WidgetQuery widgets() {
        return new WidgetQuery(this);
    }

    /**
     * Creates a new query builder for Worlds. A WorldQuery provides functionality
     * for filtering and selecting specific game worlds based on various criteria,
     * such as population, world type, or location.
     *
     * <p>Worlds are the game servers that players can connect to. Each world may
     * have unique properties, such as member status, high population, or special
     * game rules.</p>
     *
     * @return {@literal @}WorldQuery object used to chain together predicates to
     *         select specific game worlds.
     */
    public WorldQuery worlds() {
        return new WorldQuery(this);
    }
}
