package unit.com.kraken.api.query.container.inventory;

import com.kraken.api.query.container.inventory.InventoryOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the reversed reading order. BOTTOM_RIGHT_TOP_LEFT was once declared with the
 * same {@code Comparator.comparingInt(...)} as TOP_LEFT_BOTTOM_RIGHT and no {@code .reversed()}, so the
 * two orders were byte-identical. A behavioural test would need live InventoryEntity/ContainerItem
 * objects (a client and a mocking framework), so this instead asserts the structural property that
 * distinguishes the fix: the reverse order is backed by the JDK's reversing wrapper, while the forward
 * order is a plain lambda.
 */
class InventoryOrderTest {

    @Test
    void everyOrderHasAComparator() {
        for (InventoryOrder order : InventoryOrder.values()) {
            assertNotNull(order.getComparator(), order + " must have a comparator");
        }
    }

    @Test
    void reverseReadingOrderIsActuallyReversed() {
        // Comparator.reversed() returns a java.util reversing wrapper; the forward order is an
        // InventoryOrder-defined lambda. If the .reversed() were dropped, this would be a lambda too.
        assertFalse(isJdkReversingWrapper(InventoryOrder.TOP_LEFT_BOTTOM_RIGHT),
                "forward reading order should be a plain comparator, not a reversing wrapper");
        assertTrue(isJdkReversingWrapper(InventoryOrder.BOTTOM_RIGHT_TOP_LEFT),
                "BOTTOM_RIGHT_TOP_LEFT must be the reverse of TOP_LEFT_BOTTOM_RIGHT");
        assertTrue(isJdkReversingWrapper(InventoryOrder.ZIG_ZAG_REVERSE),
                "ZIG_ZAG_REVERSE must be the reverse of ZIG_ZAG");
    }

    private static boolean isJdkReversingWrapper(InventoryOrder order) {
        // Comparator.reversed() yields java.util.Collections$ReverseComparator2; a plain
        // Comparator.comparingInt(...) lambda does not carry "Reverse" in its class name.
        return order.getComparator().getClass().getName().contains("Reverse");
    }
}
