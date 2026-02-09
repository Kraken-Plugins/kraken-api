package com.kraken.api.core.packet.model;

import net.runelite.api.MenuAction;

import java.util.List;

/**
 * Enum containing references to various packet types sent by the game client.
 * <p>
 * - IF Button types are sent when any of the normal buttons on newer interfaces are clicked
 * - Resume pause are sent when a player interacts with a Dialogue like talking to an NPC "Click here to Continue"
 * - Resume counts are sent when the dialogue asks for a number like in clue scroll steps
 * - Resume string is sent when the dialogue asks for a string like "whats your clan name"?
 * - Move game click is the packet that the client sends upon clicking on a game square to move towards it.
 * - Event Mouse click packets are written whenever the player clicks anywhere on their client, whether it be dead space, or any entity in-game
 */
public enum PacketType {
    OPHELDD,
    RESUME_COUNTDIALOG,
    RESUME_PAUSEBUTTON,
    RESUME_NAMEDIALOG,
    RESUME_STRINGDIALOG,
    RESUME_OBJDIALOG,
    IF_BUTTON,
    IF_SUBOP,
    IF_BUTTONX,
    OPNPC,
    OPPLAYER,
    OPOBJ,
    OPLOC,
    MOVE_GAMECLICK,
    EVENT_MOUSE_CLICK,
    IF_BUTTONT,
    OPNPCT,
    OPPLAYERT,
    OPOBJT,
    OPLOCT,
    SET_HEADING;

    /**
     * Retrieves a list of parameter names required for the current {@literal @}PacketType.
     * <p>
     * The specific parameters returned depend on the value of the {@literal @}PacketType
     * associated with this packet definition. Each {@literal @}PacketType corresponds
     * to a particular game action or event and requires different parameters.
     * <p>
     * For example:
     * <ul>
     *     <li>{@literal @}PacketType.RESUME_NAMEDIALOG or {@literal @}PacketType.RESUME_STRINGDIALOG require: "length", "string".</li>
     *     <li>{@literal @}PacketType.OPHELDD requires: "selectedId", "selectedChildIndex", "selectedItemId", "destId",
     *         "destChildIndex", "destItemId".</li>
     *     <li>{@literal @}PacketType.MOVE_GAMECLICK requires: "worldPointX", "worldPointY", "ctrlDown", "5".</li>
     * </ul>
     * <p>
     * Other {@literal @}PacketType values will similarly yield different parameter lists based on their associated requirements.
     * If no matching {@literal @}PacketType is set, the method will return {@code null}.
     *
     * @return A {@literal List<String>} containing the parameter names for the current {@literal @}PacketType,
     *         or {@code null} if no parameters are defined for the current type.
     */
    public List<String> getParams() {
        List<String> params = null;
        if (this == PacketType.RESUME_NAMEDIALOG || this == PacketType.RESUME_STRINGDIALOG) {
            params = List.of("length", "string");
        }
        if (this == PacketType.OPHELDD) {
            params = List.of("selectedId", "selectedChildIndex", "selectedItemId", "destId", "destChildIndex", "destItemId");
        }
        if (this == PacketType.RESUME_COUNTDIALOG || this == PacketType.RESUME_OBJDIALOG) {
            params = List.of("var0");
        }
        if (this == PacketType.RESUME_PAUSEBUTTON) {
            params = List.of("var0", "var1");
        }
        if (this == PacketType.IF_BUTTON) {
            params = List.of("widgetId", "slot", "itemId");
        }
        if (this == PacketType.IF_SUBOP) {
            params = List.of("widgetId", "slot", "itemId", "menuIndex", "subActionIndex");
        }
        if (this == PacketType.IF_BUTTONX) {
            params = List.of("widgetId", "slot", "itemId", "opCode");
        }
        if (this == PacketType.OPLOC) {
            params = List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (this == PacketType.OPNPC) {
            params = List.of("npcIndex", "ctrlDown");
        }
        if (this == PacketType.OPPLAYER) {
            params = List.of("playerIndex", "ctrlDown");
        }
        if (this == PacketType.OPOBJ) {
            params = List.of("objectId", "worldPointX", "worldPointY", "ctrlDown");
        }
        if (this == PacketType.OPOBJT) {
            params = List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId",
                    "ctrlDown");
        }
        if (this == PacketType.EVENT_MOUSE_CLICK) {
            params = List.of("mouseInfo", "mouseX", "mouseY", "0");
        }
        if (this == PacketType.MOVE_GAMECLICK) {
            params = List.of("worldPointX", "worldPointY", "ctrlDown", "5");
        }
        if (this == PacketType.IF_BUTTONT) {
            params = List.of("sourceWidgetId", "sourceSlot", "sourceItemId", "destinationWidgetId",
                    "destinationSlot", "destinationItemId");
        }
        if (this == PacketType.OPLOCT) {
            params = List.of("objectId", "worldPointX", "worldPointY", "slot", "itemId", "widgetId",
                    "ctrlDown");
        }
        if (this == PacketType.OPPLAYERT) {
            params = List.of("playerIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }
        if (this == PacketType.OPNPCT) {
            params = List.of("npcIndex", "itemId", "slot", "widgetId", "ctrlDown");
        }
        if (this == PacketType.SET_HEADING) {
            params = List.of("direction");
        }

        return params;
    }

    /**
     * Makes a best effort approach to mapping a RuneLite menu action for a game click to the underlying
     * packet this that is sent to the server
     * @param action RuneLite's menu action
     * @return PacketType
     */
    public static PacketType forMenuAction(MenuAction action) {
        switch (action) {
            // Game Object interactions (OPLOC)
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case GAME_OBJECT_FOURTH_OPTION:
            case GAME_OBJECT_FIFTH_OPTION:
            case EXAMINE_OBJECT:
                return OPLOC;

            // Game Object with target (OPLOCT)
            case WIDGET_TARGET_ON_GAME_OBJECT:
            case ITEM_USE_ON_GAME_OBJECT:
                return OPLOCT;

            // NPC interactions (OPNPC)
            case NPC_FIRST_OPTION:
            case NPC_SECOND_OPTION:
            case NPC_THIRD_OPTION:
            case NPC_FOURTH_OPTION:
            case NPC_FIFTH_OPTION:
            case EXAMINE_NPC:
                return OPNPC;

            // NPC with target (OPNPCT)
            case WIDGET_TARGET_ON_NPC:
            case ITEM_USE_ON_NPC:
                return OPNPCT;

            // Ground Item interactions (OPOBJ)
            case GROUND_ITEM_FIRST_OPTION:
            case GROUND_ITEM_SECOND_OPTION:
            case GROUND_ITEM_THIRD_OPTION:
            case GROUND_ITEM_FOURTH_OPTION:
            case GROUND_ITEM_FIFTH_OPTION:
            case EXAMINE_ITEM_GROUND:
                return OPOBJ;

            // Ground Item with target (OPOBJT)
            case WIDGET_TARGET_ON_GROUND_ITEM:
            case ITEM_USE_ON_GROUND_ITEM:
                return OPOBJT;

            // Player interactions (OPPLAYER)
            case PLAYER_FIRST_OPTION:
            case PLAYER_SECOND_OPTION:
            case PLAYER_THIRD_OPTION:
            case PLAYER_FOURTH_OPTION:
            case PLAYER_FIFTH_OPTION:
            case PLAYER_SIXTH_OPTION:
            case PLAYER_SEVENTH_OPTION:
            case PLAYER_EIGHTH_OPTION:
                return OPPLAYER;

            // Player with target (OPPLAYERT)
            case WIDGET_TARGET_ON_PLAYER:
            case ITEM_USE_ON_PLAYER:
                return OPPLAYERT;

            // Widget/Interface interactions (IF_BUTTON)
            case WIDGET_FIRST_OPTION:
            case WIDGET_SECOND_OPTION:
            case WIDGET_THIRD_OPTION:
            case WIDGET_FOURTH_OPTION:
            case WIDGET_FIFTH_OPTION:
            case WIDGET_TYPE_1:
            case WIDGET_TYPE_4:
            case WIDGET_TYPE_5:
            case WIDGET_CLOSE:
            case CC_OP:
            case CC_OP_LOW_PRIORITY:
                return IF_BUTTON;

            // Widget targeting (IF_BUTTONT)
            case WIDGET_TARGET:
            case WIDGET_TARGET_ON_WIDGET:
                return IF_BUTTONT;

            // Dialog continue (could be RESUME_PAUSEBUTTON)
            case WIDGET_CONTINUE:
                return RESUME_PAUSEBUTTON;

            // Item interactions (OPHELDD for item-on-item)
            case ITEM_USE_ON_ITEM:
            case WIDGET_USE_ON_ITEM:
            case ITEM_FIRST_OPTION:
            case ITEM_SECOND_OPTION:
            case ITEM_THIRD_OPTION:
            case ITEM_FOURTH_OPTION:
            case ITEM_FIFTH_OPTION:
            case ITEM_USE:
            case EXAMINE_ITEM:
                return OPHELDD;

            // Walking (MOVE_GAMECLICK)
            case WALK:
                return MOVE_GAMECLICK;

            // Set heading
            case SET_HEADING:
                return SET_HEADING;

            // World Entity interactions (likely OPLOC)
            case WORLD_ENTITY_FIRST_OPTION:
            case WORLD_ENTITY_SECOND_OPTION:
            case WORLD_ENTITY_THIRD_OPTION:
            case WORLD_ENTITY_FOURTH_OPTION:
            case WORLD_ENTITY_FIFTH_OPTION:
            case EXAMINE_WORLD_ENTITY:
                return OPLOC; // World entities are similar to game objects

            // RuneLite specific actions - no packet sent
            case RUNELITE:
            case RUNELITE_HIGH_PRIORITY:
            case RUNELITE_LOW_PRIORITY:
            case RUNELITE_OVERLAY:
            case RUNELITE_OVERLAY_CONFIG:
            case RUNELITE_PLAYER:
            case RUNELITE_INFOBOX:
            case RUNELITE_WIDGET:
            case CANCEL:
            case UNKNOWN:
            default:
                return null; // No packet for client-side only actions
        }
    }
}
