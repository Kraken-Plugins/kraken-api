package com.kraken.api.query;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.GameObjectPackets;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.NPCPackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.core.packet.model.PacketFactory;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.bank.BankItemWidget;
import com.kraken.api.query.groundobject.GroundItem;
import com.kraken.api.service.ui.UIService;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.util.Text;

import java.lang.reflect.Method;
import java.util.function.IntFunction;

/**
 * Manages interactions across various game entities like NPC's, Players, Widgets, GameObjects, TileObjects and more.
 */
@Slf4j
@Getter
@Singleton
public class InteractionManager {
    private static final MenuAction[] NPC_ACTIONS = {
            MenuAction.NPC_FIRST_OPTION,
            MenuAction.NPC_SECOND_OPTION,
            MenuAction.NPC_THIRD_OPTION,
            MenuAction.NPC_FOURTH_OPTION,
            MenuAction.NPC_FIFTH_OPTION
    };

    private static final MenuAction[] PLAYER_ACTIONS = {
            MenuAction.PLAYER_FIRST_OPTION,
            MenuAction.PLAYER_SECOND_OPTION,
            MenuAction.PLAYER_THIRD_OPTION,
            MenuAction.PLAYER_FOURTH_OPTION,
            MenuAction.PLAYER_FIFTH_OPTION,
            MenuAction.PLAYER_SIXTH_OPTION,
            MenuAction.PLAYER_SEVENTH_OPTION,
            MenuAction.PLAYER_EIGHTH_OPTION
    };

    private static final MenuAction[] GAME_OBJECT_ACTIONS = {
            MenuAction.GAME_OBJECT_FIRST_OPTION,
            MenuAction.GAME_OBJECT_SECOND_OPTION,
            MenuAction.GAME_OBJECT_THIRD_OPTION,
            MenuAction.GAME_OBJECT_FOURTH_OPTION,
            MenuAction.GAME_OBJECT_FIFTH_OPTION
    };

    private static final MenuAction[] GROUND_ITEM_ACTIONS = {
            MenuAction.GROUND_ITEM_FIRST_OPTION,
            MenuAction.GROUND_ITEM_SECOND_OPTION,
            MenuAction.GROUND_ITEM_THIRD_OPTION,
            MenuAction.GROUND_ITEM_FOURTH_OPTION,
            MenuAction.GROUND_ITEM_FIFTH_OPTION
    };

    @Inject
    private NPCPackets npcPackets;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private WidgetPackets widgetPackets;

    @Inject
    private GameObjectPackets gameObjectPackets;

    @Inject
    private Provider<Context> ctxProvider;

    private Class<?> doActionClass;
    private Method doActionMethod;

    @SneakyThrows
    private void invokeMenu(int param0, int param1, int opcode, int identifier, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY) {
        Context ctx = RuneLite.getInjector().getInstance(Context.class);
        Client client = ctx.getClient();

        int garbageValue = Integer.parseInt(PacketFactory.getPacketMetadata().getDoActionGarbageValue());
        if(doActionClass == null) {
            String doActionClassName = PacketFactory.getPacketMetadata().getDoActionClassName();
            String doActionMethodName = PacketFactory.getPacketMetadata().getDoActionMethodName();

            doActionClass = client.getClass().getClassLoader().loadClass(doActionClassName);
            for(Method m : doActionClass.getDeclaredMethods()) {
                if(m.getName().equalsIgnoreCase(doActionMethodName)) {
                    doActionMethod = m;
                }
            }
        }

        if(doActionMethod == null) {
            log.error("Could not get doAction class/method with reflection.");
            return;
        }

        doActionMethod.setAccessible(true);
        ctx.runOnClientThreadOptional(() -> doActionMethod.invoke(null, param0, param1, opcode, identifier, itemId, worldViewId, option, target, canvasX, canvasY, garbageValue));
        doActionMethod.setAccessible(false);
    }

    private MenuOption resolve(Client client, NPC npc, String op, int worldView) {
        LocalPoint point = npc.getLocalLocation();
        if (client.isWidgetSelected() && op.equalsIgnoreCase("Use")) {
            return new MenuOption(MenuAction.WIDGET_TARGET_ON_NPC, npc.getIndex(), point.getSceneX(), point.getSceneY(), -1, worldView);
        }

        NPCComposition composition = npc.getComposition();
        if (composition == null) {
            return null;
        }

        NPCComposition transformed = npc.getTransformedComposition();
        if (transformed != null) {
            composition = transformed;
        }

        String[] ops = composition.getActions();
        return resolveAction(op, ops, i -> i < NPC_ACTIONS.length
                ? new MenuOption(NPC_ACTIONS[i], npc.getIndex(), point.getSceneX(), point.getSceneY(), -1, worldView)
                : null);
    }

    private MenuOption resolve(Client client, Player player, String op, int worldView) {
        LocalPoint point = player.getLocalLocation();
        if (client.isWidgetSelected() && op.equalsIgnoreCase("Use")) {
            return new MenuOption(MenuAction.WIDGET_TARGET_ON_PLAYER, player.getId(), point.getSceneX(), point.getSceneY(), -1, worldView);
        }

        return resolveAction(op, client.getPlayerOptions(), i -> i < PLAYER_ACTIONS.length
                ? new MenuOption(PLAYER_ACTIONS[i], player.getId(), point.getSceneX(), point.getSceneY(), -1, worldView)
                : null);
    }

    private MenuOption resolve(Client client, TileObject object, String op, int worldView) {
        int sceneX;
        int sceneY;

        // Multi-tile game objects must use their South-West (minimum) tile coordinate.
        // Standard TileObjects can fall back to standard local point conversion.
        if (object instanceof GameObject) {
            GameObject go = (GameObject) object;
            sceneX = go.getSceneMinLocation().getX();
            sceneY = go.getSceneMinLocation().getY();
        } else {
            LocalPoint localPoint = object.getLocalLocation();
            sceneX = localPoint.getSceneX();
            sceneY = localPoint.getSceneY();
        }

        if (client.isWidgetSelected() && op.equalsIgnoreCase("Use")) {
            return new MenuOption(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, object.getId(), sceneX, sceneY, -1, worldView);
        }

        ObjectComposition composition = getObjectComposition(client, object);
        if (composition == null) {
            log.info("Failed to resolve object composition for object: {}", object.getId());
            return null;
        }

        return resolveAction(op, composition.getActions(), i -> i < GAME_OBJECT_ACTIONS.length
                ? new MenuOption(GAME_OBJECT_ACTIONS[i], object.getId(), sceneX, sceneY, -1, worldView)
                : null);
    }

    private MenuOption resolve(Client client, GroundItem item, String op, int worldView) {
        TileObject tileObject = item.getTileObject();
        LocalPoint point;
        if (tileObject != null) {
            point = tileObject.getLocalLocation();
        } else {
            point = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), item.getLocation());
        }

        if (point == null) {
            return null;
        }

        if (client.isWidgetSelected() && op.equalsIgnoreCase("Use")) {
            return new MenuOption(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM, item.getTileItem().getId(), point.getSceneX(), point.getSceneY(), -1, worldView);
        }

        // OSRS Client hardcodes "Take" as the 3rd option (Index 2).
        // It is rarely present in the actual ItemComposition ground actions array.
        if (op.equalsIgnoreCase("Take")) {
            return new MenuOption(MenuAction.GROUND_ITEM_THIRD_OPTION, item.getTileItem().getId(), point.getSceneX(), point.getSceneY(), -1, worldView);
        }

        return resolveAction(op, GroundItem.getGroundItemActions(item.getItemComposition()), i -> i < GROUND_ITEM_ACTIONS.length
                ? new MenuOption(GROUND_ITEM_ACTIONS[i], item.getTileItem().getId(), point.getSceneX(), point.getSceneY(), -1, worldView)
                : null);
    }

    private MenuOption resolve(Client client, Widget widget, String op, int worldView) {
        if (widget == null || !widget.isIf3()) {
            return null;
        }

        if (client.isWidgetSelected() && (op.equalsIgnoreCase("Use") || op.equalsIgnoreCase("Cast"))) {
            return new MenuOption(MenuAction.WIDGET_TARGET_ON_WIDGET, 0, widget.getIndex(), widget.getId(), widget.getItemId(), worldView);
        }

        String targetVerb = widget.getTargetVerb();
        if (matchesAction(op, targetVerb) || op.equalsIgnoreCase("Use")) {
            return new MenuOption(MenuAction.WIDGET_TARGET, 0, widget.getIndex(), widget.getId(), widget.getItemId(), worldView);
        }

        MenuOption widgetAction = resolveAction(op, widget.getActions(), i -> new MenuOption(
                i > widget.getTargetPriority() ? MenuAction.CC_OP_LOW_PRIORITY : MenuAction.CC_OP,
                i + 1,
                widget.getIndex(),
                widget.getId(),
                widget.getItemId(),
                worldView
        ));

        if (widgetAction != null) {
            return widgetAction;
        }

        if ((widget.getClickMask() & 0x1) != 0) {
            return new MenuOption(MenuAction.WIDGET_CONTINUE, 0, widget.getIndex(), widget.getId(), widget.getItemId(), worldView);
        }

        return null;
    }

    private ResolvedMenuAction resolveNpcInteraction(NPC npc, String action) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            MenuOption option = resolve(client, npc, action, client.getTopLevelWorldView().getId());
            if (option == null) {
                return null;
            }

            return new ResolvedMenuAction(option, npc.getName() == null ? "" : npc.getName());
        });
    }

    private ResolvedMenuAction resolvePlayerInteraction(Player player, String action) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            MenuOption option = resolve(client, player, action, client.getTopLevelWorldView().getId());
            if (option == null) {
                return null;
            }

            return new ResolvedMenuAction(option, player.getName() == null ? "" : player.getName());
        });
    }

    private ResolvedMenuAction resolveTileObjectInteraction(TileObject object, String action) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            MenuOption option = resolve(client, object, action, client.getTopLevelWorldView().getId());
            if (option == null) {
                log.info("Failed to resolve tile object interaction: {}, action: {}", object.getId(), action);
                return null;
            }

            ObjectComposition composition = getObjectComposition(client, object);
            String target = composition == null || composition.getName() == null ? "" : composition.getName();
            return new ResolvedMenuAction(option, target);
        });
    }

    private ResolvedMenuAction resolveWidgetInteraction(Widget widget, String action) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            MenuOption option = resolve(client, widget, action, client.getTopLevelWorldView().getId());
            if (option == null) {
                log.info("Failed to resolve widget interaction: {}, action: {}", widget.getId(), action);
                return null;
            }

            String target = widget.getName();
            if (target == null || target.isBlank()) {
                target = widget.getText();
            }

            return new ResolvedMenuAction(option, target == null ? "" : Text.removeTags(target));
        });
    }

    private ResolvedMenuAction resolveGroundItemInteraction(GroundItem item, String action) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            MenuOption option = resolve(client, item, action, client.getTopLevelWorldView().getId());
            if (option == null) {
                log.info("Failed to resolve ground item interaction: {}, action: {}", item.getId(), action);
                return null;
            }

            return new ResolvedMenuAction(option, item.getName() == null ? "" : item.getName());
        });
    }


    private ResolvedMenuAction resolveWidgetSubAction(Widget widget, String primaryMenu, String subActionName) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();

            if (widget == null || widget.getItemId() == -1) {
                return null;
            }

            // Find the primary action index (e.g., "Rub")
            int primaryActionIndex = -1;
            String[] actions = widget.getActions();
            if (actions != null) {
                for (int i = 0; i < actions.length; i++) {
                    if (matchesAction(primaryMenu, actions[i])) {
                        // CC_OP primary indices are 1-based
                        primaryActionIndex = i + 1;
                        break;
                    }
                }
            }

            if (primaryActionIndex == -1) {
                log.error("Failed to resolve primary action '{}' on widget: {}", primaryMenu, widget.getId());
                return null;
            }

            // 2. Find the sub-action index (e.g., "Fortis Colosseum")
            int subActionIndex = -1;
            ItemComposition composition = client.getItemDefinition(widget.getItemId());
            String[][] subOps = composition.getSubops();

            if (subOps != null) {
                for (String[] subOpArray : subOps) {
                    if (subActionIndex != -1) break;
                    if (subOpArray != null) {
                        for (int i = 0; i < subOpArray.length; i++) {
                            if (matchesAction(subActionName, subOpArray[i])) {
                                subActionIndex = i;
                                break;
                            }
                        }
                    }
                }
            }

            if (subActionIndex == -1) {
                log.error("Failed to resolve sub-action '{}' for item: {}", subActionName, widget.getItemId());
                return null;
            }

            // Pack the identifier: (primaryIndex << 16) | subActionIndex
            int identifier = (primaryActionIndex << 16) | subActionIndex;

            // Construct the sub-action menu option
            MenuOption option = new MenuOption(
                    MenuAction.CC_OP_LOW_PRIORITY,
                    identifier,
                    widget.getIndex(),             // Param0
                    widget.getId(),                // Param1
                    widget.getItemId(),
                    client.getTopLevelWorldView().getId()
            );

            return new ResolvedMenuAction(option, subActionName);
        });
    }

    private ResolvedMenuAction resolveBankInteraction(BankItemWidget item, String action) {
        if (action == null) return null;

        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();

            int identifier = -1;
            MenuAction menuAction = MenuAction.CC_OP;

            String[] actions = item.getActions();
            if (actions == null || actions.length == 0) {
                Widget parent = item.getParent();
                if (parent != null) {
                    actions = parent.getActions();
                }
            }

            // Try to match the action string to the array
            if (actions != null) {
                for (int i = 0; i < actions.length; i++) {
                    if (matchesAction(action, actions[i])) {
                        identifier = i + 1; // CC_OP indices are 1-based
                        break;
                    }
                }
            }

            // Fallback: Hardcode standard OSRS bank indices if array lookup fails
            if (identifier == -1) {
                String sanitized = Text.sanitize(action).toLowerCase();
                switch (sanitized) {
                    case "withdraw-1":
                        identifier = 2;
                        break;
                    case "withdraw-5":
                        identifier = 3;
                        break;
                    case "withdraw-10":
                        identifier = 4;
                        break;
                    case "withdraw-all":
                        identifier = 5;
                        break;
                    case "withdraw-x":
                        identifier = 6;
                        break;
                    case "withdraw-all-but-1":
                        identifier = 7;
                        break;
                    case "examine":
                        identifier = 8;
                        break;
                    default:
                        // If it's a dynamic left-click quantity like "Withdraw-50" that isn't in the standard list,
                        // it is the default left-click option (id = 1).
                        identifier = 1;
                        break;
                }
            }

            // Match the engine logs: Actions like Withdraw-X (id=6+) use CC_OP_LOW_PRIORITY
            if (identifier >= 6) {
                menuAction = MenuAction.CC_OP_LOW_PRIORITY;
            }

            // Construct the target string WITH colors, exactly as the game engine logs it
            String target = "";
            if (item.getItemId() != -1) {
                ItemComposition comp = client.getItemDefinition(item.getItemId());
                target = "<col=ff9040>" + comp.getName() + "</col>";
            }

            MenuOption option = new MenuOption(
                    menuAction,
                    identifier,
                    item.getIndex(),     // Param0: Slot in bank (e.g., 179)
                    item.getId(),        // Param1: Packed container ID (786444)
                    item.getItemId(),    // ItemId (e.g., 558)
                    client.getTopLevelWorldView().getId()
            );

            return new ResolvedMenuAction(option, target);
        });
    }

    /**
     * Interacts with a specific point on the game canvas using the provided action and resolved menu action.
     * This method sends the necessary packets to perform the interaction as defined by the resolved action.
     *
     * <p>
     * The {@code interact} method is a low-level interaction utility meant for internal use. It combines
     * mouse click simulation and menu invocation to interact with various in-game entities based on the
     * specified coordinates and parameters.
     *
     * @param point The {@code Point} representing the canvas coordinates (x, y) where the interaction should occur.
     *              This specifies the location of the screen to click.
     * @param action A {@code String} representing the menu action text, used as a descriptor for the interaction
     *               (e.g., "Attack", "Examine", "Walk here").
     * @param resolvedAction A {@code ResolvedMenuAction} object containing the pre-resolved {@link MenuOption}
     *                       and target for the interaction. This encapsulates the menu option parameters and the
     *                       target string for the operation.
     */
    private void interact(Point point, String action, ResolvedMenuAction resolvedAction) {
        if (point == null || resolvedAction == null) {
            return;
        }

        MenuOption option = resolvedAction.getOption();
        mousePackets.queueClickPacket(point.getX(), point.getY());
        log.info("interact: param0 = {}, param1 = {}, menu action = {}, id = {}, itemId = {}, wv = {}, action = {}, target = {}, x = {}, y = {}", option.getParam0(), option.getParam1(), option.getType().name(), option.getIdentifier(), option.getItemId(), option.getWorldView(), action, resolvedAction.getTarget(), point.getX(), point.getY());
        invokeMenu(
                option.getParam0(),
                option.getParam1(),
                option.getType().getId(),
                option.getIdentifier(),
                option.getItemId(),
                option.getWorldView(),
                action,
                resolvedAction.getTarget(),
                point.getX(),
                point.getY()
        );
    }

    /**
     * Interacts with an NPC using the specified action i.e. "Attack", "Talk-To", or "Examine".
     *
     * @param npc the NPC to interact with
     * @param action The action to take, "Attack", "Talk-To", or "Examine".
     */
    public void interact(NPC npc, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point point = UIService.getClickbox(npc);
        interact(point, action, resolveNpcInteraction(npc, action));
    }


    /**
     * Interacts with a Player using the specified action i.e. "Attack", "Trade", or "Follow"
     *
     * @param player the Player to interact with
     * @param action The action to take, "Attack", "Trade", or "Follow"
     */
    public void interact(Player player, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point point = UIService.getClickbox(player);
        interact(point, action, resolvePlayerInteraction(player, action));
    }

    /**
     * Interacts with an item with the specified ID in an item container (inventory, inventory while banking, equipment, etc...)
     * using the specified action.
     * <p>
     * @param item The Container Item to interact with. A container item is an item stored in a container like an inventory, a inventory while banking
     *             or the equipment interface.
     * @param action The action to take. i.e. "Eat", "Remove", "Wield", "Wear", or "Use"
     */
    public void interact(ContainerItem item, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        ctxProvider.get().runOnClientThread(() -> {
            if(item == null) return;

            Widget w = item.getWidget();
            if (w == null) return;

            Point pt = UIService.getClickbox(item);

            ResolvedMenuAction resolvedAction = resolveWidgetInteraction(item.getWidget(), action);
            if (resolvedAction != null) {
                mousePackets.queueClickPacket(pt.getX(), pt.getY());
                interact(pt, action, resolvedAction);
            }
        });
    }

    /**
     * Interacts with an item with the specified ID in an item container (inventory, inventory while banking, equipment, etc...)
     * using the first matching specified action. For example, passing "wield" and "wear" as actions would result in
     * wielding weapons and wearing armor when invoked on the given container item.
     *
     * <p>
     * @param item The Container Item to interact with. A container item is an item stored in a container like an inventory, a inventory while banking
     *             or the equipment interface.
     * @param actions A variable number of actions to take. i.e. "Eat", "Remove", "Wield", "Wear", or "Use" The first action which matches
     *                the list of actions on the container item will be used.
     */
    public void interact(ContainerItem item, String... actions) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        ctxProvider.get().runOnClientThread(() -> {
            if(item == null) return;

            Widget w = item.getWidget();
            if (w == null) {
                log.error("Failed to resolve widget for item interaction: {}", item.getName());
                return;
            }

            Point pt = UIService.getClickbox(item);

            for (String action : actions) {
                ResolvedMenuAction resolvedAction = resolveWidgetInteraction(w, action);

                // If we successfully resolved an action, queue it and exit the method
                if (resolvedAction != null) {
                    mousePackets.queueClickPacket(pt.getX(), pt.getY());
                    interact(pt, action, resolvedAction);
                    return;
                }
            }

            log.warn("Failed to resolve any of the specified actions {} for item: {}", java.util.Arrays.toString(actions), item.getName());
        });
    }

    /**
     * Interacts with a widget in the players bank using the specific action.
     * @param item The bank item widget to interact with
     * @param action The action to take i.e. Withdraw-1, Withdraw-X, Examine
     */
    public void interact(BankItemWidget item, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item);

        ResolvedMenuAction resolvedAction = resolveBankInteraction(item, action);
        if (resolvedAction != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            interact(pt, action, resolvedAction);
        }
    }

    /**
     * Interacts with a widget using the specific action.
     * @param item The widget to interact with
     * @param action The action to take i.e. Wield, Use or Examine
     */
    public void interact(Widget item, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item);
        ResolvedMenuAction resolvedAction = resolveWidgetInteraction(item, action);
        if (resolvedAction != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            interact(pt, action, resolvedAction);
        }
    }

    /**
     * Interacts with a widget using the specific sub action.
     * @param item The widget to interact with
     * @param menu The primary menu to select (e.g., "Rub")
     * @param action The sub-action to take (e.g., "Fortis Colosseum")
     */
    public void interact(Widget item, String menu, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item);

        // TODO Doesn't work but is probably something simple
        ResolvedMenuAction resolvedAction = resolveWidgetSubAction(item, menu, action);
        if (resolvedAction != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            interact(pt, action, resolvedAction);
        }
    }

    /**
     * Uses a source widget on a destination widget (i.e. High Alchemy)
     * @param src The source widget to use on the destination widget
     * @param dest The destination widget
     */
    public void interact(Widget src, Widget dest) {
        if(!ctxProvider.get().isPacketsLoaded()) return;

        Point pt = UIService.getClickbox(src);
        Point destPoint = UIService.getClickbox(dest);

        ResolvedMenuAction resolvedAction = resolveWidgetInteraction(src, src.getTargetVerb());
        if(resolvedAction != null) {
            // TODO Test with HLA because that is a Cast -> Cast and this may only support "Use" like chiseling gems etc...
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            interact(pt, src.getTargetVerb(), resolvedAction);

             // now client.isWidgetSelected() will be true so the next resolve will be for WIDGET_TARGET_ON_WIDGET
            ResolvedMenuAction targetAction = resolveWidgetInteraction(dest, dest.getTargetVerb());
            if(targetAction != null) {
                mousePackets.queueClickPacket(destPoint.getX(), destPoint.getY());
                interact(destPoint, dest.getTargetVerb(), targetAction);
            }
        }
    }

    /**
     * Interacts with a widget using the specific action index
     * @param action The action index to take
     * @param packedWidgetId The packed widget id
     * @param childId The child id of the widget to interact with
     * @param itemId The item id of the widget to interact with
     */
    public void interact(int action, int packedWidgetId, int childId, int itemId) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(ctxProvider.get().widgets().get(packedWidgetId).raw());
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetActionPacket(packedWidgetId, childId, itemId, action);
    }

    /**
     * Uses a source widget on a destination NPC (i.e. Crumble Undead spell on Vorkath Spawn)
     * @param src The source widget to use on the destination widget
     * @param npc The NPC to use the widget on
     */
    public void interact(Widget src, NPC npc) {
        if (!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(src);
        Point npcPoint = UIService.getClickbox(npc);

        ResolvedMenuAction resolvedAction = resolveWidgetInteraction(src, src.getTargetVerb());
        if(resolvedAction != null) {
            // Resolve our own menu action because resolveWidgetInteraction doesn't support WIDGET_TARGET_ON_NPC (we know we are targeting an NPC in this)
            ResolvedMenuAction targetAction = ctxProvider.get().runOnClientThread(() -> {
                Client client = ctxProvider.get().getClient();
                MenuOption option = new MenuOption(
                        MenuAction.WIDGET_TARGET_ON_NPC,
                        npc.getIndex(),
                        0,
                        0,
                        -1,
                        client.getTopLevelWorldView().getId()
                );

                String npcName = npc.getName() == null ? "" : npc.getName();
                String target = resolvedAction.getTarget() + " -> " + npcName;
                return new ResolvedMenuAction(option, target);
            });

            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            interact(pt, src.getTargetVerb(), resolvedAction);
            mousePackets.queueClickPacket(npcPoint.getX(), npcPoint.getY());
            interact(npcPoint, src.getTargetVerb(), targetAction);
        }
    }

    /**
     * Uses a source widget on a destination Game Object (i.e. "Bones" on the "Chaos Altar")
     * @param src The source widget to use on the destination widget
     * @param object The Tile Object (Game Object) to use the widget on
     */
    public void interact(Widget src, TileObject object) {
        if (!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(src);
        Point gameObjectPoint = UIService.getClickbox(object);

        // Target verb will be: "Use", "Cast", etc... this basically ensures this action resolves to WIDGET_TARGET
        ResolvedMenuAction resolvedAction = resolveWidgetInteraction(src, src.getTargetVerb());
        if(resolvedAction != null) {

            Point p;
            if (object instanceof GameObject) {
                GameObject gameObject = (GameObject) object;
                p = gameObject.getSceneMinLocation();
            } else {
                p = new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY());
            }

            // Resolve our own menu action because resolveWidgetInteraction doesn't support WIDGET_TARGET_ON_GAME_OBJECT
            ResolvedMenuAction targetAction = ctxProvider.get().runOnClientThread(() -> {
                Client client = ctxProvider.get().getClient();
                MenuOption option = new MenuOption(
                        MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
                        object.getId(),
                        p.getX(),
                        p.getY(),
                        -1,
                        client.getTopLevelWorldView().getId()
                );

                String name;
                ObjectComposition composition = getObjectComposition(client, object);
                if(composition == null) {
                    name = "";
                } else {
                    name = composition.getName();
                }

                String target = resolvedAction.getTarget() + " -> " + name;
                return new ResolvedMenuAction(option, target);
            });

            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            interact(pt, src.getTargetVerb(), resolvedAction); // -> MenuAction=WIDGET_TARGET, ItemId=1925, id=0, Option=Use, Target=<col=ff9040>Bucket</col>
            mousePackets.queueClickPacket(gameObjectPoint.getX(), gameObjectPoint.getY());
            interact(gameObjectPoint, src.getTargetVerb(), targetAction); // -> MenuAction=WIDGET_TARGET_ON_GAME_OBJECT, ItemId=-1, id=5125, Option=Use, Target=<col=ff9040>Bucket</col><col=ffffff> -> <col=ffff>Fountain
        }
    }

    /**
     * Interacts with a GameObject ({@code TileObject}) using the specified action i.e. "Chop", "Mine", or "Examine".
     * GameObject's are objects that exist on a tile like walls, trees, ore, or fishing spots.
     *
     * @param object the {@code TileObject} to interact with
     * @param action The action to take on the game object, i.e. "Chop", "Mine", or "Examine".
     */
    public void interact(TileObject object, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(object);
        interact(pt, action, resolveTileObjectInteraction(object, action));
    }

    /**
     * Interacts with a ground item ({@code GroundItem}) using the specified action i.e. "Take" or "Examine". A
     * Ground item is an actual item that is on the ground like coins dropped from a boss or logs a player has
     * dropped on a tile. This differs from GameObjects like trees, ore, or fish which exist on a tile but are not
     * "takeable" into the players inventory.
     *
     * @param item the {@code GroundItem} to interact with
     */
    public void interact(GroundItem item) {
        interact(item, "Take");
    }

    public void interact(GroundItem item, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item.getTileObject());
        interact(pt, action, resolveGroundItemInteraction(item, action));
    }

    // -------------------------
    // Helper methods
    // -------------------------

    /**
     * Resolves the appropriate {@code MenuOption} based on a requested action and a list of available actions.
     * <p>
     * This method compares the {@code requestedAction} with each {@code availableActions} entry to find a match.
     * If a match is found, the corresponding {@code MenuOption} is generated using the provided {@code optionFactory}.
     * If no match is found or if {@code availableActions} is {@code null}, the method returns {@code null}.
     * </p>
     *
     * @param requestedAction The action requested by the user. This string is compared against the available actions
     *                        to find a match. Must not be {@code null}.
     * @param availableActions An array of actions to compare against the {@code requestedAction}.
     *                         It can be {@code null}, in which case the method will return {@code null}.
     * @param optionFactory A factory function to generate a {@code MenuOption} for the matching action's index in the
     *                      {@code availableActions} array. Must not be {@code null}.
     *
     * @return The {@code MenuOption} corresponding to the matched action, or {@code null} if no match is found or
     *         if {@code availableActions} is {@code null}.
     */
    private MenuOption resolveAction(String requestedAction, String[] availableActions, IntFunction<MenuOption> optionFactory) {
        if (availableActions == null) {
            return null;
        }

        for (int i = 0; i < availableActions.length; i++) {
            if (matchesAction(requestedAction, availableActions[i])) {
                return optionFactory.apply(i);
            }
        }

        return null;
    }

    /**
     * Determines if the requested action matches the candidate action after sanitization.
     *
     * <p>This method compares the {@code requestedAction} with a sanitized version of
     * {@code candidateAction} in a case-insensitive manner. Returns {@code true} if they match and
     * {@code false} otherwise. If {@code candidateAction} is {@code null}, the method returns {@code false}.
     *
     * @param requestedAction The action being requested. Must not be {@code null}.
     * @param candidateAction The action to compare against the requested action. Can be {@code null}.
     * @return {@code true} if the {@code requestedAction} matches the sanitized {@code candidateAction},
     *         ignoring case. {@code false} otherwise.
     */
    private boolean matchesAction(String requestedAction, String candidateAction) {
        return candidateAction != null && requestedAction.equalsIgnoreCase(Text.sanitize(candidateAction));
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
    private ObjectComposition getObjectComposition(Client client, TileObject object) {
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

    @Value
    public static class MenuOption {
        MenuAction type;
        int identifier;
        int param0;
        int param1;
        int itemId;
        int worldView;
    }

    @Value
    private static class ResolvedMenuAction {
        MenuOption option;
        String target;
    }
}
