package com.kraken.api.plugins.packetmapper;

import com.kraken.api.core.packet.ObfuscatedNames;
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

    @Inject
    private Client client;

    public static final EventBus eventBus = RuneLite.getInjector().getInstance(EventBus.class);
    public static PacketInterceptor instance;
    public volatile boolean isIntercepting = false;
    public boolean injected = false;

    public PacketInterceptor() {
        instance = this;
    }

    /**
     * The Advice class. This code is injected DIRECTLY into the start of the 'ah' method.
     * It must be public and static.
     */
    public static class PacketHookAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(0) Object packetBufferNode) {
            if (instance != null && instance.isIntercepting) {
                try {
                    eventBus.post(new PacketSent(packetBufferNode));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startInterception() throws Exception {
        if (isIntercepting) return;

        if(injected) {
            log.info("Already injected, skipping");
            return;
        }

        // 1. Install the ByteBuddy Agent to the current JVM
        // This gives us permission to redefine loaded classes
        try {
            ByteBuddyAgent.install();
        } catch (IllegalStateException e) {
            log.warn("Agent already installed or failed: " + e.getMessage());
        }

        // 2. Identify the target class (dh) TODO Exists on the client this revision (236), could exist in packet writer other revisions
        Field packetWriterField = client.getClass().getDeclaredField(ObfuscatedNames.packetWriterFieldName);
        packetWriterField.setAccessible(true);
        Object writerInstance = packetWriterField.get(null);

        if (writerInstance == null) throw new IllegalStateException("PacketWriter is null");
        Class<?> packetWriterClass = writerInstance.getClass();

        log.info("Redefining class: {}", packetWriterClass.getName());

        // Redefine the class in memory by patch the bytecode of the existing class.
        new ByteBuddy()
                .redefine(packetWriterClass)
                .visit(Advice.to(PacketHookAdvice.class).on(ElementMatchers.named(ObfuscatedNames.addNodeMethodName)))
                .make()
                .load(packetWriterClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

        isIntercepting = true;
        injected = true;
        log.info("Packet interception hooked");
    }

    public void stopInterception() {
        isIntercepting = false;
        log.info("Packet interception paused");
    }
}