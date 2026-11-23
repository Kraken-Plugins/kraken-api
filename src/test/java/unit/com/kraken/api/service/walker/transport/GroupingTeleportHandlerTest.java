package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.service.ui.tab.InterfaceTab;
import com.kraken.api.service.ui.tab.TabService;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.handler.GroupingTeleportHandler;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers what the minigame teleport handler does before it clicks anything: reading the activity
 * from the display info, looking it up in the game's table and opening the grouping tab. Each way
 * that can fail has to end the crossing with a reason and without a click.
 */
class GroupingTeleportHandlerTest {

    private static final int CASTLE_WARS_ROW = 13276;
    private static final int CASTLE_WARS_ID = 5;

    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final TabService tabs = mock(TabService.class);
    private final GroupingTeleportHandler handler = new GroupingTeleportHandler();

    @BeforeEach
    void setup() {
        when(ctx.getClient()).thenReturn(client);
        when(ctx.getService(TabService.class)).thenReturn(tabs);
        when(ctx.runOnClientThread(any(Callable.class), any()))
                .thenAnswer(call -> ((Callable<?>) call.getArgument(0)).call());
        when(client.getDBTableRows(DBTableID.MinigameTeleport.ID)).thenReturn(List.of(CASTLE_WARS_ROW));
        when(client.getDBTableField(CASTLE_WARS_ROW, DBTableID.MinigameTeleport.COL_ID, 0))
                .thenReturn(new Object[]{CASTLE_WARS_ID});
        when(client.getDBTableField(CASTLE_WARS_ROW, DBTableID.MinigameTeleport.COL_NAME, 0))
                .thenReturn(new Object[]{"Castle Wars"});
    }

    private TransportContext teleport(String displayInfo) {
        return TransportContext.builder().ctx(ctx).displayInfo(displayInfo).build();
    }

    @Test
    void aTeleportThatNamesNoActivityFailsBeforeAnythingIsOpened() {
        TransportContext context = teleport("  ");

        assertFalse(handler.execute(context));
        assertTrue(context.getFailureReason().contains("names no activity"), context.getFailureReason());
        verify(tabs, never()).switchTo(any());
    }

    @Test
    void anActivityTheGameDoesNotListFailsBeforeAnythingIsOpened() {
        TransportContext context = teleport("Fight Caves Minigame Teleport");

        assertFalse(handler.execute(context));
        assertTrue(context.getFailureReason().contains("Fight Caves"), context.getFailureReason());
        verify(tabs, never()).switchTo(any());
    }

    @Test
    void anActivityIsLookedUpInTheGamesTableAndTheChatChannelPanelIsOpenedForIt() {
        when(tabs.switchTo(InterfaceTab.CHAT)).thenReturn(false);
        TransportContext context = teleport("Castle Wars Minigame Teleport");

        assertFalse(handler.execute(context));
        assertTrue(context.getFailureReason().contains("grouping tab did not open"), context.getFailureReason());
        verify(tabs).switchTo(InterfaceTab.CHAT);
        verify(client, never()).runScript(any(), anyInt());
    }

    @Test
    void aTableThatCannotBeReadListsNoActivity() {
        when(client.getDBTableField(CASTLE_WARS_ROW, DBTableID.MinigameTeleport.COL_NAME, 0)).thenReturn(null);
        TransportContext context = teleport("Castle Wars");

        assertFalse(handler.execute(context));
        assertTrue(context.getFailureReason().contains("Castle Wars"), context.getFailureReason());
        verify(tabs, never()).switchTo(any());
    }
}
