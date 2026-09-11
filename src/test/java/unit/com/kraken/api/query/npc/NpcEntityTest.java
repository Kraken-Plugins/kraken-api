package unit.com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.gameval.SpriteID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcEntityTest {

    private NPC npc;
    private NpcEntity entity;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        Context ctx = mock(Context.class);
        when(ctx.runOnClientThread(any(Callable.class), any()))
                .thenAnswer(invocation -> ((Callable<Object>) invocation.getArgument(0)).call());
        npc = mock(NPC.class);
        entity = new NpcEntity(ctx, npc);
    }

    @Test
    void unknownHealthIsMinusOne() {
        when(npc.getHealthRatio()).thenReturn(-1);
        when(npc.getHealthScale()).thenReturn(-1);
        assertEquals(-1.0, entity.getHealthPercentage());
    }

    @Test
    void zeroScaleIsUnknown() {
        when(npc.getHealthRatio()).thenReturn(5);
        when(npc.getHealthScale()).thenReturn(0);
        assertEquals(-1.0, entity.getHealthPercentage());
    }

    @Test
    void knownHealthIsPercentage() {
        when(npc.getHealthRatio()).thenReturn(15);
        when(npc.getHealthScale()).thenReturn(30);
        assertEquals(50.0, entity.getHealthPercentage());
    }

    @Test
    void noOverheadsGivesNoIcon() {
        assertNull(entity.getHeadIcon());
        when(npc.getOverheadArchiveIds()).thenReturn(new int[] {-1});
        when(npc.getOverheadSpriteIds()).thenReturn(new short[] {-1});
        assertNull(entity.getHeadIcon());
    }

    @Test
    void prayerArchiveIndexMapsToIcon() {
        when(npc.getOverheadArchiveIds()).thenReturn(new int[] {SpriteID.HEADICONS_PRAYER});
        when(npc.getOverheadSpriteIds()).thenReturn(new short[] {(short) HeadIcon.MAGIC.ordinal()});
        assertEquals(HeadIcon.MAGIC, entity.getHeadIcon());
    }

    @Test
    void otherArchivesAreIgnoredAndPrayerIconStillFound() {
        when(npc.getOverheadArchiveIds()).thenReturn(new int[] {12345, SpriteID.HEADICONS_PRAYER});
        when(npc.getOverheadSpriteIds()).thenReturn(new short[] {0, (short) HeadIcon.RANGED.ordinal()});
        assertEquals(HeadIcon.RANGED, entity.getHeadIcon());
    }

    @Test
    void unsupportedArchiveOrOutOfRangeIndexDoesNotThrow() {
        when(npc.getOverheadArchiveIds()).thenReturn(new int[] {12345, SpriteID.HEADICONS_PRAYER});
        when(npc.getOverheadSpriteIds()).thenReturn(new short[] {2, Short.MAX_VALUE});
        assertNull(entity.getHeadIcon());
    }
}
