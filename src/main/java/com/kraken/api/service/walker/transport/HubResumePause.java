package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.ui.UIService;

/**
 * The destination list spirit trees and minecarts open after {@code Travel}.
 *
 * <p>The dataset writes those stops as numbered chat entries ({@code "4: Grand Exchange"}), but the
 * live list is not {@code DialogueService}'s chat group. VitaLite waits for packed widget
 * {@code 12255235} (group 187, child 3) and selects with resume-pause using a 0-based index. The
 * dataset's position is 1-based against the same list, so Grand Exchange is position 4 and index 3.</p>
 */
public final class HubResumePause {

    /**
     * Packed widget VitaLite waits on after Travel. Group 187, child 3.
     *
     * <p>Kept as the packed id VitaLite uses rather than a copied {@code InterfaceID} name, because
     * this list is not the client's current chat-option group.</p>
     */
    public static final int LIST = UIService.pack(187, 3);

    /** Returned when display info carries no menu position. */
    public static final int NO_INDEX = -1;

    private HubResumePause() {
    }

    /**
     * The 0-based resume-pause index for a numbered hub destination.
     *
     * @param displayInfo the parsed destination, may be null
     * @return the index, or {@link #NO_INDEX} when there is no position to send
     */
    public static int optionIndex(DisplayInfo displayInfo) {
        if (displayInfo == null || !displayInfo.hasPosition()) {
            return NO_INDEX;
        }

        int index = displayInfo.getPosition() - 1;
        return index >= 0 ? index : NO_INDEX;
    }

    /**
     * Reports whether the destination list is on screen.
     *
     * @param ctx the API context
     * @return true when the list widget is present
     */
    public static boolean isOpen(Context ctx) {
        if (ctx == null) {
            return false;
        }

        WidgetEntity widget = ctx.widgets().get(LIST);
        return widget != null && widget.isPresent();
    }
}
