package com.kraken.api.core.packet;

import com.kraken.api.core.packet.model.PacketData;
import com.kraken.api.core.packet.model.PacketSent;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;

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
    }

    /**
     * The Advice class. This code is injected DIRECTLY into the start of the "addNode" method.
     * It must be public and static.
     */
    public static class PacketHookAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(0) Object packetBufferNode) {
            if (instance != null) {
                try {
                    PacketData data = PacketBufferReader.readPacketBuffer(packetBufferNode);
                    eventBus.post(new PacketSent(packetBufferNode, data));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Modifies the bytecode of the "addNode" method within the client at runtime to invoke
     * the {@link PacketHookAdvice} class whenever the method is called. This will publish
     * the {@link PacketSent} event to the eventbus which can be {@link net.runelite.client.eventbus.Subscribe}
     * to within plugins who need access to low level packets.
     * @throws Exception
     */
    public void injectHook() throws Exception {
        if(injected) {
            log.info("Already injected, skipping");
            return;
        }

        // Install the ByteBuddy Agent to the current JVM
        // This gives us permission to redefine loaded classes
        try {
            ByteBuddyAgent.install();
        } catch (IllegalStateException e) {
            log.warn("Agent already installed or failed: " + e.getMessage());
        }

        Field packetWriterField = client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        packetWriterField.setAccessible(true);
        Object writerInstance = packetWriterField.get(null);

        if (writerInstance == null) throw new IllegalStateException("PacketWriter is null");
        Class<?> packetWriterClass = writerInstance.getClass();

        // Redefine the class in memory by patch the bytecode of the existing class.
        new ByteBuddy()
                .redefine(packetWriterClass)
                .visit(Advice.to(PacketHookAdvice.class).on(ElementMatchers.named(ObfuscatedNames.addNodeMethodName)))
                .make()
                .load(packetWriterClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

        injected = true;
        log.info("Packet interception hooked");
    }
}