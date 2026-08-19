package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.handler.HubDialogueHandler;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers picking a hub destination from a numbered chat list.
 *
 * <p>The dataset's position is one-based against the title-stripped option list, not a widget child
 * index. Sending that number as {@code selectOption(int)} is what this chooser exists to avoid.</p>
 */
class HubDialogueChooserTest {

    private static final List<String> SPIRIT_TREE = Arrays.asList(
            "1: Tree Gnome Village",
            "2: Gnome Stronghold",
            "3: Battlefield of Khazard",
            "4: Grand Exchange",
            "5: Feldip Hills",
            "6: Prifddinas",
            "7: Port Tyras"
    );

    @Test
    void aLabelMatchIsPreferred() {
        DisplayInfo info = DisplayInfo.parse("6: Prifddinas");

        assertEquals("6: Prifddinas", HubDialogueHandler.chooseOption(SPIRIT_TREE, info));
    }

    @Test
    void aPartialLabelStillMatches() {
        DisplayInfo info = DisplayInfo.parse("Prifddinas");

        assertEquals("6: Prifddinas", HubDialogueHandler.chooseOption(SPIRIT_TREE, info));
    }

    @Test
    void positionIsUsedWhenTheLabelDoesNotMatch() {
        DisplayInfo info = DisplayInfo.parse("6: Nowhere");

        assertEquals("6: Prifddinas", HubDialogueHandler.chooseOption(SPIRIT_TREE, info));
    }

    @Test
    void anUnknownLabelAndPositionYieldsNothing() {
        DisplayInfo info = DisplayInfo.parse("9: Nowhere");

        assertNull(HubDialogueHandler.chooseOption(SPIRIT_TREE, info));
    }

    @Test
    void nullsYieldNothing() {
        assertNull(HubDialogueHandler.chooseOption(null, DisplayInfo.parse("6: Prifddinas")));
        assertNull(HubDialogueHandler.chooseOption(SPIRIT_TREE, null));
        assertNull(HubDialogueHandler.chooseOption(Collections.emptyList(), DisplayInfo.parse("6: Prifddinas")));
    }
}
