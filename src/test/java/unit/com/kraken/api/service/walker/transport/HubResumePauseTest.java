package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.HubResumePause;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers turning a numbered hub destination into the resume-pause index VitaLite sends.
 *
 * <p>The dataset writes {@code "4: Grand Exchange"} as a 1-based menu entry. The packed list widget
 * is selected with a 0-based index, so Grand Exchange is 3 — the same value VitaLite's
 * {@code SpiritTree.GRAND_EXCHANGE} uses.</p>
 */
class HubResumePauseTest {

    @Test
    void theListWidgetIsThePackedIdVitaliteWaitsOn() {
        assertEquals(12255235, HubResumePause.LIST);
    }

    @Test
    void grandExchangeIsIndexThree() {
        assertEquals(3, HubResumePause.optionIndex(DisplayInfo.parse("4: Grand Exchange")));
    }

    @Test
    void prifddinasIsIndexFive() {
        assertEquals(5, HubResumePause.optionIndex(DisplayInfo.parse("6: Prifddinas")));
    }

    @Test
    void aBareNameHasNoIndex() {
        assertEquals(HubResumePause.NO_INDEX, HubResumePause.optionIndex(DisplayInfo.parse("Grand Exchange")));
    }

    @Test
    void nullYieldsNoIndex() {
        assertEquals(HubResumePause.NO_INDEX, HubResumePause.optionIndex(null));
    }
}
