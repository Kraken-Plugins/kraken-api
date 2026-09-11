package com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.model.BufferOperation;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketWrite;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketPayloadTest {
    @Test
    void bundledPacketWireBytesArePinned() {
        assertArrayEquals(new byte[]{1}, encode("EVENT_APPLET_FOCUS", 1, 1));
        assertArrayEquals(new byte[]{0, (byte) 147, 1, 4, (byte) 210, (byte) 222, 1},
                encode("EVENT_MOUSE_CLICK", 7, 1234, 350, 275, 0));
        assertArrayEquals(new byte[]{0, 15, 66, 64}, encode("RESUME_COUNTDIALOG", 4, 1_000_000));
        assertArrayEquals(new byte[]{3, (byte) 227}, encode("RESUME_OBJDIALOG", 2, 995));
        assertArrayEquals(new byte[]{5, 'T', 'e', 's', 't', 0}, encode("RESUME_STRINGDIALOG", -1, 5, "Test"));
        // MOVE_GAMECLICK: one-byte body length, encoded coordinates, modifier.
        assertArrayEquals(new byte[]{5, 18, 12, (byte) 255, 21, 12},
                encode("MOVE_GAMECLICK", -1, 3221, 3218, 1, 5));
    }

    @Test
    void cp1252LengthLimitsAndPrefixAreCheckedBeforeAllocation() {
        assertArrayEquals(new byte[]{5, 'a', (byte) 128, (byte) 153, '?', 0},
                encode("RESUME_STRINGDIALOG", -1, 5, "a\u20ac\u2122\u03c0"));
        assertEquals(256, encode("RESUME_STRINGDIALOG", -1, 255, "a".repeat(254)).length);
        assertThrows(IllegalArgumentException.class, () -> encode("RESUME_STRINGDIALOG", -1, 256, "a".repeat(255)));
        assertThrows(IllegalArgumentException.class, () -> encode("RESUME_STRINGDIALOG", -1, 4, "Test"));
        assertThrows(IllegalArgumentException.class, () -> encode("RESUME_STRINGDIALOG", -1, 4, "a\0b"));
        assertThrows(RuntimeException.class, () -> encode("RESUME_STRINGDIALOG", -1, 10001, "a".repeat(10000)));
    }

    @Test
    void missingOperandAndOmittedWritesAreRejected() {
        PacketDefinition empty = new PacketDefinition("RESUME_COUNTDIALOG", "test", new PacketWrite[0]);
        assertThrows(IllegalArgumentException.class, () -> PacketPayload.encode(empty, new Object[]{1}, 4));
        BufferOperation operation = new com.google.gson.Gson().fromJson("{\"type\":\"ADD\"}", BufferOperation.class);
        PacketDefinition invalid = new PacketDefinition("RESUME_COUNTDIALOG", "test", new PacketWrite[]{
                new PacketWrite("var0", "", new BufferOperation[]{operation})});
        assertThrows(IllegalStateException.class, () -> PacketPayload.encode(invalid, new Object[]{1}, 1));
    }

    @Test
    void fixedCapacityBoundariesAndTwoBytePrefixesAreValidated() {
        for (int size : new int[]{18, 19, 98, 99, 259}) {
            BufferOperation[] operations = new BufferOperation[size];
            java.util.Arrays.fill(operations, BufferOperation.raw());
            PacketDefinition definition = new PacketDefinition("RESUME_COUNTDIALOG", "test", new PacketWrite[]{
                    new PacketWrite("var0", "", operations)});
            assertEquals(size, PacketPayload.encode(definition, new Object[]{1}, size).length);
            assertThrows(IllegalArgumentException.class, () -> PacketPayload.encode(definition, new Object[]{1}, 260));
        }
        PacketDefinition variable = new PacketDefinition("RESUME_COUNTDIALOG", "test", new PacketWrite[]{
                new PacketWrite("var0", "", new BufferOperation[]{BufferOperation.rightShift(8), BufferOperation.raw(),
                        BufferOperation.subtract(2), BufferOperation.subtract(2)})});
        assertArrayEquals(new byte[]{0, 2, 0, 0}, PacketPayload.encode(variable, new Object[]{2}, -2));
        assertThrows(IllegalArgumentException.class, () -> PacketPayload.encode(variable, new Object[]{3}, -2));
        assertThrows(IllegalArgumentException.class, () -> PacketPayload.encode(variable, new Object[]{2}, -3));
    }

    private byte[] encode(String name, int length, Object... values) {
        return PacketPayload.encode(HooksLoader.getPackets().get(name), values, length);
    }
}
