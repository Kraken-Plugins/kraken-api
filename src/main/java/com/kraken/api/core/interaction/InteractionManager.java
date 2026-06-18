package com.kraken.api.core.interaction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.model.MenuOption;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import com.kraken.api.core.interaction.resolver.MenuActionResolverRegistry;
import com.kraken.api.core.interaction.resolver.WidgetSubActionResolver;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.bank.BankItemWidget;
import com.kraken.api.query.groundobject.GroundItem;
import com.kraken.api.service.ui.UIService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.Arrays;
import java.util.Optional;

/**
 * Public API for interacting with in-game entities. Delegates all resolution
 * to {@link MenuActionResolverRegistry} and all dispatching to {@link InteractionDispatcher}.
 */
@Slf4j
@Singleton
public class InteractionManager {

    @Inject
    private MenuActionResolverRegistry registry;

    @Inject
    private InteractionDispatcher dispatcher;

    @Inject
    private WidgetSubActionResolver subActionResolver;

    @Inject
    private Provider<Context> ctxProvider;

    @Inject
    @Getter
    private WidgetPackets widgetPackets;

    private <T> void interact(T entity, Class<T> type, String action, Point point) {
        registry.getResolver(type)
                .flatMap(r -> r.resolve(entity, action))
                .ifPresentOrElse(
                        resolved -> dispatcher.dispatch(point, action, resolved),
                        () -> log.warn("No resolver or resolution failed for type={} action={}",
                                type.getSimpleName(), action)
                );
    }

    /**
     * Interacts with an NPC using a specified action.
     *
     * @param npc    The NPC to interact with.
     * @param action The action to perform (e.g., "Attack", "Pickpocket").
     */
    public void interact(NPC npc, String action) {
        interact(npc, NPC.class, action, UIService.getClickbox(npc));
    }

    /**
     * Interacts with another player using a specified action.
     *
     * @param player The player to interact with.
     * @param action The action to perform (e.g., "Trade", "Follow").
     */
    public void interact(Player player, String action) {
        interact(player, Player.class, action, UIService.getClickbox(player));
    }

    /**
     * Interacts with a game object using a specified action.
     *
     * @param object The TileObject (game object) to interact with.
     * @param action The action to perform (e.g., "Chop down", "Mine").
     */
    public void interact(TileObject object, String action) {
        interact(object, TileObject.class, action, UIService.getClickbox(object));
    }

    /**
     * Picks up a ground item. Defaults to the "Take" action.
     *
     * @param item The ground item to pick up.
     */
    public void interact(GroundItem item) {
        interact(item, "Take");
    }

    /**
     * Interacts with a ground item using a specified action.
     *
     * @param item   The ground item to interact with.
     * @param action The action to perform (e.g., "Take", "Cast").
     */
    public void interact(GroundItem item, String action) {
        interact(item, GroundItem.class, action, UIService.getClickbox(item.getTileObject()));
    }

    /**
     * Interacts with a UI widget using a specified action.
     *
     * @param widget The widget to interact with.
     * @param action The action to perform.
     */
    public void interact(Widget widget, String action) {
        interact(widget, Widget.class, action, UIService.getClickbox(widget));
    }

    /**
     * Interacts with an item inside the bank interface.
     *
     * @param item   The bank item widget to interact with.
     * @param action The action to perform (e.g., "Withdraw-1", "Withdraw-All").
     */
    public void interact(BankItemWidget item, String action) {
        interact(item, BankItemWidget.class, action, UIService.getClickbox(item));
    }

    /**
     * Interacts with an item within a container (such as the player's inventory).
     *
     * @param item   The container item to interact with.
     * @param action The action to perform (e.g., "Drop", "Wield").
     */
    public void interact(ContainerItem item, String action) {
        ctxProvider.get().runOnClientThread(() -> {
            if (item == null) return;
            Widget w = item.getWidget();
            if (w == null) return;
            interact(w, Widget.class, action, UIService.getClickbox(item));
        });
    }

    /**
     * Attempts to interact with a container item by trying a sequence of actions.
     * Dispatches the first action that successfully resolves.
     *
     * @param item    The container item to interact with.
     * @param actions An array of actions to attempt, in order of priority.
     */
    public void interact(ContainerItem item, String... actions) {
        ctxProvider.get().runOnClientThread(() -> {
            if (item == null) return;
            Widget w = item.getWidget();
            if (w == null) {
                log.error("Failed to resolve widget for item interaction: {}", item.getName());
                return;
            }

            Point pt = UIService.getClickbox(item);
            for (String action : actions) {
                Optional<ResolvedMenuAction> resolved = registry.getResolver(Widget.class)
                        .flatMap(r -> r.resolve(w, action));

                if (resolved.isPresent()) {
                    dispatcher.dispatch(pt, action, resolved.get());
                    return;
                }
            }

            log.warn("Failed to resolve any of the specified actions {} for item: {}",
                    Arrays.toString(actions), item.getName());
        });
    }
    
    /**
     * Handles dialogue progression or selection using a widget's packed ID.
     *
     * @param packedWidgetId The packed ID of the dialogue widget.
     * @param option         The dialogue option to select (-1 for continue, 1-5 for choices).
     */
    public void interact(int packedWidgetId, int option) {
        Widget widget = ctxProvider.get().getClient().getWidget(packedWidgetId);
        if (widget == null) return;
        interact(widget, option);
    }

    /**
     * Handles dialogue progression or selection using a specific widget.
     *
     * @param widget The dialogue widget.
     * @param option The dialogue option to select (-1 for continue, 1-5 for choices).
     */
    public void interact(Widget widget, int option) {
        if (widget == null) return;

        Point pt = UIService.getClickbox(widget);
        int worldView = ctxProvider.get().getClient().getTopLevelWorldView().getId();

        // option = -1 for normal NPC/player continue, or 1–5 for dynamic dialogue choices
        MenuOption opt = new MenuOption(MenuAction.WIDGET_CONTINUE, 0, option,
                widget.getId(), -1, worldView);

        dispatcher.dispatch(pt, "Continue", new ResolvedMenuAction(opt, ""));
    }

    /**
     * Sets the player's heading/camera direction.
     *
     * @param heading The heading value to set.
     */
    public void interact(int heading) {
        int worldView = ctxProvider.get().getClient().getTopLevelWorldView().getId();
        MenuOption option = new MenuOption(MenuAction.SET_HEADING, heading, 0, 0, 0, worldView);
        dispatcher.dispatch(
                ctxProvider.get().getClient().getMouseCanvasPosition(),
                "Set heading",
                new ResolvedMenuAction(option, "")
        );
    }

    /**
     * Performs a nested or sub-action on a widget, such as right-click equipment teleports.
     *
     * @param item   The widget item to interact with.
     * @param menu   The parent menu name (e.g., "Rub").
     * @param action The specific sub-action to perform (e.g., "Grand Exchange").
     */
    public void interact(Widget item, String menu, String action) {
        Point pt = UIService.getClickbox(item);

        subActionResolver.resolve(item, menu, action)
                .ifPresentOrElse(
                        resolved -> dispatcher.dispatch(pt, action, resolved),
                        () -> log.warn("Failed to resolve sub-action menu='{}' action='{}' on widget: {}",
                                menu, action, item.getId())
                );
    }


    /**
     * Uses one widget on another widget (e.g., casting High Alchemy on an inventory item,
     * or using a chisel on a gem).
     *
     * @param src  The source widget (the item or spell being used).
     * @param dest The destination widget (the item being targeted).
     */
    public void interact(Widget src, Widget dest) {
        Point srcPoint  = UIService.getClickbox(src);
        Point destPoint = UIService.getClickbox(dest);

        // First dispatch sets isWidgetSelected() = true on the client
        registry.getResolver(Widget.class)
                .flatMap(r -> r.resolve(src, src.getTargetVerb()))
                .ifPresent(srcResolved -> {
                    dispatcher.dispatch(srcPoint, src.getTargetVerb(), srcResolved);

                    // Second resolve now sees isWidgetSelected() == true → WIDGET_TARGET_ON_WIDGET
                    registry.getResolver(Widget.class)
                            .flatMap(r -> r.resolve(dest, dest.getTargetVerb()))
                            .ifPresentOrElse(
                                    destResolved -> dispatcher.dispatch(destPoint, dest.getTargetVerb(), destResolved),
                                    () -> log.warn("Failed to resolve dest widget for Widget→Widget: id={}", dest.getId())
                            );
                });
    }

    /**
     * Uses a widget on a game object (e.g., using a bucket on a fountain or a spell on an object).
     *
     * @param src  The source widget (the item or spell).
     * @param dest The destination TileObject in the game world.
     */
    public void interact(Widget src, TileObject dest) {
        Point srcPoint  = UIService.getClickbox(src);
        Point destPoint = UIService.getClickbox(dest);

        registry.getResolver(Widget.class)
                .flatMap(r -> r.resolve(src, src.getTargetVerb()))
                .ifPresent(srcResolved -> {
                    dispatcher.dispatch(srcPoint, src.getTargetVerb(), srcResolved);

                    // Build WIDGET_TARGET_ON_GAME_OBJECT directly — the generic TileObject
                    // resolver would not see isWidgetSelected() == true in time, so we
                    // construct it explicitly to mirror the original behavior.
                    ctxProvider.get().runOnClientThread(() -> {
                        Client client = ctxProvider.get().getClient();
                        int worldView = client.getTopLevelWorldView().getId();

                        Point scenePoint;

                        if (dest instanceof GameObject) {
                            GameObject go = (GameObject) dest;
                            scenePoint = go.getSceneMinLocation();
                        } else {
                            scenePoint = new Point(dest.getLocalLocation().getSceneX(),
                                    dest.getLocalLocation().getSceneY());
                        }

                        String name = Optional.ofNullable(getObjectComposition(client, dest))
                                .map(ObjectComposition::getName)
                                .orElse("");

                        MenuOption option = new MenuOption(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
                                dest.getId(), scenePoint.getX(), scenePoint.getY(), -1, worldView);

                        String target = srcResolved.getTarget() + " -> " + name;
                        dispatcher.dispatch(destPoint, src.getTargetVerb(), new ResolvedMenuAction(option, target));
                    });
                });
    }

    /**
     * Uses a widget on an NPC (e.g., casting Crumble Undead on a Vorkath spawn,
     * or using an item on an NPC).
     *
     * @param src  The source widget (the item or spell).
     * @param dest The destination NPC.
     */
    public void interact(Widget src, NPC dest) {
        Point srcPoint  = UIService.getClickbox(src);
        Point destPoint = UIService.getClickbox(dest);

        registry.getResolver(Widget.class)
                .flatMap(r -> r.resolve(src, src.getTargetVerb()))
                .ifPresent(srcResolved -> {
                    dispatcher.dispatch(srcPoint, src.getTargetVerb(), srcResolved);

                    ctxProvider.get().runOnClientThread(() -> {
                        Client client = ctxProvider.get().getClient();
                        int worldView = client.getTopLevelWorldView().getId();

                        String npcName = dest.getName() == null ? "" : dest.getName();
                        String target  = srcResolved.getTarget() + " -> " + npcName;

                        MenuOption option = new MenuOption(MenuAction.WIDGET_TARGET_ON_NPC,
                                dest.getIndex(), 0, 0, -1, worldView);

                        dispatcher.dispatch(destPoint, src.getTargetVerb(), new ResolvedMenuAction(option, target));
                    });
                });
    }


    /**
     * Uses a widget on a ground item (e.g., casting Telekinetic Grab on dropped loot).
     *
     * @param src  The source widget (the spell or item).
     * @param dest The destination ground item.
     */
    public void interact(Widget src, GroundItem dest) {
        Point srcPoint  = UIService.getClickbox(src);
        Point destPoint = UIService.getClickbox(dest.getTileObject());

        registry.getResolver(Widget.class)
                .flatMap(r -> r.resolve(src, src.getTargetVerb()))
                .ifPresent(srcResolved -> {
                    dispatcher.dispatch(srcPoint, src.getTargetVerb(), srcResolved);

                    ctxProvider.get().runOnClientThread(() -> {
                        Client client = ctxProvider.get().getClient();
                        int worldView = client.getTopLevelWorldView().getId();

                        LocalPoint location = dest.getTileObject().getLocalLocation();
                        String itemName = dest.getName() == null ? "" : dest.getName();
                        String target   = srcResolved.getTarget() + " -> " + itemName;

                        MenuOption option = new MenuOption(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM,
                                dest.getTileItem().getId(),
                                location.getSceneX(), location.getSceneY(),
                                -1, worldView);

                        dispatcher.dispatch(destPoint, src.getTargetVerb(), new ResolvedMenuAction(option, target));
                    });
                });
    }

    /**
     * Interacts with a widget by resolving its context and dispatching the specified interaction.
     * This method performs the following tasks:
     * <ul>
     *   <li>Ensures that packets are loaded before proceeding.</li>
     *   <li>Resolves the widget using the provided {@code widgetId}.</li>
     *   <li>Determines the display name for the action based on the specified {@code action} index.</li>
     *   <li>Dispatches the interaction to the game's input system.</li>
     * </ul>
     * If the widget cannot be resolved or the action name cannot be determined, appropriate warnings
     * will be logged.
     *
     * @param widgetId    the unique identifier of the widget to be interacted with.
     *                    This corresponds to the parent widget in the user interface.
     * @param childId     the identifier of the child element within the widget being targeted.
     *                    If no child element is specified, this is typically {@code -1}.
     * @param itemId      the identifier of a specific item within the widget, if applicable.
     *                    If not targeting an item, this is typically {@code -1}.
     * @param action      the zero-based index of the action to be performed from the widget's action list.
     *                    This determines the type of interaction executed (e.g., click, examine).
     */
    public void interact(int widgetId, int childId, int itemId, int action) {
        Context ctx = ctxProvider.get();
        Widget widget = ctx.getWidget(widgetId);
        if (widget == null) {
            log.error("Failed to resolve widget for interaction: widgetId={}", widgetId);
            return;
        }

        Point pt = UIService.getClickbox(widget);
        int worldView = ctx.getClient().getTopLevelWorldView().getId();

        // Resolve the display name for this action index (1-based)
        String actionName = "";
        String[] actions = widget.getActions();
        if (actions != null && action > 0 && action <= actions.length) {
            String candidate = actions[action - 1];
            if (candidate != null && !candidate.isEmpty()) {
                actionName = candidate;
            }
        }

        if (actionName.isEmpty()) {
            log.warn("Failed to resolve action name for action index={} on widgetId={}", action, widgetId);
        }

        String target = widget.getName();
        if (target == null || target.isBlank()) {
            target = widget.getText();
        }

        MenuOption option = new MenuOption(MenuAction.CC_OP, action, childId, widgetId, itemId, worldView);
        dispatcher.dispatch(pt, actionName, new ResolvedMenuAction(option, target == null ? "" : Text.removeTags(target)));
    }

    /**
     * Retrieves the {@link ObjectComposition} of the specified {@link TileObject}.
     * <p>
     * This method runs on the client thread to ensure safe access to client state.
     * It first retrieves the {@link ObjectComposition} corresponding to the object's ID.
     * If the composition has been transformed (via impostor), the transformed
     * {@link ObjectComposition} is returned; otherwise, the original composition is returned.
     *
     * @param client the {@link Client} instance used to interact with the game state.
     * @param object the {@link TileObject} whose {@link ObjectComposition} is to be fetched.
     *               The {@link TileObject} must have a valid ID associated with it.
     * @return the {@link ObjectComposition} of the given {@link TileObject}, or {@code null}
     *         if no composition could be retrieved or determined.
     */
    public static ObjectComposition getObjectComposition(Client client, TileObject object) {
        ObjectComposition composition = client.getObjectDefinition(object.getId());
        if (composition == null) {
            log.error("Failed to resolve object composition for object: {}", object.getId());
            return null;
        }

        if(composition.getImpostorIds() == null) {
            return composition;
        }

        ObjectComposition transformed = composition.getImpostor();
        return transformed != null ? transformed : composition;
    }
}