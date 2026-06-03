package com.kraken.api.core.interceptor;

import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketFactory;
import com.kraken.api.core.packet.model.PacketMetadata;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.runelite.api.Client;

import javax.inject.Inject;

@Slf4j
@Singleton
public class MouseHookInterceptor implements Interceptor {

    public final Client client;
    private final PacketMetadata packetMetadata;
    public boolean injected = false;

    @Inject
    public MouseHookInterceptor(Client client) {
        this.client = client;
        this.packetMetadata = PacketFactory.getPacketMetadata();

        if (packetMetadata == null) {
            log.error("Failed to load packet metadata. Mouse hook patching will be unavailable.");
        }
    }

    /**
     * Redefines the obfuscated client mouse hook method so that any read of the
     * 'llimc' field returns 0, leaving the rest of the packet building logic intact.
     */
    public void injectHook() {
        if (injected) {
            log.debug("Mouse hook already injected, skipping");
            return;
        }

        if (packetMetadata == null
                || packetMetadata.getMouseHookDllClassName() == null
                || packetMetadata.getMouseHookDllMethodName() == null) {
            log.warn("Mouse hook reflection mapping is unavailable, skipping injection");
            return;
        }

        try {
            Class<?> targetClass = resolveHookClass(packetMetadata.getMouseHookDllClassName());

            new ByteBuddy()
                    .redefine(targetClass)
                    // Replace the field read with our custom method call
                    .visit(MemberSubstitution.strict()
                            .field(ElementMatchers.named("llimc"))
                            .onRead()
                            .replaceWith(MouseHookInterceptor.class.getMethod("provideZero"))
                            .on(ElementMatchers.named(packetMetadata.getMouseHookDllMethodName())))
                    .make()
                    .load(targetClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

            injected = true;
            log.info("Mouse hook DLL load hooked into {}.{}",
                    packetMetadata.getMouseHookDllClassName(),
                    packetMetadata.getMouseHookDllMethodName());
        } catch (Exception e) {
            log.error("Failed to patch mouse hook interception", e);
        }
    }

    private Class<?> resolveHookClass(String className) throws ClassNotFoundException {
        if (client.getClass().getName().equals(className)) {
            return client.getClass();
        }

        return client.getClass().getClassLoader().loadClass(className);
    }

    /**
     * Delegate method injected by ByteBuddy.
     * Whenever the client tries to read 'llimc', it will call this instead.
     * @return hardcoded 0 value.
     */
    public static int provideZero() {
        return 0;
    }
}
