package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.WidgetClicks;
import net.runelite.api.widgets.Widget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers reading a child a client script created inside a layer, which has to answer with nothing
 * for an index the layer does not have. A layer is rebuilt whenever its interface redraws, so an
 * index that was there a moment ago can be past the end.
 */
class WidgetClicksTest {

    @Test
    void theChildAtAnIndexIsReturned() {
        Widget first = mock(Widget.class);
        Widget third = mock(Widget.class);
        Widget layer = mock(Widget.class);
        when(layer.getChildren()).thenReturn(new Widget[]{first, null, third});

        assertSame(first, WidgetClicks.childAt(layer, 0));
        assertSame(third, WidgetClicks.childAt(layer, 2));
        assertNull(WidgetClicks.childAt(layer, 1), "a slot the script left empty");
    }

    @Test
    void anIndexTheLayerDoesNotHaveIsNoChild() {
        Widget layer = mock(Widget.class);
        when(layer.getChildren()).thenReturn(new Widget[]{mock(Widget.class)});
        Widget empty = mock(Widget.class);
        when(empty.getChildren()).thenReturn(null);

        assertNull(WidgetClicks.childAt(layer, 1));
        assertNull(WidgetClicks.childAt(layer, 29));
        assertNull(WidgetClicks.childAt(layer, -1));
        assertNull(WidgetClicks.childAt(empty, 0), "a layer whose children were all deleted");
        assertNull(WidgetClicks.childAt(null, 0));
    }
}
