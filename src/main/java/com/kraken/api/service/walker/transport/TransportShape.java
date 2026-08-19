package com.kraken.api.service.walker.transport;

/**
 * How a transport is operated, as opposed to what it is.
 *
 * <p>The dataset distinguishes twenty six kinds of transport, but they are worked in far fewer ways.
 * Grouping by mechanism is what keeps the number of handlers small: a gate, a lever and a portal are
 * all one click on something the dataset names.</p>
 */
public enum TransportShape {

    /** One menu action on an object or NPC found from the transport's object info. */
    SINGLE_CLICK,

    /** A click that opens a conversation, where the destination or a confirmation is chosen. */
    CLICK_THEN_DIALOGUE,

    /** A click that opens a dedicated interface listing destinations. */
    CLICK_THEN_WIDGET,

    /** A click that offers its destinations as a numbered list of chat options. */
    HUB_DIALOGUE,

    /**
     * A click that opens a numbered destination list selected with resume-pause, not chat options.
     *
     * <p>Spirit trees and minecarts look like {@code "4: Grand Exchange"} in the dataset, but the live
     * list is a dedicated widget rather than {@code DialogueService}'s chat group.</p>
     */
    HUB_RESUME_PAUSE,

    /** A fairy ring, selected by its three letter code. */
    FAIRY_RING,

    /** An item in the inventory or equipment, used through one of its menu actions. */
    ITEM_SUBOP,

    /** A teleport spell cast from the spellbook. */
    SPELL,

    /** The grouping tab's minigame teleport. */
    GROUPING_TELEPORT,

    /** A canoe, which has to be felled and shaped before it can be sailed. */
    CANOE
}
