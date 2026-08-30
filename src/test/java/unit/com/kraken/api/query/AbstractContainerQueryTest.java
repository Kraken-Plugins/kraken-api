package unit.com.kraken.api.query;

import com.kraken.api.Context;
import com.kraken.api.core.Interactable;
import com.kraken.api.query.container.AbstractContainerQuery;
import com.kraken.api.query.container.ItemEntity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the shared container-item vocabulary against fake items: slot, noted, stackable,
 * quantity, action, and presence filters.
 */
class AbstractContainerQueryTest {

    private static final class FakeItem implements Interactable<Object>, ItemEntity {
        private final int id;
        private final String name;
        private final int quantity;
        private final int slot;
        private final boolean noted;
        private final boolean stackable;
        private final List<String> actions;

        FakeItem(int id, String name, int quantity, int slot, boolean noted, boolean stackable, String... actions) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.slot = slot;
            this.noted = noted;
            this.stackable = stackable;
            this.actions = Arrays.asList(actions);
        }

        @Override
        public boolean interact(String action) {
            return true;
        }

        @Override
        public Object raw() {
            return this;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getQuantity() {
            return quantity;
        }

        @Override
        public int getSlot() {
            return slot;
        }

        @Override
        public boolean isNoted() {
            return noted;
        }

        @Override
        public boolean isStackable() {
            return stackable;
        }

        @Override
        public boolean hasAction(String action) {
            return actions.stream().anyMatch(a -> a.equalsIgnoreCase(action));
        }
    }

    private static final class FakeContainerQuery extends AbstractContainerQuery<FakeItem, FakeContainerQuery, Object> {
        private final List<FakeItem> items;

        FakeContainerQuery(Context ctx, FakeItem... items) {
            super(ctx);
            this.items = Arrays.asList(items);
        }

        @Override
        protected Supplier<Stream<FakeItem>> source() {
            return items::stream;
        }
    }

    private static FakeItem shark(int slot) {
        return new FakeItem(385, "Shark", 1, slot, false, false, "Eat", "Drop");
    }

    private static FakeItem notedLobster(int quantity) {
        return new FakeItem(380, "Lobster", quantity, 3, true, true, "Drop");
    }

    private static FakeContainerQuery query(FakeItem... items) {
        return new FakeContainerQuery(QueryTestSupport.contextWithPlayerAt(null), items);
    }

    @Test
    void inSlotFiltersBySlot() {
        List<Integer> slots = query(shark(0), shark(5)).inSlot(5).stream()
                .map(FakeItem::getSlot).collect(Collectors.toList());

        assertEquals(List.of(5), slots);
    }

    @Test
    void notedAndUnnotedPartitionTheItems() {
        FakeContainerQuery base = query(shark(0), notedLobster(10));

        assertEquals(1, query(shark(0), notedLobster(10)).noted().count());
        assertEquals(1, base.unnoted().count());
    }

    @Test
    void stackableFilters() {
        assertEquals(1, query(shark(0), notedLobster(10)).stackable().count());
    }

    @Test
    void quantityGreaterThanIsStrict() {
        assertTrue(query(notedLobster(10)).quantityGreaterThan(10).isEmpty());
        assertFalse(query(notedLobster(11)).quantityGreaterThan(10).isEmpty());
    }

    @Test
    void withActionMatchesCaseInsensitively() {
        assertEquals(1, query(shark(0), notedLobster(10)).withAction("eat").count());
    }

    @Test
    void hasItemById() {
        assertTrue(query(shark(0)).hasItem(385));
        assertFalse(query(shark(0)).hasItem(386));
    }

    @Test
    void hasItemByNameIsCaseInsensitive() {
        assertTrue(query(shark(0)).hasItem("shark"));
        assertFalse(query(shark(0)).hasItem("lobster"));
        assertFalse(query(shark(0)).hasItem((String) null));
    }

    @Test
    void hasItemsRequiresEveryId() {
        assertTrue(query(shark(0), notedLobster(1)).hasItems(385, 380));
        assertFalse(query(shark(0)).hasItems(385, 380));
        assertTrue(query().hasItems(new int[0]));
    }

    @Test
    void hasItemsRequiresEveryName() {
        assertTrue(query(shark(0), notedLobster(1)).hasItems("Shark", "Lobster"));
        assertFalse(query(shark(0)).hasItems("Shark", "Lobster"));
    }
}
