package unit.com.kraken.api.query;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractSpatialQuery;
import com.kraken.api.core.Interactable;
import com.kraken.api.core.Locatable;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the shared spatial vocabulary against fake entities: distance filters, plane exclusion,
 * ordering, exact-tile matching, and the no-local-player degradation.
 */
class AbstractSpatialQueryTest {

    private static final WorldPoint PLAYER = new WorldPoint(3200, 3200, 0);

    private static final class FakeEntity implements Interactable<Object>, Locatable {
        private final int id;
        private final WorldPoint location;

        FakeEntity(int id, WorldPoint location) {
            this.id = id;
            this.location = location;
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
            return "fake-" + id;
        }

        @Override
        public WorldPoint getWorldLocation() {
            return location;
        }
    }

    private static final class FakeSpatialQuery extends AbstractSpatialQuery<FakeEntity, FakeSpatialQuery, Object> {
        private final List<FakeEntity> entities;

        FakeSpatialQuery(Context ctx, FakeEntity... entities) {
            super(ctx);
            this.entities = Arrays.asList(entities);
        }

        @Override
        protected Supplier<Stream<FakeEntity>> source() {
            return entities::stream;
        }
    }

    private static FakeSpatialQuery query(WorldPoint playerLocation, FakeEntity... entities) {
        return new FakeSpatialQuery(QueryTestSupport.contextWithPlayerAt(playerLocation), entities);
    }

    @Test
    void withinFiltersByChebyshevDistanceFromPlayer() {
        FakeEntity close = new FakeEntity(1, new WorldPoint(3203, 3202, 0));
        FakeEntity far = new FakeEntity(2, new WorldPoint(3210, 3200, 0));

        List<Integer> ids = query(PLAYER, close, far).within(5).stream()
                .map(FakeEntity::getId).collect(Collectors.toList());

        assertEquals(List.of(1), ids);
    }

    @Test
    void withinExcludesEntitiesOnOtherPlanes() {
        FakeEntity sameTileUpstairs = new FakeEntity(1, new WorldPoint(3200, 3200, 1));

        assertTrue(query(PLAYER, sameTileUpstairs).within(5).isEmpty());
    }

    @Test
    void withinYieldsNothingWithoutLocalPlayer() {
        FakeEntity entity = new FakeEntity(1, new WorldPoint(3200, 3200, 0));

        assertTrue(query(null, entity).within(5).isEmpty());
    }

    @Test
    void nearestReturnsClosestEntity() {
        FakeEntity near = new FakeEntity(1, new WorldPoint(3201, 3200, 0));
        FakeEntity far = new FakeEntity(2, new WorldPoint(3230, 3200, 0));

        Optional<FakeEntity> nearest = query(PLAYER, far, near).nearest();

        assertTrue(nearest.isPresent());
        assertEquals(1, nearest.get().getId());
    }

    @Test
    void nearestIsEmptyWithoutLocalPlayer() {
        FakeEntity entity = new FakeEntity(1, new WorldPoint(3200, 3200, 0));

        assertFalse(query(null, entity).nearest().isPresent());
    }

    @Test
    void nearestSortsDespawnedEntitiesLast() {
        FakeEntity despawned = new FakeEntity(1, null);
        FakeEntity alive = new FakeEntity(2, new WorldPoint(3220, 3200, 0));

        Optional<FakeEntity> nearest = query(PLAYER, despawned, alive).nearest();

        assertTrue(nearest.isPresent());
        assertEquals(2, nearest.get().getId());
    }

    @Test
    void nearestToUsesTheAnchorNotThePlayer() {
        WorldPoint anchor = new WorldPoint(3230, 3200, 0);
        FakeEntity nearPlayer = new FakeEntity(1, new WorldPoint(3201, 3200, 0));
        FakeEntity nearAnchor = new FakeEntity(2, new WorldPoint(3229, 3200, 0));

        Optional<FakeEntity> nearest = query(PLAYER, nearPlayer, nearAnchor).nearestTo(anchor);

        assertTrue(nearest.isPresent());
        assertEquals(2, nearest.get().getId());
    }

    @Test
    void atMatchesExactTileIncludingPlane() {
        WorldPoint tile = new WorldPoint(3205, 3207, 0);
        FakeEntity onTile = new FakeEntity(1, tile);
        FakeEntity upstairs = new FakeEntity(2, new WorldPoint(3205, 3207, 1));

        List<FakeEntity> matched = query(PLAYER, onTile, upstairs).at(tile).list();

        assertEquals(1, matched.size());
        assertEquals(1, matched.get(0).getId());
    }

    @Test
    void withinAreaAcceptsCornersInAnyOrder() {
        FakeEntity inside = new FakeEntity(1, new WorldPoint(3205, 3205, 0));
        FakeEntity outside = new FakeEntity(2, new WorldPoint(3250, 3250, 0));
        WorldPoint northEast = new WorldPoint(3210, 3210, 0);
        WorldPoint southWest = new WorldPoint(3200, 3200, 0);

        List<FakeEntity> matched = query(PLAYER, inside, outside).withinArea(northEast, southWest).list();

        assertEquals(1, matched.size());
        assertEquals(1, matched.get(0).getId());
    }

    @Test
    void sortByDistanceOrdersAscending() {
        FakeEntity far = new FakeEntity(1, new WorldPoint(3220, 3200, 0));
        FakeEntity near = new FakeEntity(2, new WorldPoint(3201, 3200, 0));
        FakeEntity middle = new FakeEntity(3, new WorldPoint(3210, 3200, 0));

        List<Integer> ids = query(PLAYER, far, near, middle).sortByDistance().stream()
                .map(FakeEntity::getId).collect(Collectors.toList());

        assertEquals(List.of(2, 3, 1), ids);
    }
}
