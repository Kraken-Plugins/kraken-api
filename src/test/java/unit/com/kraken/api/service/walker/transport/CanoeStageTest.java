package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.CanoeStage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers reading how far along a canoe station is from the one action it offers.
 *
 * <p>The actions are the ones the game cache gives the fifteen forms a station takes: the standing
 * tree, the falling tree, the felled trunk, four shaped hulls and eight floating ones.</p>
 */
class CanoeStageTest {

    @Test
    void eachFormOfAStationIsReadAsItsStage() {
        assertEquals(CanoeStage.FELL, CanoeStage.of(List.of("Chop-down")));
        assertEquals(CanoeStage.SHAPE, CanoeStage.of(List.of("Shape-Canoe")));
        assertEquals(CanoeStage.FLOAT, CanoeStage.of(List.of("Float Log")));
        assertEquals(CanoeStage.FLOAT, CanoeStage.of(List.of("Float Canoe")));
        assertEquals(CanoeStage.PADDLE, CanoeStage.of(List.of("Paddle Log")));
        assertEquals(CanoeStage.PADDLE, CanoeStage.of(List.of("Paddle Canoe")));
    }

    @Test
    void aStationThatOffersNothingToDoIsBusy() {
        assertEquals(CanoeStage.BUSY, CanoeStage.of(List.of()), "the falling tree offers no action");
        assertEquals(CanoeStage.BUSY, CanoeStage.of(null));
        assertEquals(CanoeStage.BUSY, CanoeStage.of(List.of("Make-Canoe")),
                "the station's own action, which shows only while it has no form");
        assertEquals(CanoeStage.BUSY, CanoeStage.of(List.of("Examine")));
    }

    @Test
    void theActionIsReturnedAsTheStationSpellsIt() {
        assertEquals("Float Log", CanoeStage.FLOAT.actionIn(List.of("Float Log")));
        assertEquals("Paddle Canoe", CanoeStage.PADDLE.actionIn(Arrays.asList(null, "Examine", "Paddle Canoe")));
        assertEquals("chop-down", CanoeStage.FELL.actionIn(List.of("chop-down")), "case does not matter");
    }

    @Test
    void aStageFindsNoActionOnAStationAtAnotherStage() {
        assertNull(CanoeStage.FELL.actionIn(List.of("Shape-Canoe")));
        assertNull(CanoeStage.PADDLE.actionIn(List.of("Float Canoe")));
        assertNull(CanoeStage.SHAPE.actionIn(null));
        assertNull(CanoeStage.BUSY.actionIn(List.of("Chop-down")), "there is nothing to click while it is busy");
    }
}
