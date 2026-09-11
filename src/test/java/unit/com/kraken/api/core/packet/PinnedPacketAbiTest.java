package com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.util.GarbageValueUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
}
