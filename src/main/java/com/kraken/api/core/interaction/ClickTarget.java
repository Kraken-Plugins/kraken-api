package com.kraken.api.core.interaction;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;

/**
 * What a Kraken click is about to land on, handed to a {@link ClickPointSampler} so it can choose a point that suits
 * the target. Widget targets carry the packed widget id, the dynamic child index and the item id the menu entry would
 * carry, so a sampler can recognise a prayer button, the quick prayer orb or an inventory slot.
 */
@Value
@With
@Builder(toBuilder = true)
public class ClickTarget {

    /**
     * The kind of thing being clicked.
     */
    public enum Kind {
        /** An interface component: a prayer button, an orb, a side tab, a spell. */
        WIDGET,
        /** An item in a container interface such as the inventory or the bank. */
        CONTAINER_ITEM,
        /** A non-player character in the scene. */
        NPC,
        /** Another player in the scene. */
        PLAYER,
        /** A game, wall, decorative or ground object in the scene. */
        OBJECT,
        /** An item on the ground. */
        GROUND_ITEM,
        /** Anything else. */
        OTHER
    }

    /**
     * The kind of thing being clicked.
     */
    Kind kind;

    /**
     * The packed widget id ({@code group << 16 | child}) for widget and container item targets, otherwise -1.
     */
    @Builder.Default
    int widgetId = -1;

    /**
     * The dynamic child index inside the widget (an inventory slot), or -1.
     */
    @Builder.Default
    int childIndex = -1;

    /**
     * The item id involved, or -1.
     */
    @Builder.Default
    int itemId = -1;

    /**
     * The NPC, object or ground item id for scene targets, or -1.
     */
    @Builder.Default
    int entityId = -1;

    /**
     * The menu action about to be performed, such as "Activate", or null when not known yet.
     */
    String action;

    /**
     * Where the previous click from any source landed, for samplers that shift the point toward the side the hand
     * comes from. {@link ClickPoints} fills it in; null when no click has been made yet.
     */
    Point previousClick;

    /**
     * @param widget the widget about to be clicked.
     * @param action the menu action, or null.
     * @return a widget target.
     */
    public static ClickTarget widget(Widget widget, String action) {
        return builder()
                .kind(Kind.WIDGET)
                .widgetId(widget.getId())
                .childIndex(widget.getIndex())
                .itemId(widget.getItemId())
                .action(action)
                .build();
    }

    /**
     * @param widgetId   the packed widget id.
     * @param childIndex the dynamic child index, or -1.
     * @param itemId     the item id, or -1.
     * @param action     the menu action, or null.
     * @return a widget target.
     */
    public static ClickTarget widget(int widgetId, int childIndex, int itemId, String action) {
        return builder()
                .kind(Kind.WIDGET)
                .widgetId(widgetId)
                .childIndex(childIndex)
                .itemId(itemId)
                .action(action)
                .build();
    }

    /**
     * @param widget the item's widget in its container.
     * @param itemId the item id.
     * @param action the menu action, or null.
     * @return a container item target.
     */
    public static ClickTarget containerItem(Widget widget, int itemId, String action) {
        return builder()
                .kind(Kind.CONTAINER_ITEM)
                .widgetId(widget.getId())
                .childIndex(widget.getIndex())
                .itemId(itemId)
                .action(action)
                .build();
    }

    /**
     * @param npc    the NPC about to be clicked.
     * @param action the menu action, or null.
     * @return an NPC target.
     */
    public static ClickTarget npc(NPC npc, String action) {
        return builder().kind(Kind.NPC).entityId(npc.getId()).action(action).build();
    }

    /**
     * @param player the player about to be clicked.
     * @param action the menu action, or null.
     * @return a player target.
     */
    public static ClickTarget player(Player player, String action) {
        return builder().kind(Kind.PLAYER).entityId(player.getId()).action(action).build();
    }

    /**
     * @param object the object about to be clicked.
     * @param action the menu action, or null.
     * @return an object target.
     */
    public static ClickTarget object(TileObject object, String action) {
        return builder().kind(Kind.OBJECT).entityId(object.getId()).action(action).build();
    }

    /**
     * @param itemId the ground item's id.
     * @param action the menu action, or null.
     * @return a ground item target.
     */
    public static ClickTarget groundItem(int itemId, String action) {
        return builder().kind(Kind.GROUND_ITEM).entityId(itemId).itemId(itemId).action(action).build();
    }
}
