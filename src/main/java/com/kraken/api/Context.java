package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.ClientThreadException;
import com.kraken.api.core.ClientThreadGateway;
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
import com.kraken.api.query.graphicsobject.GraphicsObjectQuery;
import com.kraken.api.query.groundobject.GroundObjectQuery;
import com.kraken.api.query.npc.NpcQuery;
import com.kraken.api.query.projectile.ProjectileQuery;
import com.kraken.api.query.tileobject.TileObjectQuery;
import com.kraken.api.query.player.LocalPlayerEntity;
import com.kraken.api.query.player.PlayerQuery;
import com.kraken.api.query.widget.WidgetQuery;
import com.kraken.api.query.world.WorldQuery;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.shop.ShopService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * The single entry point to the Kraken API.
 *
 * <p>One Context exists per game client and every plugin shares it. RuneLite gives each plugin a
 * child injector, but Guice places a just-in-time singleton in the outermost injector that can
 * satisfy its dependencies, and every Kraken type depends only on RuneLite and other Kraken types.
 * The root injector therefore owns this Context and every {@code @Singleton} service it exposes,
 * and {@link #getService(Class)} returns the same instances a plugin has injected. Do not bind Kraken
 * types in a plugin module: an explicit child binding creates a second, private copy that the rest of
 * the API cannot see.</p>
 *
 * <p>Because the Context is client-wide, it lives for the life of the client. Plugins do not release
 * it; a plugin's own {@code shutDown()} should stop the scripts and break handling it started and
 * nothing more.</p>
 */
@Slf4j
@Singleton
public class Context {

    /**
     * How long a caller will wait for the client thread before giving up.
     *
     * <p>The client thread services this queue once per frame, so anything approaching this budget
     * means it is blocked or the client is loading — not that the work is slow.</p>
     */
    public static final long CLIENT_THREAD_TIMEOUT_MS = 3_000L;

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

    private final ClientThreadGateway commandGateway;

    @Inject
    public Context(final Client client, final ClientThread clientThread, final VirtualMouse mouse, final EventBus eventBus,
                   final ItemManager itemManager, final BankService bankService, final ShopService shopService,
                   final InteractionManager interactionManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.commandGateway = new ClientThreadGateway(client, clientThread, CLIENT_THREAD_TIMEOUT_MS);
        this.mouse = mouse;
        this.itemManager = itemManager;
        this.interactionManager = interactionManager;
        this.localPlayer = new LocalPlayerEntity(this);

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

        // Registered last so that a constructor failure above leaves nothing attached to the event bus.
        eventBus.register(bankService);

        // ShopService learns shop prices from the game messages the "Value" action produces, which is
        // the only place the client ever sees them.
        eventBus.register(shopService);

        log.info("Game context initialized successfully, loaded {} packet definitions", HooksLoader.getPackets().size());
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
     * respect. Pending work is revoked on timeout or interruption. If execution already
     * began, timeout/interruption reports an unknown outcome and cannot undo its effects.</p>
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
        return commandGateway.call(method);
    }

    /**
     * Run a method on the client thread, falling back to a supplied value when the work could not be
     * completed.
     *
     * <p>Failures use the fallback unless execution started before a timeout or interruption;
     * an unknown outcome is propagated as {@link ClientThreadException}. A genuine {@code null}
     * result is returned as {@code null}, not replaced by the
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
            if (e.isOutcomeUnknown()) {
                throw e;
            }
            log.debug("Falling back after client-thread failure: {}", e.getMessage());
            return fallback;
        }
    }

    /**
     * Runs inline on the client thread, otherwise queues work without waiting. Pending work expires
     * after the client-thread timeout; asynchronous failures are logged.
     * @param method Runnable method to execute
     */
    public void runOnClientThread(Runnable method) {
        commandGateway.execute(method);
    }

    /**
     * Run a method on the client thread, returning an optional of the result.
     *
     * <p>A failed hand-off and a genuine {@code null} result both yield an empty
     * {@link Optional}. Unknown outcomes after execution starts are thrown, so callers do not
     * mistake an in-flight action for a safe-to-retry failure.</p>
     *
     * @param method The method to call
     * @param <T> The type of the method's return value
     * @return The result from the called method, or empty if it was null or could not be obtained
     */
    public <T> Optional<T> runOnClientThreadOptional(Callable<T> method) {
        try {
            return Optional.ofNullable(runOnClientThread(method));
        } catch (ClientThreadException e) {
            if (e.isOutcomeUnknown()) {
                throw e;
            }
            log.debug("Client-thread work did not complete: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves an instance of a specified service class.
     *
     * <p>Resolves against RuneLite's root injector, which owns every Kraken singleton, so this returns
     * the same instance a plugin has injected. Prefer injecting the service directly; use this only
     * where injection is not available.</p>
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
     * Usage: {@code ctx.npcs().withName("Goblin").interact("Attack");}
     *
     * @return NpcQuery object used to chain together predicates to select specific NPC's within the scene.
     */
    public NpcQuery npcs() {
        return new NpcQuery(this);
    }

    /**
     * Creates a new query builder for Players. This will also include the local player as well which can be
     * grabbed with {@code .local()}.
     * Usage: {@code ctx.players().withName("Zezima").interact("Follow");}
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
     * Usage: {@code ctx.shop().withName("Iron ore").first().ifPresent(item -&gt; item.buy(50));}
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
     * Usage: {@code ctx.shopInventory().withName("Bones").first().ifPresent(item -&gt; item.sell(10));}
     *
     * @return ShopInventoryQuery object used to chain together predicates to select specific items or groups
     * of items within the players inventory while the shop interface is open.
     */
    public ShopInventoryQuery shopInventory() {
        return new ShopInventoryQuery(this);
    }

    /**
     * Creates a new query builder for the equipment interface.
     * Usage: {@code ctx.equipment().inSlot(EquipmentInventorySlot.HEAD).ifPresent(e -&gt; e.interact("Remove"));}
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
     * {@code ctx.gameObjects().withName("Oak Tree").sortByDistance().interact("Chop");}
     *
     * @return GameObjectQuery used to chain together predicates to select specific game objects within the scene.
     */
    public GameObjectQuery gameObjects() {
        return new GameObjectQuery(this);
    }

    /**
     * Creates a new query builder for Ground Items. GroundItems are items that exist on a tile that the player can pick up
     * and store in their inventory. Examples include: bones dropped from an NPC or loot dropped by another player on a tile.
     * Usage: {@code ctx.groundObjects().withName("Twisted Bow").sortByDistance().interact("Take");}
     *
     * @return GroundObjectQuery used to chain together predicates to select specific ground items within the scene.
     */
    public GroundObjectQuery groundItems() {
        return new GroundObjectQuery(this);
    }

    /**
     * Creates a new query builder for tile objects. Where {@link #gameObjects()} sees only game objects, this
     * query also sees wall, decorative and ground objects, which is what doors, gates and archways usually are.
     * Usage: {@code ctx.tileObjects().withName("Door").sortByDistance().interact("Open");}
     *
     * @return TileObjectQuery used to chain together predicates to select scenery of any kind within the scene.
     */
    public TileObjectQuery tileObjects() {
        return new TileObjectQuery(this);
    }

    /**
     * Creates a new query builder for projectiles currently in flight: boss attacks, spells, arrows.
     * Projectiles cannot be interacted with; the query exists for dodge and prayer logic.
     * Usage: {@code ctx.projectiles().withId(MAGIC_ATTACK).targetingMe().isPresent();}
     *
     * @return ProjectileQuery used to chain together predicates to select projectiles in flight.
     */
    public ProjectileQuery projectiles() {
        return new ProjectileQuery(this);
    }

    /**
     * Creates a new query builder for graphics objects playing in the scene: AOE impact markers,
     * spell effects, boss telegraphs. Graphics objects cannot be interacted with; the query exists
     * for hazard detection. Usage: {@code ctx.graphicsObjects().withId(TELEGRAPH).at(myTile).isPresent();}
     *
     * @return GraphicsObjectQuery used to chain together predicates to select active graphics objects.
     */
    public GraphicsObjectQuery graphicsObjects() {
        return new GraphicsObjectQuery(this);
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
