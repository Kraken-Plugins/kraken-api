package com.kraken.api.core.interceptor;

import com.kraken.api.core.interceptor.model.PacketSent;
import com.kraken.api.core.packet.BufferUtils;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketFactory;
import com.kraken.api.core.packet.model.PacketMetadata;
import com.kraken.api.core.packet.model.PacketWrite;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.*;


@Slf4j
@Singleton
public class PacketInterceptor {
    public static final EventBus eventBus = RuneLite.getInjector().getInstance(EventBus.class);
    public static PacketInterceptor instance;
    public boolean injected = false;
    public Client client;
    public PacketFactory packetFactory;

    private volatile PendingActionTemplate pendingAction = null;

    @Inject
    public PacketInterceptor(Client client, PacketFactory packetFactory) {
        instance = this;
        this.client = client;
        this.packetFactory = packetFactory;
        eventBus.register(this);
    }

    /**
     * Modifies the bytecode of the "addNode" method within the client at runtime to invoke
     * the {@link PacketHookAdvice} class whenever the method is called. This will publish
     * the {@link PacketSent} event to the eventbus which can be {@link net.runelite.client.eventbus.Subscribe}
     * to within plugins who need access to low level packets.
     * @throws Exception Throws an Illegal state exception if the hook is not able to be injected.
     */
    public void injectHook() throws Exception {
        if(injected) {
            log.info("Already injected, skipping");
            return;
        }

        if(PacketFactory.getPacketMetadata() == null) {
            log.error("No PacketMetadata found. Cannot inject PacketInterceptor");
            return;
        }
        PacketMetadata metadata = PacketFactory.getPacketMetadata();

        Class<?> packetWriterClass = client.getClass()
                .getClassLoader()
                .loadClass(metadata.getPacketWriterClassName());

        new ByteBuddy()
                .redefine(packetWriterClass)
                .visit(Advice.to(PacketHookAdvice.class).on(ElementMatchers.named(metadata.getAddNodeMethodName())))
                .make()
                .load(packetWriterClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

        injected = true;
        log.info("Packet interception (addNode) hooked into: {}.{}", metadata.getPacketWriterClassName(), metadata.getAddNodeMethodName());
    }

    /**
     * Seeds the interceptor with expected values right before the packet is built.
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuAction() == MenuAction.GAME_OBJECT_FIRST_OPTION) {
            Map<String, Integer> expectedArgs = new HashMap<>();
            expectedArgs.put("objectId", event.getId());
            expectedArgs.put("worldPointX", event.getParam0());
            expectedArgs.put("worldPointY", event.getParam1());
            expectedArgs.put("ctrlDown", client.isKeyPressed(KeyCode.KC_CONTROL) ? 1 : 0);
            expectedArgs.put("subop", 0);

            PacketDefinition def = packetFactory.getOpObj1();

            log.info("OPOBJ1 Template: {}", expectedArgs);

            pendingAction = new PendingActionTemplate("OPOBJ1", expectedArgs, Arrays.asList(def.getWrites()), client.getTickCount());
            log.info("Primed template for OPOBJ1. Waiting for packet...");
        }
    }

    /**
     * The Advice class injected directly into "addNode".
     */
    public static class PacketHookAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(0) Object packetBufferNode) {
            if (instance != null) {
                try {
                    instance.processPacket(packetBufferNode);
                } catch (Throwable e) {
                    log.error("Error processing packet node", e);
                }
            }
        }
    }

    /**
     * Handles the routing between known packets and brute-forcing unknown ones.
     */
    public void processPacket(Object packetBufferNode) {
        RawPacket rawPacket = analyzePacket(packetBufferNode);

        PendingActionTemplate template = pendingAction;
        if (template != null) {
            // Expire templates older than 600ms (1 tick) to avoid mismatching unrelated packets
            if (client.getTickCount() != template.tick) {
                pendingAction = null;
                return;
            }

            log.info("Attempting decode of: {}", rawPacket.getEncryptedOpCode());
            attemptLinearDecodeAndMatch(rawPacket, template);
        }
    }

    /**
     * Extracts the encrypted Packet Opcode, size, and byte array payload from the PacketBufferNode.
     */
    public RawPacket analyzePacket(Object packetBufferNode) {
        RawPacket rawPacket = new RawPacket();
        try {
            Class<?> nodeClass = packetBufferNode.getClass();
            Object clientPacket = null; // iq.ap
            Object packetBuffer = null; // wg.ai

            // Iterate through every declared field in the PacketBufferNode class
            for (Field field : nodeClass.getDeclaredFields()) {
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                String typeName = fieldType.getSimpleName();

                // METHOD 1: If you know the current obfuscated class names ("iq", "wg")
                if (typeName.equals(PacketFactory.getPacketMetadata().getClientPacketClassName())) {
                    clientPacket = field.get(packetBufferNode);
                } else if (typeName.equals(PacketFactory.getPacketMetadata().getBufferClassName())) {
                    packetBuffer = field.get(packetBufferNode); // wg extends wj implements PacketBuffer
                }
            }

            if (clientPacket != null) {
                // 2 fields opcode and size
                List<Integer> values = new ArrayList<>();
                for (Field clientPacketField : clientPacket.getClass().getDeclaredFields()) {
                    clientPacketField.setAccessible(true);

                    if(clientPacketField.getType() == int.class) {
                        int value = (int) clientPacketField.get(clientPacket);
                        values.add(value);
                    }
                }
                rawPacket.setEncryptedOpCode(values.get(0));
                rawPacket.setEncryptedSize(values.get(1));
            }

            if (packetBuffer != null) {
                rawPacket.setBufferInstance(packetBuffer);

                // Scan for the byte[] array inside the buffer just like we did above
                for (Field bufferField : packetBuffer.getClass().getSuperclass().getDeclaredFields()) {
                    bufferField.setAccessible(true);
                    if(bufferField.getType() == byte[].class) {
                        byte[] packetPayload = (byte[]) bufferField.get(packetBuffer);
                        rawPacket.setPayload(packetPayload);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error while analyzing PacketBufferNode:", e);
        }

        return rawPacket;
    }

    private void attemptLinearDecodeAndMatch(RawPacket rawPacket, PendingActionTemplate template) {
        Object bufferInstance = rawPacket.getBufferInstance();
        int originalOffset = BufferUtils.getOffset(bufferInstance);

        // The strictly ordered writes from your definition
        List<PacketWrite> orderedWrites = template.getWrites();
        // A modifiable copy of your expected args (objectId, sceneX, sceneY, etc.)
        Map<String, Integer> availableExpectedArgs = new HashMap<>(template.getExpectedValues());

        try {
            BufferUtils.setOffset(bufferInstance, 0);
            List<Integer> decodedValues = new ArrayList<>();

            // 1. Read the buffer sequentially using the known order of operations
            for (PacketWrite write : orderedWrites) {
                int value = BufferUtils.readInt(write.getOperations(), bufferInstance);
                decodedValues.add(value);
            }

            log.info("--- Packet Mapping Report for {} ---", template.getPacketName());

            // 2. Try each decoded value against the expected args to figure out the mapping
            for (int i = 0; i < decodedValues.size(); i++) {
                int decodedValue = decodedValues.get(i);
                PacketWrite currentWrite = orderedWrites.get(i);

                String matchedKey = null;

                // Search the expected args to see if this decoded value matches any of them
                for (Map.Entry<String, Integer> entry : availableExpectedArgs.entrySet()) {
                    if (entry.getValue() == decodedValue) {
                        matchedKey = entry.getKey();
                        break;
                    }
                }

                if (matchedKey != null) {
                    log.info("[MAPPED] writes[{}] -> '{}' (Value: {})", i, matchedKey, decodedValue);

                    // Assign the semantic parameter name directly to the write object!
                    currentWrite.setParam(matchedKey);

                    // Remove it from the available pool so we don't map two writes to the same expected arg
                    availableExpectedArgs.remove(matchedKey);
                } else {
                    log.warn("[UNMAPPED] writes[{}] decoded to {}, but no expected arg matched this value.", i, decodedValue);
                }
            }

            // 3. Report any expected args that we never found a match for
            if (!availableExpectedArgs.isEmpty()) {
                log.warn("--- Missing Expected Args ---");
                for (Map.Entry<String, Integer> missing : availableExpectedArgs.entrySet()) {
                    log.warn("Expected to find '{}' with value: {}, but it was nowhere in the decoded payload.",
                            missing.getKey(), missing.getValue());
                }
            }

            log.info("------------------------------------------");

        } catch (Exception e) {
            log.error("Failed to read buffer sequentially. The write array length or operations might be incorrect.", e);
        } finally {
            // Always restore the buffer state so the client isn't corrupted
            BufferUtils.setOffset(bufferInstance, originalOffset);
        }
    }

    @Data
    @AllArgsConstructor
    private static class MatchCandidate {
        private List<PacketWrite> permutation;
        private List<Integer> decodedValues;
        private int score;
    }

    @Data
    @NoArgsConstructor
    public static class RawPacket {
        private Object bufferInstance;
        private byte[] payload;
        private int encryptedOpCode;
        private int encryptedSize;
    }

    @Data
    @AllArgsConstructor
    private static class PendingActionTemplate {
        private String packetName;
        private Map<String, Integer> expectedValues;
        private List<PacketWrite> writes;
        private int tick;
    }
}