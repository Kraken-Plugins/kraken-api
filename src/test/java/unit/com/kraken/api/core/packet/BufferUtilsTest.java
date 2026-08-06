package unit.com.kraken.api.core.packet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import com.kraken.api.core.packet.BufferUtils;
import com.kraken.api.core.packet.model.BufferOperation;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketWrite;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link BufferUtils} produces byte-identical output to the client's own buffer
 * encoding, independent of the reflective field-handle caching. Every operation is checked
 * against a plain append-only byte model that shares none of the scrambled-offset arithmetic
 * with production code, so a regression in the offset/index math or the field caching shows up
 * as a concrete byte difference.
 * <p>
 * The tests install a copy of the real hooks with only the buffer field names redirected at the
 * {@link FakeBuffer} fixture; the offset and index multipliers are the real values from
 * {@code hooks.json}, so the index arithmetic under test is exactly what runs in production.
 */
class BufferUtilsTest {

    private static final int BUFFER_SIZE = 1 << 16;

    private static ReflectionHooks realHooks;

    /**
     * Stands in for the client's obfuscated packet buffer. Field names deliberately mirror the
     * shape of a hooked buffer class: one public int offset field and one public byte[] field,
     * whose names are injected into the hooks copy installed by {@link #installTestHooks()}.
     */
    public static class FakeBuffer {
        public int fakeOffset;
        public byte[] fakeArray;
    }

    /**
     * Simulates a client revision where the hooked fields live on a buffer superclass, which
     * {@code Class.getField} resolves through inheritance.
     */
    public static class ExtendedFakeBuffer extends FakeBuffer {
    }

    @BeforeAll
    static void installTestHooks() throws Exception {
        realHooks = HooksLoader.getReflectionHooks();
        Gson gson = new Gson();
        JsonObject copy = gson.toJsonTree(realHooks).getAsJsonObject();
        copy.addProperty("bufferOffsetField", "fakeOffset");
        copy.addProperty("bufferArrayField", "fakeArray");
        setHooks(gson.fromJson(copy, ReflectionHooks.class));
    }

    @AfterAll
    static void restoreRealHooks() throws Exception {
        setHooks(realHooks);
    }

    private static void setHooks(ReflectionHooks hooks) throws Exception {
        Field field = HooksLoader.class.getDeclaredField("reflectionHooks");
        field.setAccessible(true);
        field.set(null, hooks);
    }

    /**
     * The entire scrambled-offset scheme rests on the two hook multipliers being modular
     * inverses mod 2^32. If a hooks.json update ever breaks this, every write lands at a
     * garbage index.
     */
    @Test
    void offsetAndIndexMultipliersAreModularInverses() {
        assertEquals(1, realHooks.getOffsetMultiplier() * realHooks.getIndexMultiplier(),
                "offsetMultiplier * indexMultiplier must equal 1 (mod 2^32); hooks.json is inconsistent");
    }

    @Test
    void rawWriteMatchesClientEncoding() {
        int[] values = {0, 1, 127, 128, 255, 256, 1234, 32767, 32768, 65535, -1, -128, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int value : values) {
            FakeBuffer buffer = newBuffer(0);
            BufferUtils.writeOperation(BufferOperation.raw(), value, buffer);

            assertEquals((byte) value, buffer.fakeArray[0], "value=" + value);
            assertEquals(1, realOffset(buffer), "value=" + value);
        }
    }

    @Test
    void addWriteMatchesClientEncoding() {
        int[] values = {0, 1, 200, 255, 275, 3200, 65535, -5};
        for (int value : values) {
            FakeBuffer buffer = newBuffer(0);
            BufferUtils.writeOperation(BufferOperation.add(128), value, buffer);

            assertEquals((byte) (128 + value), buffer.fakeArray[0], "value=" + value);
            assertEquals(1, realOffset(buffer), "value=" + value);
        }
    }

    @Test
    void subtractWriteMatchesClientEncoding() {
        int[] values = {0, 1, 100, 255, -3};
        for (int value : values) {
            FakeBuffer buffer = newBuffer(0);
            BufferUtils.writeOperation(BufferOperation.subtract(0), value, buffer);

            assertEquals((byte) (0 - value), buffer.fakeArray[0], "value=" + value);
            assertEquals(1, realOffset(buffer), "value=" + value);
        }
    }

    @Test
    void rightShiftWriteMatchesClientEncoding() {
        int[] values = {0, 255, 256, 1234, 32767, 65535, Integer.MAX_VALUE};
        for (int value : values) {
            FakeBuffer buffer = newBuffer(0);
            BufferUtils.writeOperation(BufferOperation.rightShift(8), value, buffer);

            assertEquals((byte) (value >> 8), buffer.fakeArray[0], "value=" + value);
            assertEquals(1, realOffset(buffer), "value=" + value);
        }
    }

    @Test
    void nullTerminatedStringMatchesClientEncoding() {
        FakeBuffer buffer = newBuffer(0);
        String value = "Hello, world";
        BufferUtils.writeOperation(BufferOperation.stringCp1252NullTerminated(), value, buffer);

        ExpectedBytes expected = new ExpectedBytes();
        expected.string(value);
        expected.raw(0);
        assertBufferMatches(expected, buffer);
    }

    @Test
    void nullCircumfixedStringMatchesClientEncoding() {
        FakeBuffer buffer = newBuffer(0);
        String value = "Kraken";
        BufferUtils.writeOperation(BufferOperation.stringCp1252NullCircumfixed(), value, buffer);

        ExpectedBytes expected = new ExpectedBytes();
        expected.raw(0);
        expected.string(value);
        expected.raw(0);
        assertBufferMatches(expected, buffer);
    }

    @Test
    void cp1252SpecialCharactersMatchClientEncoding() {
        FakeBuffer buffer = newBuffer(0);
        // The euro sign and trademark sign come from the client's explicit CP1252 mapping table;
        // pi is unmappable and must degrade to '?' exactly as the client does. Escapes keep the
        // literals immune to source-file encoding differences between build environments.
        String value = "a\u20AC\u2122\u03C0";
        BufferUtils.writeOperation(BufferOperation.stringCp1252NullTerminated(), value, buffer);

        assertEquals((byte) 'a', buffer.fakeArray[0]);
        assertEquals((byte) -128, buffer.fakeArray[1]);
        assertEquals((byte) -103, buffer.fakeArray[2]);
        assertEquals((byte) '?', buffer.fakeArray[3]);
        assertEquals((byte) 0, buffer.fakeArray[4]);
        assertEquals(5, realOffset(buffer));
    }

    @Test
    void writesStartingAtNonZeroOffsetLandAtTheRealIndex(){
        int initialOffset = 37;
        FakeBuffer buffer = newBuffer(initialOffset);
        BufferUtils.writeOperation(BufferOperation.raw(), 0xAB, buffer);

        assertEquals((byte) 0xAB, buffer.fakeArray[initialOffset]);
        assertEquals(initialOffset + 1, realOffset(buffer));
    }

    /**
     * Pins the exact bytes of an EVENT_MOUSE_CLICK payload for the current hooks revision. If a
     * future hooks.json changes this packet's write structure, re-derive the literal expectation
     * against a known-good client before updating it.
     */
    @Test
    void eventMouseClickWireFormatIsPinned() {
        PacketDefinition def = HooksLoader.getPackets().get("EVENT_MOUSE_CLICK");
        FakeBuffer buffer = newBuffer(0);
        replayPacketWrites(def, new Object[]{1234, 350, 275, 0}, buffer, null);

        byte[] expected = {0, (byte) (128 + 275), (byte) (275 >> 8), (byte) (1234 >> 8), (byte) 1234, (byte) (128 + 350), (byte) (350 >> 8)};
        byte[] actual = new byte[expected.length];
        System.arraycopy(buffer.fakeArray, 0, actual, 0, expected.length);
        assertArrayEquals(expected, actual);
        assertEquals(expected.length, realOffset(buffer));
    }

    @Test
    void everyPacketDefinitionReplaysToTheModeledBytes() {
        Map<String, Object[]> scenarios = new HashMap<>();
        scenarios.put("EVENT_MOUSE_CLICK", new Object[]{4321, 12, 900, 0});
        scenarios.put("MOVE_GAMECLICK", new Object[]{3221, 3218, 1, 5});
        scenarios.put("RESUME_COUNTDIALOG", new Object[]{1_000_000});
        scenarios.put("RESUME_OBJDIALOG", new Object[]{995});
        scenarios.put("RESUME_STRINGDIALOG", new Object[]{4, "Test"});

        for (Map.Entry<String, Object[]> scenario : scenarios.entrySet()) {
            PacketDefinition def = HooksLoader.getPackets().get(scenario.getKey());
            assertEquals(scenario.getKey(), def.getPacketName());

            FakeBuffer buffer = newBuffer(0);
            ExpectedBytes expected = new ExpectedBytes();
            replayPacketWrites(def, scenario.getValue(), buffer, expected);
            assertBufferMatches(expected, buffer);
        }
    }

    @Test
    void inheritedBufferFieldsResolveThroughTheSuperclass() {
        ExtendedFakeBuffer buffer = new ExtendedFakeBuffer();
        buffer.fakeArray = new byte[BUFFER_SIZE];
        buffer.fakeOffset = 0;
        BufferUtils.writeOperation(BufferOperation.add(128), 300, buffer);

        assertEquals((byte) (128 + 300), buffer.fakeArray[0]);
        assertEquals(1, realOffset(buffer));
    }

    @Test
    void thousandsOfMixedOperationsStayByteIdenticalToTheModel() {
        FakeBuffer buffer = newBuffer(0);
        ExpectedBytes expected = new ExpectedBytes();
        Random random = new Random(42);

        for (int i = 0; i < 4000; i++) {
            int value = random.nextInt();
            switch (random.nextInt(5)) {
                case 0:
                    BufferUtils.writeOperation(BufferOperation.raw(), value, buffer);
                    expected.raw(value);
                    break;
                case 1:
                    BufferUtils.writeOperation(BufferOperation.add(128), value, buffer);
                    expected.add(128, value);
                    break;
                case 2:
                    BufferUtils.writeOperation(BufferOperation.subtract(0), value, buffer);
                    expected.subtract(0, value);
                    break;
                case 3:
                    BufferUtils.writeOperation(BufferOperation.rightShift(8), value, buffer);
                    expected.rightShift(8, value);
                    break;
                default:
                    String s = "v" + (value & 0xFFF);
                    BufferUtils.writeOperation(BufferOperation.stringCp1252NullTerminated(), s, buffer);
                    expected.string(s);
                    expected.raw(0);
                    break;
            }
        }
        assertBufferMatches(expected, buffer);
    }

    @Test
    void concurrentWritersOnSeparateBuffersDoNotInterfere() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> workers = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                final int seed = t;
                workers.add(() -> {
                    for (int i = 0; i < 250; i++) {
                        FakeBuffer buffer = newBuffer(0);
                        ExpectedBytes expected = new ExpectedBytes();
                        int value = seed * 1000 + i;

                        BufferUtils.writeOperation(BufferOperation.rightShift(8), value, buffer);
                        expected.rightShift(8, value);
                        BufferUtils.writeOperation(BufferOperation.raw(), value, buffer);
                        expected.raw(value);
                        BufferUtils.writeOperation(BufferOperation.stringCp1252NullCircumfixed(), "w" + seed, buffer);
                        expected.raw(0);
                        expected.string("w" + seed);
                        expected.raw(0);

                        assertBufferMatches(expected, buffer);
                    }
                    return null;
                });
            }
            for (Future<Void> result : pool.invokeAll(workers)) {
                result.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void unresolvableBufferClassFailsLoudlyBeforeTouchingAnyState() {
        assertThrows(IllegalStateException.class, () -> BufferUtils.getOffset(new Object()));
        assertThrows(IllegalStateException.class, () -> BufferUtils.getArray(new Object()));
    }

    /**
     * Runs a packet definition's writes through {@link BufferUtils} exactly the way
     * {@code PacketClient.sendPacket} does: values mapped by parameter name, operations applied
     * in declaration order.
     */
    private static void replayPacketWrites(PacketDefinition def, Object[] objects, Object buffer, ExpectedBytes expected) {
        List<String> params = def.getType().getParams();
        Map<String, Integer> paramIndices = new HashMap<>();
        for (int i = 0; i < params.size(); i++) {
            paramIndices.put(params.get(i), i);
        }

        for (PacketWrite write : def.getWrites()) {
            Object value = objects[paramIndices.get(write.getParam())];
            for (BufferOperation operation : write.getOperations()) {
                BufferUtils.writeOperation(operation, value, buffer);
                if (expected != null) {
                    applyToModel(expected, operation, value);
                }
            }
        }
    }

    private static void applyToModel(ExpectedBytes expected, BufferOperation operation, Object value) {
        switch (operation.getType()) {
            case RAW:
                expected.raw(((Number) value).intValue());
                break;
            case ADD:
                expected.add(operation.requireOperand(), ((Number) value).intValue());
                break;
            case SUBTRACT:
                expected.subtract(operation.requireOperand(), ((Number) value).intValue());
                break;
            case RIGHT_SHIFT:
                expected.rightShift(operation.requireOperand(), ((Number) value).intValue());
                break;
            case STRING_CP1252_NULL_TERMINATED:
                expected.string((String) value);
                expected.raw(0);
                break;
            case STRING_CP1252_NULL_CIRCUMFIXED:
                expected.raw(0);
                expected.string((String) value);
                expected.raw(0);
                break;
            default:
                throw new IllegalArgumentException("Unhandled operation: " + operation);
        }
    }

    private static FakeBuffer newBuffer(int initialRealOffset) {
        FakeBuffer buffer = new FakeBuffer();
        buffer.fakeArray = new byte[BUFFER_SIZE];
        // A real offset of r is stored scrambled as r * offsetMultiplier (mod 2^32), which is what
        // the client keeps in the hooked offset field.
        buffer.fakeOffset = initialRealOffset * HooksLoader.getReflectionHooks().getOffsetMultiplier();
        return buffer;
    }

    /**
     * Descrambles the buffer's stored offset back to the count of bytes written.
     */
    private static int realOffset(FakeBuffer buffer) {
        return buffer.fakeOffset * HooksLoader.getReflectionHooks().getIndexMultiplier();
    }

    private static void assertBufferMatches(ExpectedBytes expected, FakeBuffer buffer) {
        byte[] expectedBytes = expected.toArray();
        assertEquals(expectedBytes.length, realOffset(buffer), "final offset diverged from bytes written");
        byte[] actual = new byte[expectedBytes.length];
        System.arraycopy(buffer.fakeArray, 0, actual, 0, expectedBytes.length);
        assertArrayEquals(expectedBytes, actual, "buffer contents diverged from the modeled bytes");
    }

    /**
     * Append-only reference model for the client's buffer encoding. Deliberately knows nothing
     * about scrambled offsets, index multipliers or reflection — just the bytes that must end up
     * on the wire, in order.
     */
    private static final class ExpectedBytes {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        void raw(int value) {
            out.write(value);
        }

        void add(int operand, int value) {
            out.write(operand + value);
        }

        void subtract(int operand, int value) {
            out.write(operand - value);
        }

        void rightShift(int operand, int value) {
            out.write(value >> operand);
        }

        void string(String value) {
            byte[] encoded = cp1252(value);
            out.write(encoded, 0, encoded.length);
        }

        byte[] toArray() {
            return out.toByteArray();
        }

        private static byte[] cp1252(String value) {
            try {
                CharsetEncoder encoder = Charset.forName("windows-1252").newEncoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)
                        .replaceWith(new byte[]{'?'});
                ByteBuffer encoded = encoder.encode(CharBuffer.wrap(value));
                byte[] bytes = new byte[encoded.remaining()];
                encoded.get(bytes);
                return bytes;
            } catch (Exception e) {
                throw new IllegalStateException("CP1252 encoding failed for: " + value, e);
            }
        }
    }
}
