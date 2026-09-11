package unit.com.kraken.api.query;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractSpatialQuery;
import com.kraken.api.core.Interactable;
import com.kraken.api.core.Locatable;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Player;
import java.util.Comparator;
import static org.mockito.Mockito.*;
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
    @Test
    void reusedFiltersAndSortCaptureOneFreshAnchorPerEvaluation() {
        Context ctx = QueryTestSupport.contextWithPlayerAt(PLAYER);
        Player player = ctx.getClient().getLocalPlayer();
        FakeEntity oldTarget = new FakeEntity(1, PLAYER);
        FakeEntity newTarget = new FakeEntity(2, new WorldPoint(3210, 3200, 0));
        FakeSpatialQuery query = new FakeSpatialQuery(ctx, oldTarget, newTarget)
                .within(20).within(1).sortByDistance();
        verify(player, never()).getWorldLocation();
        assertEquals(List.of(oldTarget), query.list());
        verify(player, times(1)).getWorldLocation();

        when(player.getWorldLocation()).thenReturn(newTarget.location);
        assertEquals(List.of(newTarget), query.list());
        verify(player, times(2)).getWorldLocation();
        assertEquals(Optional.of(newTarget), query.nearest());
        verify(player, times(3)).getWorldLocation();
    }

    @Test
    void reusedSortTracksPlayerAndLastSortDeclarationWins() {
        Context ctx = QueryTestSupport.contextWithPlayerAt(PLAYER);
        FakeEntity first = new FakeEntity(1, PLAYER);
        FakeEntity second = new FakeEntity(2, new WorldPoint(3210, 3200, 0));
        FakeSpatialQuery query = new FakeSpatialQuery(ctx, first, second).sortByDistance();
        assertEquals(List.of(first, second), query.list());
        when(ctx.getClient().getLocalPlayer().getWorldLocation()).thenReturn(second.location);
        assertEquals(List.of(second, first), query.list());
        assertEquals(List.of(first, second), query.sortByDistanceTo(PLAYER).list());
        assertEquals(List.of(second, first), query.sortByDistance().list());
        assertEquals(List.of(first, second), query.sorted(Comparator.comparingInt(FakeEntity::getId)).list());
        assertEquals(List.of(first, second), query.sortByDistance().sorted(null).list());
    }

    @Test
    void missingPlayerDoesNotPermanentlyEmptyReusableQueries() {
        Context ctx = QueryTestSupport.contextWithPlayerAt(null);
        FakeEntity entity = new FakeEntity(1, PLAYER);
        FakeSpatialQuery query = new FakeSpatialQuery(ctx, entity).within(Integer.MAX_VALUE);
        assertTrue(query.list().isEmpty());
        Player player = mock(Player.class);
        when(player.getWorldLocation()).thenReturn(PLAYER);
        when(ctx.getClient().getLocalPlayer()).thenReturn(player);
        assertEquals(List.of(entity), query.list());
        when(ctx.getClient().getLocalPlayer()).thenReturn(null);
        assertTrue(query.list().isEmpty());
        assertTrue(query.nearest().isEmpty());
        when(ctx.getClient().getLocalPlayer()).thenReturn(player);
        assertEquals(Optional.of(entity), query.nearest());
    }

    @Test
    void explicitAnchorRemainsFixedAndNoPlayerSortKeepsSourceOrder() {
        Context ctx = QueryTestSupport.contextWithPlayerAt(PLAYER);
        FakeEntity near = new FakeEntity(1, PLAYER);
        FakeEntity far = new FakeEntity(2, new WorldPoint(3210, 3200, 0));
        FakeSpatialQuery fixed = new FakeSpatialQuery(ctx, near, far).within(PLAYER, 1);
        when(ctx.getClient().getLocalPlayer()).thenReturn(null);
        assertEquals(List.of(near), fixed.list());
        assertEquals(Optional.of(near), fixed.nearestTo(PLAYER));
        assertEquals(List.of(far, near), new FakeSpatialQuery(ctx, far, near).sortByDistance().list());
    }

    @Test
    void nestedEvaluationCannotReplaceOuterAnchor() {
        Context ctx = QueryTestSupport.contextWithPlayerAt(PLAYER);
        Player player = ctx.getClient().getLocalPlayer();
        FakeEntity entity = new FakeEntity(1, PLAYER);
        boolean[] nested = {false};
        FakeSpatialQuery query = new FakeSpatialQuery(ctx, entity);
        query.filter(ignored -> {
            if (!nested[0]) {
                nested[0] = true;
                when(player.getWorldLocation()).thenReturn(new WorldPoint(3300, 3300, 0));
                assertTrue(query.list().isEmpty());
            }
            return true;
        }).within(1);
        assertEquals(List.of(entity), query.list());
    }

}
