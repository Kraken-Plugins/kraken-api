package com.kraken.api.query;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.*;
import com.kraken.api.core.packet.model.PacketFactory;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.bank.BankItemWidget;
import com.kraken.api.query.groundobject.GroundItem;
import com.kraken.api.service.ui.UIService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
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
    private void invokeMenu(int param0, int param1, int opcode, int identifier, int itemId, String option, String target, int canvasX, int canvasY) {
        invokeMenu(param0, param1, opcode, identifier, itemId, -1, option, target, canvasX, canvasY);
    }

    @SneakyThrows
    private void invokeMenu(int param0, int param1, int opcode, int identifier, int itemId, String option, String target) {
        invokeMenu(param0, param1, opcode, identifier, itemId, -1, option, target, -1, -1);
    }

    @SneakyThrows
    private void invokeMenu(int param0, int param1, int opcode, int identifier, int itemId, int worldViewId, String option, String target, int canvasX, int canvasY) {
        Context ctx = RuneLite.getInjector().getInstance(Context.class);
        Client client = ctx.getClient();

        // TODO Hasn't been released yet
        // int garbageValue = Integer.parseInt(PacketFactory.getPacketMetadata().getDoActionGarbageValue());
        int garbageValue = -1948098697;
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
        Point scenePoint = getScenePoint(object);
        if (scenePoint == null) {
            return null;
        }

        if (client.isWidgetSelected() && op.equalsIgnoreCase("Use")) {
            return new MenuOption(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, object.getId(), scenePoint.getX(), scenePoint.getY(), -1, worldView);
        }

        ObjectComposition composition = getObjectComposition(client, object);
        if (composition == null) {
            return null;
        }

        return resolveAction(op, composition.getActions(), i -> i < GAME_OBJECT_ACTIONS.length
                ? new MenuOption(GAME_OBJECT_ACTIONS[i], object.getId(), scenePoint.getX(), scenePoint.getY(), -1, worldView)
                : null);
    }

    private MenuOption resolve(Client client, GroundItem item, String op, int worldView) {
        LocalPoint point = getGroundItemLocalPoint(client, item);
        if (point == null) {
            return null;
        }

        if (client.isWidgetSelected() && op.equalsIgnoreCase("Use")) {
            return new MenuOption(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM, item.getTileItem().getId(), point.getSceneX(), point.getSceneY(), -1, worldView);
        }

        return resolveAction(op, GroundItem.getGroundItemActions(item.getItemComposition()), i -> i < GROUND_ITEM_ACTIONS.length
                ? new MenuOption(GROUND_ITEM_ACTIONS[i], item.getTileItem().getId(), point.getSceneX(), point.getSceneY(), -1, worldView)
                : null);
    }

    private MenuOption resolve(Client client, Widget widget, String op, int worldView) {
        if (widget == null || !widget.isIf3()) {
            return null;
        }

        if (client.isWidgetSelected() && op.equalsIgnoreCase("Use")) {
            int events = widget.getClickMask();
            boolean targetable = (events >> 21 & 0x1) != 0;
            if (targetable) {
                return new MenuOption(MenuAction.WIDGET_TARGET_ON_WIDGET, 0, widget.getIndex(), widget.getId(), widget.getItemId(), worldView);
            }
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

        if ((widget.getClickMask() & 0x1) != 0 && matchesAction(op, "Continue")) {
            return new MenuOption(MenuAction.WIDGET_CONTINUE, 0, widget.getIndex(), widget.getId(), widget.getItemId(), worldView);
        }

        return null;
    }

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

    private boolean matchesAction(String requestedAction, String candidateAction) {
        return candidateAction != null && requestedAction.equalsIgnoreCase(Text.sanitize(candidateAction));
    }

    private Point getScenePoint(TileObject object) {
        if (object instanceof GameObject) {
            return ((GameObject) object).getSceneMinLocation();
        }

        LocalPoint localPoint = object.getLocalLocation();
        if (localPoint == null) {
            return null;
        }

        return new Point(localPoint.getSceneX(), localPoint.getSceneY());
    }

    private LocalPoint getGroundItemLocalPoint(Client client, GroundItem item) {
        TileObject tileObject = item.getTileObject();
        if (tileObject != null) {
            return tileObject.getLocalLocation();
        }

        return LocalPoint.fromWorld(client.getTopLevelWorldView(), item.getLocation());
    }

    private ObjectComposition getObjectComposition(Client client, TileObject object) {
        ObjectComposition composition = client.getObjectDefinition(object.getId());
        if (composition == null) {
            return null;
        }

        ObjectComposition transformed = composition.getImpostor();
        return transformed != null ? transformed : composition;
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
                return null;
            }

            return new ResolvedMenuAction(option, item.getName() == null ? "" : item.getName());
        });
    }

    private void interact(Point point, String action, ResolvedMenuAction resolvedAction) {
        if (point == null || resolvedAction == null) {
            return;
        }

        MenuOption option = resolvedAction.getOption();
        mousePackets.queueClickPacket(point.getX(), point.getY());
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
            if (w == null) {
                log.error("Failed to resolve widget for item interaction: {}", item.getName());
                return;
            }

            Point pt = UIService.getClickbox(item);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(w, action);
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
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(w, actions);
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

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item, action);
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
        interact(pt, action, resolveWidgetInteraction(item, action));
    }

    /**
     * Interacts with a widget using the specific sub action.
     * @param item The widget to interact with
     * @param menu The menu to select
     * @param action The action to take i.e. Wield, Use or Examine
     */
    public void interact(Widget item, String menu, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetSubAction(item, menu, action);
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

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            mousePackets.queueClickPacket(destPoint.getX(), destPoint.getY());
            widgetPackets.queueWidgetOnWidget(src, dest);
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
        if(!ctxProvider.get().isPacketsLoaded()) return;

        Point pt = UIService.getClickbox(src);
        Point npcPoint = UIService.getClickbox(npc);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            mousePackets.queueClickPacket(npcPoint.getX(), npcPoint.getY());
            npcPackets.queueWidgetOnNPC(npc, src);
        }
    }

    /**
     * Uses a source widget on a destination Game Object (i.e. "Bones" on the "Chaos Altar")
     * @param src The source widget to use on the destination widget
     * @param gameObject The Game Object to use the widget on
     */
    public void interact(Widget src, GameObject gameObject) {
        if(!ctxProvider.get().isPacketsLoaded()) return;

        Point pt = UIService.getClickbox(src);
        Point gameObjectPoint = UIService.getClickbox(gameObject);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            mousePackets.queueClickPacket(gameObjectPoint.getX(), gameObjectPoint.getY());
            gameObjectPackets.queueWidgetOnTileObject(src, gameObject);
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

    @Getter
    @AllArgsConstructor
    public static class MenuOption {
        private final MenuAction type;
        private final int identifier;
        private final int param0;
        private final int param1;
        private final int itemId;
        private final int worldView;
    }

    @Getter
    @AllArgsConstructor
    private static class ResolvedMenuAction {
        private final MenuOption option;
        private final String target;
    }
}
