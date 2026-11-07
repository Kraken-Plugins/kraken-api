package com.kraken.api.core.interaction;

import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.input.mouse.MousePresses;
import net.runelite.api.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClickHistoryTest {

    private final MousePackets mousePackets = mock(MousePackets.class);
    private final MousePresses mousePresses = new MousePresses();
    private final ClickHistory history = new ClickHistory(mousePackets, mousePresses);

    @Test
    void noClicksYet() {
        assertEquals(0, history.getLastClickMillis());
        assertNull(history.getLastClickPoint());
    }

    @Test
    void aPressAfterKrakensClickIsTheLastClick() {
        when(mousePackets.getLastClickMillis()).thenReturn(1_000L);
        when(mousePackets.getLastClickPoint()).thenReturn(new Point(10, 10));
        mousePresses.record(1_200, new Point(300, 400), false);

        assertEquals(1_200, history.getLastClickMillis());
        assertEquals(new Point(300, 400), history.getLastClickPoint());
        assertTrue(history.isLastClickPress());
    }

    @Test
    void krakensClickAfterAPressIsTheLastClick() {
        mousePresses.record(1_200, new Point(300, 400), false);
        when(mousePackets.getLastClickMillis()).thenReturn(1_350L);
        when(mousePackets.getLastClickPoint()).thenReturn(new Point(10, 10));

        assertEquals(1_350, history.getLastClickMillis());
        assertEquals(new Point(10, 10), history.getLastClickPoint());
        assertFalse(history.isLastClickPress());
    }
}
