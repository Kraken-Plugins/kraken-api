package com.kraken.api.core.packet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kraken.api.Context;
import com.kraken.api.core.ClientThreadException;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import com.kraken.api.core.packet.model.BufferOperation;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketWrite;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PacketPreflightTest {
    private ReflectionHooks original;
    private PacketClient sender;
    private Client client;
    public static Writer writer;
    public static Packet packet;
    private static int allocations;
    private static boolean failQueue;
    private static boolean failFactory;
    private static boolean undersized;
    private static int offsetMultiplier;

    public static class Packet { public int length; }
    public static class Cipher { int consumed; }
    public static class Buffer { public byte[] array; public int offset; }
    public static class Node { public Buffer buffer; }
    public static class Writer {
        public Cipher cipher = new Cipher();
        Node queued;
        public void enqueue(Node node, byte garbage) {
            if (failQueue) throw new IllegalStateException("queue failure");
            queued = node;
        }
    }
    public static class StaticQueue {
        public static void enqueue(Writer writer, Node node, short garbage) {
            writer.enqueue(node, (byte) garbage);
        }
    }
    public static Node unrelatedFactory(Packet definition, Cipher cipher, byte garbage) {
        throw new AssertionError("Unmapped factory must not be selected");
    }
    public static Node create(Packet definition, Cipher cipher, byte garbage) {
        allocations++;
        cipher.consumed++;
        if (failFactory) throw new IllegalStateException("factory failure after cipher advancement");
        Node node = new Node();
        node.buffer = new Buffer();
        node.buffer.array = new byte[undersized ? 1 : 260];
        node.buffer.array[0] = (byte) 0xA7;
        node.buffer.offset = offsetMultiplier;
        return node;
    }

    @BeforeEach
    void setup() throws Exception {
        original = HooksLoader.getReflectionHooks();
        offsetMultiplier = original.getOffsetMultiplier();
        Gson gson = new Gson();
        JsonObject hooks = gson.toJsonTree(original).getAsJsonObject();
        hooks.addProperty("clientPacketClassName", PacketPreflightTest.class.getName());
        hooks.addProperty("classContainingPacketBufferNodeName", PacketPreflightTest.class.getName());
        hooks.addProperty("packetBufferNodeClassName", Node.class.getName());
        hooks.addProperty("packetBufferNodeFactoryMethodName", "create");
        hooks.addProperty("packetBufferFieldName", "buffer");
        hooks.addProperty("packetWriterFieldName", "writer");
        hooks.addProperty("packetWriterClassName", Writer.class.getSimpleName());
        hooks.addProperty("isaacCipherFieldName", "cipher");
        hooks.addProperty("addNodeClassName", "client");
        hooks.addProperty("addNodeMethodName", "enqueue");
        hooks.addProperty("bufferOffsetField", "offset");
        hooks.addProperty("bufferArrayField", "array");
        hooks.addProperty("clientPacketLengthField", "length");
        hooks.addProperty("clientPacketLengthMultiplier", 1);
        install(gson.fromJson(hooks, ReflectionHooks.class));
        writer = new Writer();
        packet = new Packet();
        packet.length = 4;
        allocations = 0;
        failQueue = failFactory = undersized = false;
        client = mock(Client.class);
        when(client.isClientThread()).thenReturn(true);
        sender = sender();
    }

    private PacketClient sender() throws Exception {
        Context context = mock(Context.class);
        when(context.runOnClientThreadOptional(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return Optional.ofNullable(callable.call());
        });
        PacketClient result = new PacketClient(client, () -> context);
        // The actual client class is mocked; seed only the writer field and packet definition class.
        set(result, "packetWriterField", PacketPreflightTest.class.getField("writer"));
        set(result, "clientPacketClass", Packet.class);
        // Packet constants normally live on ClientPacket itself; the test holder is separate.
        Field definitions = PacketClient.class.getDeclaredField("packetDefinitionFields");
        definitions.setAccessible(true);
        ((java.util.Map<String, Field>) definitions.get(result)).put("packet", PacketPreflightTest.class.getField("packet"));
        return result;
    }

    @AfterEach
    void restore() throws Exception { install(original); }

    @Test
    void validPacketConsumesOneCipherValueAndPreservesOpcode() {
        sender.sendPacket(definition(), 0x12345678);
        assertEquals(1, allocations);
        assertEquals(1, writer.cipher.consumed);
        assertArrayEquals(new byte[]{(byte) 0xA7, 0x12, 0x34, 0x56, 0x78},
                Arrays.copyOf(writer.queued.buffer.array, 5));
        assertEquals(5 * offsetMultiplier, writer.queued.buffer.offset);
    }

    @Test
    void invalidPayloadsNeverAllocateOrConsumeCipher() {
        reject(definition());
        reject(definition(), "wrong type");
        reject(definition(), (Object) null);
        reject(definition(), 1, 2);
        reject(new PacketDefinition("RESUME_COUNTDIALOG", "packet", null), 1);
        reject(new PacketDefinition("RESUME_COUNTDIALOG", "packet", new PacketWrite[]{
                new PacketWrite("missing", "", new BufferOperation[]{BufferOperation.raw()})}), 1);
        reject(new PacketDefinition("RESUME_COUNTDIALOG", "packet", new PacketWrite[]{
                new PacketWrite("var0", "", new BufferOperation[]{new BufferOperation()})}), 1);
        reject(new PacketDefinition("RESUME_COUNTDIALOG", "packet", new PacketWrite[]{
                new PacketWrite("var0", "", new BufferOperation[]{BufferOperation.rightShift(32)})}), 1);
    }

    @Test
    void wrongLengthAndMissingEnqueueRejectBeforeFactory() throws Exception {
        packet.length = 3;
        reject(definition(), 1);
        packet.length = 4;
        JsonObject hooks = new Gson().toJsonTree(HooksLoader.getReflectionHooks()).getAsJsonObject();
        hooks.addProperty("addNodeMethodName", "missing");
        install(new Gson().fromJson(hooks, ReflectionHooks.class));
        set(sender, "addNodeMethod", null);
        reject(definition(), 1);
    }

    @Test
    void staticEnqueueUsesWriterNodeAndDeclaredGarbageWidth() throws Exception {
        JsonObject hooks = new Gson().toJsonTree(HooksLoader.getReflectionHooks()).getAsJsonObject();
        hooks.addProperty("addNodeClassName", StaticQueue.class.getName());
        install(new Gson().fromJson(hooks, ReflectionHooks.class));
        sender = sender();
        sender.sendPacket(definition(), 1);
        assertNotNull(writer.queued);
        assertEquals(1, writer.cipher.consumed);
    }

    @Test
    void queueFailurePoisonsTransportAndStopsGameTraffic() {
        failQueue = true;
        assertThrows(ClientThreadException.class, () -> sender.sendPacket(definition(), 1));
        verify(client).setGameState(GameState.LOGIN_SCREEN);
        failQueue = false;
        assertThrows(ClientThreadException.class, () -> sender.sendPacket(definition(), 2));
        assertEquals(1, writer.cipher.consumed);
    }

    @Test
    void factoryFailureAndUnexpectedCapacityAreFatal() {
        failFactory = true;
        assertThrows(ClientThreadException.class, () -> sender.sendPacket(definition(), 1));
        assertEquals(1, writer.cipher.consumed);
        verify(client).setGameState(GameState.LOGIN_SCREEN);
    }

    @Test
    void unexpectedlySmallFactoryBufferIsFatal() {
        undersized = true;
        assertThrows(ClientThreadException.class, () -> sender.sendPacket(definition(), 1));
        assertEquals(1, writer.cipher.consumed);
        verify(client).setGameState(GameState.LOGIN_SCREEN);
    }

    private void reject(PacketDefinition definition, Object... values) {
        assertThrows(IllegalArgumentException.class, () -> sender.sendPacket(definition, values));
        assertEquals(0, allocations);
        assertEquals(0, writer.cipher.consumed);
        assertNull(writer.queued);
    }

    private PacketDefinition definition() {
        return new PacketDefinition("RESUME_COUNTDIALOG", "packet", new PacketWrite[]{
                new PacketWrite("var0", "", new BufferOperation[]{BufferOperation.rightShift(24),
                        BufferOperation.rightShift(16), BufferOperation.rightShift(8), BufferOperation.raw()})});
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void install(ReflectionHooks hooks) throws Exception {
        Field field = HooksLoader.class.getDeclaredField("reflectionHooks");
        field.setAccessible(true);
        field.set(null, hooks);
    }
}
