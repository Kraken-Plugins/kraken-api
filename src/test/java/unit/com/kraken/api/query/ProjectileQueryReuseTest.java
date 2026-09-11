package unit.com.kraken.api.query;

import com.kraken.api.Context;
import com.kraken.api.query.projectile.ProjectileEntity;
import com.kraken.api.query.projectile.ProjectileQuery;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectileQueryReuseTest {
    @Test
    void landingFilterRefreshesAndSharesAnchorWithSpatialFilters() {
        WorldPoint origin = new WorldPoint(3200, 3200, 0);
        WorldPoint target = new WorldPoint(3210, 3200, 0);
        Context ctx = QueryTestSupport.contextWithPlayerAt(origin);
        Player player = ctx.getClient().getLocalPlayer();
        ProjectileEntity projectile = mock(ProjectileEntity.class);
        when(projectile.getTargetPoint()).thenReturn(target);
        when(projectile.getWorldLocation()).thenReturn(target);
        ProjectileQuery query = new ProjectileQuery(ctx).from(projectile)
                .landingWithin(1).within(2).sortByDistance();
        verify(player, never()).getWorldLocation();
        assertTrue(query.list().isEmpty());
        verify(player, times(1)).getWorldLocation();
        when(player.getWorldLocation()).thenReturn(target);
        assertEquals(List.of(projectile), query.list());
        verify(player, times(2)).getWorldLocation();
        when(ctx.getClient().getLocalPlayer()).thenReturn(null);
        assertTrue(query.list().isEmpty());
        when(ctx.getClient().getLocalPlayer()).thenReturn(player);
        assertEquals(List.of(projectile), query.list());
    }
}
