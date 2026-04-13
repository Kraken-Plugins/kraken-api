package com.kraken.api.core.interceptor;

import com.kraken.api.core.interceptor.model.EncodedPacket;
import com.kraken.api.core.interceptor.model.PacketSent;
import com.kraken.api.core.packet.model.PacketFactory;
import com.kraken.api.core.packet.model.PacketMetadata;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class PacketInterceptor {
    public static final EventBus eventBus = RuneLite.getInjector().getInstance(EventBus.class);
    public static PacketInterceptor instance;
    public boolean injected = false;
    public Client client;


    @Inject
    public PacketInterceptor(Client client) {
        instance = this;
        this.client = client;
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
            log.debug("Already injected, skipping");
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
     * The Advice class injected directly into "addNode".
     */
    public static class PacketHookAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(0) Object packetBufferNode) {
            if (instance != null) {
                try {
                    EncodedPacket rawPacket = analyzePacket(packetBufferNode);
                    eventBus.post(new PacketSent(rawPacket));
                } catch (Throwable e) {
                    log.error("Error processing packet node", e);
                }
            }
        }
    }

    /**
     * Extracts the encrypted Packet id (not opcode), size, and byte array payload from the PacketBufferNode.
     * @param packetBufferNode The packet buffer node object.
     * @return EncodedPacket The encoded packet, containing the id (not opcode), size, and byte array payload.
     */
    public static EncodedPacket analyzePacket(Object packetBufferNode) {
        EncodedPacket rawPacket = new EncodedPacket();
        try {
            Class<?> nodeClass = packetBufferNode.getClass();
            Object clientPacket = null;
            Object packetBuffer = null;

            // Iterate through every declared field in the PacketBufferNode class
            for (Field field : nodeClass.getDeclaredFields()) {
                field.setAccessible(true);

                Class<?> fieldType = field.getType();
                String typeName = fieldType.getSimpleName();

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
                rawPacket.setEncodedId(values.get(0));
                rawPacket.setEncodedLength(values.get(1));
            }

            if (packetBuffer != null) {
                rawPacket.setPacketBufferNode(packetBuffer);

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
}