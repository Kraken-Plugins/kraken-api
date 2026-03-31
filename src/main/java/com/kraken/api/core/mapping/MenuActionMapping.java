package com.kraken.api.core.mapping;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * A mapping of RuneLite MenuActions to their internal Engine Action IDs
 * and corresponding PacketUtils network packet names.
 */
@Getter
@AllArgsConstructor
public enum MenuActionMapping {

    // --- Targeted Actions (Using something on something) ---
    WIDGET_TARGET_ON_GAME_OBJECT(2, "OPLOCT", "Using an item/spell on a scenery object"),
    WIDGET_TARGET_ON_NPC(8, "OPNPCT", "Using an item/spell on an NPC"),
    WIDGET_TARGET_ON_PLAYER(15, "OPPLAYERT", "Using an item/spell on a Player"),
    WIDGET_TARGET_ON_GROUND_ITEM(17, "OPOBJT", "Using an item/spell on an item on the ground"),
    WIDGET_TARGET_ON_WIDGET(58, "IF_BUTTONT", "Using an item/spell on another interface item (e.g., Alching)"),

    // --- Player Actions (Right-clicking a player) ---
    PLAYER_FIRST_OPTION(44, "OPPLAYER1", "Player option 1 (e.g., Attack)"),
    PLAYER_SECOND_OPTION(45, "OPPLAYER2", "Player option 2 (e.g., Follow)"),
    PLAYER_THIRD_OPTION(46, "OPPLAYER3", "Player option 3 (e.g., Trade)"),
    PLAYER_FOURTH_OPTION(47, "OPPLAYER4", "Player option 4"),
    PLAYER_FIFTH_OPTION(48, "OPPLAYER5", "Player option 5"),
    PLAYER_SIXTH_OPTION(49, "OPPLAYER6", "Player option 6"),
    PLAYER_SEVENTH_OPTION(50, "OPPLAYER7", "Player option 7"),
    PLAYER_EIGHTH_OPTION(51, "OPPLAYER8", "Player option 8"),

    // --- Game Object Actions (Clicking scenery) ---
    GAME_OBJECT_FIRST_OPTION(3, "OPLOC1", "Game object option 1 (e.g., Chop down Tree)"),
    GAME_OBJECT_SECOND_OPTION(4, "OPLOC2", "Game object option 2"),
    GAME_OBJECT_THIRD_OPTION(5, "OPLOC3", "Game object option 3"),
    GAME_OBJECT_FOURTH_OPTION(6, "OPLOC4", "Game object option 4"),
    GAME_OBJECT_FIFTH_OPTION(1001, "OPLOC5", "Game object option 5"),

    // --- NPC Actions (Clicking NPCs) ---
    NPC_FIRST_OPTION(9, "OPNPC1", "NPC option 1 (e.g., Attack)"),
    NPC_SECOND_OPTION(10, "OPNPC2", "NPC option 2 (e.g., Pickpocket)"),
    NPC_THIRD_OPTION(11, "OPNPC3", "NPC option 3"),
    NPC_FOURTH_OPTION(12, "OPNPC4", "NPC option 4"),
    NPC_FIFTH_OPTION(13, "OPNPC5", "NPC option 5"),

    // --- Ground Item Actions (Clicking items on the floor) ---
    GROUND_ITEM_FIRST_OPTION(18, "OPOBJ1", "Ground item option 1 (e.g., Take)"),
    GROUND_ITEM_SECOND_OPTION(19, "OPOBJ2", "Ground item option 2"),
    GROUND_ITEM_THIRD_OPTION(20, "OPOBJ3", "Ground item option 3"),
    GROUND_ITEM_FOURTH_OPTION(21, "OPOBJ4", "Ground item option 4"),
    GROUND_ITEM_FIFTH_OPTION(22, "OPOBJ5", "Ground item option 5"),

    // --- Movement & Interface Actions ---
    WALK(82, "MOVE_GAMECLICK", "Standard scene click for movement"),
    CC_OP(57, "IF_BUTTONX", "Standard interface click (e.g., clicking a prayer, dropping an item)"),
    UNKNOWN(-1, "UNKNOWN", "Unknown packet type");

    private final int actionId;
    private final String packetName;
    private final String description;

    // Static lookup map for O(1) retrieval by Action ID
    private static final Map<Integer, MenuActionMapping> ID_LOOKUP = new HashMap<>();

    static {
        for (MenuActionMapping mapping : values()) {
            ID_LOOKUP.put(mapping.getActionId(), mapping);
        }
    }

    /**
     * Finds the corresponding mapping based on the internal engine Action ID.
     * Useful for ASM bytecode analysis.
     *
     * @param id The integer opcode found in the doAction method.
     * @return The MenuActionMapping, or null if the ID is not mapped.
     */
    public static MenuActionMapping fromId(int id) {
        MenuActionMapping mapping = ID_LOOKUP.get(id);
        if(mapping == null) {
            return MenuActionMapping.UNKNOWN;
        }
        return mapping;
    }
}
