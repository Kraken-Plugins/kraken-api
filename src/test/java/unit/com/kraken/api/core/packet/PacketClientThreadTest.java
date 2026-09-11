package unit.com.kraken.api.core.packet;

import com.kraken.api.Context;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.PacketFactory;
import net.runelite.api.Client;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Covers the thread ownership {@link PacketClient} enforces.
 *
 * <p>The packet writer, node pool and ISAAC cipher a send reaches through belong to the game thread
 * and are shared with the client's own traffic. Building a packet from a script worker races that
 * traffic and can leave the cipher out of step with the server, so the sender refuses to run
 * anywhere but the client thread rather than trusting every caller to marshal first.</p>
 */
class PacketClientThreadTest {

    @Test
    void refusesToBuildAPacketOffTheClientThread() {
        Client client = mock(Client.class);
        when(client.isClientThread()).thenReturn(false);

        Context ctx = contextThatRunsInline();
        PacketClient packetClient = new PacketClient(client, () -> ctx);

        // Stands in for a caller that reaches the sender without a client-thread hand-off, which is
        // what every packet helper used to do.
        assertThrows(IllegalStateException.class,
                () -> packetClient.sendPacket(PacketFactory.getEventMouseClick(), 0, 1, 1, 0));
    }

    @Test
    void buildsThePacketOnceItIsOnTheClientThread() {
        Client client = mock(Client.class);
        when(client.isClientThread()).thenReturn(true);

        Context ctx = contextThatRunsInline();
        PacketClient packetClient = new PacketClient(client, () -> ctx);

        // The mocked client resolves none of the obfuscated handles, so the send bails out and logs.
        // What matters is that it got past the ownership check instead of being refused.
        assertDoesNotThrow(() -> packetClient.sendPacket(PacketFactory.getEventMouseClick(), 0, 1, 1, 0));
    }

    @Test
    void handsEverySendToTheClientThread() {
        Client client = mock(Client.class);
        when(client.isClientThread()).thenReturn(true);

        Context ctx = contextThatRunsInline();
        new PacketClient(client, () -> ctx).sendPacket(PacketFactory.getEventMouseClick(), 0, 1, 1, 0);

        verify(ctx).runOnClientThreadOptional(any());
    }

    /**
     * A {@link Context} whose client-thread hand-off runs the work on the calling thread, so a test
     * observes what the sender itself does rather than what the hand-off would have done.
     *
     * @return the mocked context
     */
    private static Context contextThatRunsInline() {
        Context ctx = mock(Context.class);
        try {
            when(ctx.runOnClientThreadOptional(any())).thenAnswer(invocation -> {
                Callable<?> work = invocation.getArgument(0);
                return Optional.ofNullable(work.call());
            });
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return ctx;
    }
}
