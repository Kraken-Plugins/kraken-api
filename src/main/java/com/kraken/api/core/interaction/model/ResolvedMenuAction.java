package com.kraken.api.core.interaction.model;

import lombok.Value;

/**
 * A menu option paired with the target text the client writes for it, ready to be dispatched.
 */
@Value
public class ResolvedMenuAction {
    /** The menu option to dispatch. */
    MenuOption option;
    /** The entity name written into the menu entry, or an empty string when there is none. */
    String target;
}
