package com.kraken.api.core.interaction.model;

import lombok.Value;
import net.runelite.api.MenuAction;

/**
 * One menu entry to dispatch to the client: the menu action type and the values the client's
 * menu handler reads from it.
 */
@Value
public class MenuOption {
    /** The menu action type the client should perform. */
    MenuAction type;
    /** The entity or operation identifier: an NPC index, player id, object or item id, or a 1-based widget operation index. */
    int identifier;
    /** The first parameter: a scene x coordinate for world entities, or the child index for widget operations. */
    int param0;
    /** The second parameter: a scene y coordinate for world entities, or the widget id for widget operations. */
    int param1;
    /** The item id involved in the action, or -1 when no item is involved. */
    int itemId;
    /** The id of the world view the entity belongs to. */
    int worldView;
}
