package unit.com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import com.kraken.api.core.packet.BufferUtils;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.util.GarbageValueUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offline ABI checks against the injected client pinned by the test runtime dependencies. */
class PinnedPacketAbiTest {

    @Test
    void livePacketLengthsAndFactoryCapacitiesMatchPreflightContract() throws Exception {
        ReflectionHooks hooks = HooksLoader.getReflectionHooks();
        Class<?> packets = Class.forName(hooks.getClientPacketClassName());
        Class<?> nodeType = Class.forName(hooks.getPacketBufferNodeClassName());
        Class<?> writerType = Class.forName(hooks.getPacketWriterClassName());
        Field cipherField = writerType.getDeclaredField(hooks.getIsaacCipherFieldName());
        Constructor<?> cipherConstructor = cipherField.getType().getConstructor(int[].class);
        Method factory = Class.forName(hooks.getClassContainingPacketBufferNodeName()).getDeclaredMethod(
                hooks.getPacketBufferNodeFactoryMethodName(), packets, cipherField.getType(), byte.class);
        factory.setAccessible(true);
        Field lengthField = packets.getDeclaredField(hooks.getClientPacketLengthField());
        lengthField.setAccessible(true);
        Field bufferField = nodeType.getDeclaredField(hooks.getPacketBufferFieldName());
        bufferField.setAccessible(true);
        BufferUtils.validateFields(bufferField.getType());
        Method enqueue = writerType.getDeclaredMethod(hooks.getAddNodeMethodName(), nodeType, int.class);
        assertNotNull(enqueue);

        Map<String, Integer> lengths = Map.of("EVENT_APPLET_FOCUS", 1, "EVENT_MOUSE_CLICK", 7, "MOVE_GAMECLICK", -1,
                "RESUME_COUNTDIALOG", 4, "RESUME_OBJDIALOG", 2, "RESUME_STRINGDIALOG", -1);
        for (PacketDefinition definition : HooksLoader.getPackets().values()) {
            Field constant = packets.getDeclaredField(definition.getObfuscatedName());
            constant.setAccessible(true);
            Object packet = constant.get(null);
            int length = lengthField.getInt(packet) * hooks.getClientPacketLengthMultiplier();
            assertEquals(lengths.get(definition.getName()).intValue(), length, definition.getName());
            // This is a fresh private cipher. No client instance, connection or live writer is used.
            Object cipher = cipherConstructor.newInstance((Object) new int[]{1, 2, 3, 4});
            Object node = factory.invoke(null, packet, cipher,
                    GarbageValueUtils.coerceToParameterType(factory.getParameterTypes()[2], hooks.getPacketBufferNodeGarbageValue()));
            Object buffer = bufferField.get(node);
            assertEquals(1, BufferUtils.getOffset(buffer) * hooks.getIndexMultiplier());
            assertEquals(length < 0 ? 260 : 20, BufferUtils.getArray(buffer).length, definition.getName());
        }
    }

    @Test
    void nativeRevision241WritersProducePinnedBytesOffline() throws Exception {
        ReflectionHooks hooks = HooksLoader.getReflectionHooks();
        Class<?> bufferType = Class.forName(hooks.getExtendedBufferClassName());

        Method rd = bufferType.getDeclaredMethod("rd", bufferType, int.class, int.class);
        Method bz = bufferType.getDeclaredMethod("bz", int.class, int.class);
        Method oy = bufferType.getDeclaredMethod("oy", bufferType, int.class, byte.class);
        Method eo = bufferType.getDeclaredMethod("eo", int.class, byte.class);
        Method eh = bufferType.getDeclaredMethod("eh", int.class, int.class);
        Method ua = bufferType.getDeclaredMethod("ua", bufferType, int.class, int.class);
        Method cv = bufferType.getDeclaredMethod("cv", String.class, int.class);
        Method cl = bufferType.getDeclaredMethod("cl", int.class, int.class);

        assertTrue(Modifier.isStatic(rd.getModifiers()));
        assertTrue(!Modifier.isStatic(bz.getModifiers()));
        assertTrue(Modifier.isStatic(oy.getModifiers()));
        assertTrue(!Modifier.isStatic(eo.getModifiers()));
        assertTrue(!Modifier.isStatic(eh.getModifiers()));
        assertTrue(Modifier.isStatic(ua.getModifiers()));
        assertTrue(!Modifier.isStatic(cv.getModifiers()));
        assertTrue(!Modifier.isStatic(cl.getModifiers()));

        Constructor<?> ctor = bufferType.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);

        // EVENT_APPLET_FOCUS hasFocus=1 -> [1]
        Object focus = ctor.newInstance(32);
        rd.invoke(null, focus, 1, -1706893525);
        assertArrayEquals(new byte[]{1}, bytes(focus, hooks.getBufferArrayField(), hooks.getBufferOffsetField(), hooks.getIndexMultiplier()));

        // EVENT_MOUSE_CLICK info=1234, x=350, y=275, zero=0 -> [147,1,222,1,4,82,0]
        Object mouse = ctor.newInstance(32);
        bz.invoke(mouse, 275, -968225836);
        bz.invoke(mouse, 350, -968225836);
        oy.invoke(null, mouse, 1234, (byte) 46);
        eo.invoke(mouse, 0, (byte) -32);
        assertArrayEquals(new byte[]{(byte) 147, 1, (byte) 222, 1, 4, 82, 0},
                bytes(mouse, hooks.getBufferArrayField(), hooks.getBufferOffsetField(), hooks.getIndexMultiplier()));

        // MOVE_GAMECLICK x=3221, y=3218, ctrl=1, prefix=5 -> [5,129,12,18,12,21]
        Object move = ctor.newInstance(32);
        rd.invoke(null, move, 5, -1706893525);
        eh.invoke(move, 1, -671009295);
        oy.invoke(null, move, 3218, (byte) 46);
        oy.invoke(null, move, 3221, (byte) 46);
        assertArrayEquals(new byte[]{5, (byte) 129, 12, 18, 12, 21},
                bytes(move, hooks.getBufferArrayField(), hooks.getBufferOffsetField(), hooks.getIndexMultiplier()));

        // RESUME_COUNTDIALOG var0=1_000_000 -> [0,15,66,64]
        Object count = ctor.newInstance(32);
        ua.invoke(null, count, 1_000_000, -1816920624);
        assertArrayEquals(new byte[]{0, 15, 66, 64},
                bytes(count, hooks.getBufferArrayField(), hooks.getBufferOffsetField(), hooks.getIndexMultiplier()));

        // RESUME_STRINGDIALOG length=5 + "Test" CP1252 null terminated -> [5,'T','e','s','t',0]
        Object string = ctor.newInstance(64);
        rd.invoke(null, string, 5, -1706893525);
        cv.invoke(string, "Test", 512392066);
        assertArrayEquals(new byte[]{5, 'T', 'e', 's', 't', 0},
                bytes(string, hooks.getBufferArrayField(), hooks.getBufferOffsetField(), hooks.getIndexMultiplier()));

        // RESUME_OBJDIALOG var0=995 -> [3,227]
        Object obj = ctor.newInstance(32);
        cl.invoke(obj, 995, -328421806);
        assertArrayEquals(new byte[]{3, (byte) 227},
                bytes(obj, hooks.getBufferArrayField(), hooks.getBufferOffsetField(), hooks.getIndexMultiplier()));
    }

    @Test
    void packetFactoryGuardByteZeroPassesAndMinus111ThrowsForFixedAndVariableLengths() throws Exception {
        ReflectionHooks hooks = HooksLoader.getReflectionHooks();
        Class<?> packets = Class.forName(hooks.getClientPacketClassName());
        Class<?> writerType = Class.forName(hooks.getPacketWriterClassName());
        Field cipherField = writerType.getDeclaredField(hooks.getIsaacCipherFieldName());
        Constructor<?> cipherConstructor = cipherField.getType().getConstructor(int[].class);
        Method factory = Class.forName(hooks.getClassContainingPacketBufferNodeName()).getDeclaredMethod(
                hooks.getPacketBufferNodeFactoryMethodName(), packets, cipherField.getType(), byte.class);
        factory.setAccessible(true);
        Object cipher = cipherConstructor.newInstance((Object) new int[]{1, 2, 3, 4});

        Field fixedField = packets.getDeclaredField(HooksLoader.getPackets().get("EVENT_APPLET_FOCUS").getObfuscatedName());
        Field variableField = packets.getDeclaredField(HooksLoader.getPackets().get("MOVE_GAMECLICK").getObfuscatedName());
        fixedField.setAccessible(true);
        variableField.setAccessible(true);
        Object fixedPacket = fixedField.get(null);
        Object variablePacket = variableField.get(null);

        assertNotNull(factory.invoke(null, fixedPacket, cipher, (byte) 0));
        assertNotNull(factory.invoke(null, variablePacket, cipher, (byte) 0));

        assertThrows(InvocationTargetException.class, () -> factory.invoke(null, fixedPacket, cipher, (byte) -111));
        assertThrows(InvocationTargetException.class, () -> factory.invoke(null, variablePacket, cipher, (byte) -111));
    }

    private static byte[] bytes(Object buffer, String arrayField, String offsetField, int indexMultiplier) throws Exception {
        Field data = buffer.getClass().getField(arrayField);
        Field offset = buffer.getClass().getField(offsetField);
        int size = offset.getInt(buffer) * indexMultiplier;
        byte[] src = (byte[]) data.get(buffer);
        return Arrays.copyOf(src, size);
    }
}
