package unit.com.kraken.api.core.interaction;

import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.InteractionDispatcher;
import com.kraken.api.core.interaction.DoActionInvoker;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import com.kraken.api.core.interaction.resolver.*;
import com.kraken.api.core.packet.entity.MousePackets;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EntityWorldViewTest {
    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final WorldView owner = mock(WorldView.class);
    private final NpcMenuActionResolver npcs = new NpcMenuActionResolver();
    private final PlayerMenuActionResolver players = new PlayerMenuActionResolver();
    private final TileObjectMenuActionResolver objects = new TileObjectMenuActionResolver();

    @BeforeEach
    void setup() throws Exception {
        when(ctx.getClient()).thenReturn(client);
        when(ctx.runOnClientThread(any(Callable.class))).thenAnswer(call -> ((Callable<?>) call.getArgument(0)).call());
        when(owner.getId()).thenReturn(42);
        for (Object resolver : new Object[]{npcs, players, objects}) {
            inject(resolver, "ctxProvider", (Provider<Context>) () -> ctx);
        }
    }

    @Test
    void npcNormalAndTargetActionsUseOwnerAndDispatchPreservesIt() throws Exception {
        NPC npc = mock(NPC.class);
        NPCComposition definition = mock(NPCComposition.class);
        when(npc.getWorldView()).thenReturn(owner);
        when(npc.getLocalLocation()).thenAnswer(call -> LocalPoint.fromScene(10, 20, owner));
        when(npc.getIndex()).thenReturn(7);
        when(npc.getComposition()).thenReturn(definition);
        when(definition.getActions()).thenReturn(new String[]{"Attack"});
        ResolvedMenuAction normal = npcs.resolve(npc, "Attack").orElseThrow();
        assertEquals(42, normal.getOption().getWorldView());
        when(client.isWidgetSelected()).thenReturn(true);
        assertEquals(42, npcs.resolve(npc, "Cast").orElseThrow().getOption().getWorldView());

        InteractionDispatcher dispatcher = new InteractionDispatcher();
        DoActionInvoker engine = mock(DoActionInvoker.class);
        inject(dispatcher, "ctxProvider", (Provider<Context>) () -> ctx);
        inject(dispatcher, "mousePackets", mock(MousePackets.class));
        inject(dispatcher, "doActionInvoker", engine);
        when(ctx.runOnClientThread(any(Callable.class), eq(Boolean.FALSE)))
                .thenAnswer(call -> ((Callable<?>) call.getArgument(0)).call());
        dispatcher.dispatch(new Point(1, 2), "Attack", normal);
        verify(engine).invoke(10, 20, MenuAction.NPC_FIRST_OPTION.getId(), 7, -1, 42, "Attack", "", 1, 2);
        verify(client, never()).getTopLevelWorldView();
    }

    @Test
    void tileObjectsUseOwnerAndSouthWestSceneTile() {
        GameObject object = mock(GameObject.class);
        ObjectComposition definition = mock(ObjectComposition.class);
        when(object.getWorldView()).thenReturn(owner);
        when(object.getId()).thenReturn(123);
        when(object.getSceneMinLocation()).thenReturn(new Point(15, 16));
        when(client.getObjectDefinition(123)).thenReturn(definition);
        when(definition.getActions()).thenReturn(new String[]{"Open"});
        ResolvedMenuAction normal = objects.resolve(object, "Open").orElseThrow();
        assertEquals(42, normal.getOption().getWorldView());
        assertEquals(15, normal.getOption().getParam0());
        assertEquals(16, normal.getOption().getParam1());
        when(client.isWidgetSelected()).thenReturn(true);
        assertEquals(42, objects.resolve(object, "Use").orElseThrow().getOption().getWorldView());
    }

    @Test
    void playersUseOwnerForNormalAndTargetActions() {
        Player player = mock(Player.class);
        when(player.getWorldView()).thenReturn(owner);
        when(player.getLocalLocation()).thenAnswer(call -> LocalPoint.fromScene(10, 20, owner));
        when(client.getPlayerOptions()).thenReturn(new String[]{"Follow"});
        assertEquals(42, players.resolve(player, "Follow").orElseThrow().getOption().getWorldView());
        when(client.isWidgetSelected()).thenReturn(true);
        assertEquals(42, players.resolve(player, "Cast").orElseThrow().getOption().getWorldView());
    }

    @Test
    void missingOwnerIsRejected() {
        assertTrue(npcs.resolve(mock(NPC.class), "Attack").isEmpty());
        assertTrue(players.resolve(mock(Player.class), "Follow").isEmpty());
        assertTrue(objects.resolve(mock(GameObject.class), "Open").isEmpty());
    }

    private void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
