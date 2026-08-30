package unit.com.kraken.api.query;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.core.Interactable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Optional contract on the single-valued query terminals: a miss is
 * {@link Optional#empty()}, never null, and the interact terminals degrade to false.
 */
class AbstractQueryTerminalsTest {

    private static final class FakeEntity implements Interactable<Object> {
        private final int id;
        private final String name;
        private boolean interacted = false;

        FakeEntity(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean interact(String action) {
            interacted = true;
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
    }

    private static final class FakeQuery extends AbstractQuery<FakeEntity, FakeQuery, Object> {
        private final List<FakeEntity> entities;

        FakeQuery(Context ctx, FakeEntity... entities) {
            super(ctx);
            this.entities = Arrays.asList(entities);
        }

        @Override
        protected Supplier<Stream<FakeEntity>> source() {
            return entities::stream;
        }
    }

    private static FakeQuery query(FakeEntity... entities) {
        return new FakeQuery(QueryTestSupport.contextWithPlayerAt(null), entities);
    }

    @Test
    void firstReturnsEmptyOptionalOnNoMatch() {
        Optional<FakeEntity> result = query().first();

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void firstReturnsTheFirstMatch() {
        Optional<FakeEntity> result = query(new FakeEntity(1, "a"), new FakeEntity(2, "b")).first();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
    }

    @Test
    void firstRespectsFilters() {
        Optional<FakeEntity> result = query(new FakeEntity(1, "a"), new FakeEntity(2, "b"))
                .withName("b")
                .first();

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
    }

    @Test
    void firstMatchingReturnsEmptyOptionalOnNoMatch() {
        Optional<FakeEntity> result = query(new FakeEntity(1, "a")).firstMatching(e -> e.getId() == 9);

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void randomReturnsEmptyOptionalOnNoMatch() {
        Optional<FakeEntity> result = query().random();

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    void randomReturnsTheOnlyMatch() {
        Optional<FakeEntity> result = query(new FakeEntity(7, "a")).random();

        assertTrue(result.isPresent());
        assertEquals(7, result.get().getId());
    }

    @Test
    void interactReturnsFalseOnNoMatch() {
        assertFalse(query().interact("Use"));
    }

    @Test
    void interactDispatchesToTheFirstMatch() {
        FakeEntity first = new FakeEntity(1, "a");
        FakeEntity second = new FakeEntity(2, "b");

        assertTrue(query(first, second).interact("Use"));
        assertTrue(first.interacted);
        assertFalse(second.interacted);
    }

    @Test
    void interactRandomReturnsFalseOnNoMatch() {
        assertFalse(query().interactRandom("Use"));
    }

    @Test
    void listIsNeverNull() {
        assertNotNull(query().list());
        assertTrue(query().list().isEmpty());
    }
}
