package com.kraken.api.core.interceptor;

import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketFactory;
import com.kraken.api.core.packet.model.PacketMetadata;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.runelite.api.Client;

import javax.inject.Inject;

@Slf4j
@Singleton
public class CallStackInterceptor implements Interceptor {

    public final Client client;
    private final PacketMetadata packetMetadata;
    public boolean injected = false;

    @Inject
    public CallStackInterceptor(Client client) {
        this.client = client;
        this.packetMetadata = PacketFactory.getPacketMetadata();

        if (packetMetadata == null) {
            log.error("Failed to load packet metadata. Call stack patching will be unavailable.");
        }
    }

    public void injectHook() {
        if (injected) {
            log.debug("Call stack already injected, skipping");
            return;
        }

        if (packetMetadata == null || packetMetadata.getCallStackMethodName() == null || packetMetadata.getCleanCallStackValue() == null) {
            log.warn("Call stack mapping is unavailable, skipping injection");
            return;
        }

        try {
            Class<?> targetClass = resolveHookClass(packetMetadata.getCallStackClassName());

            new ByteBuddy()
                    .redefine(targetClass)
                    .visit(Advice.to(CallStackHookAdvice.class)
                            .on(
                                    ElementMatchers
                                            .named(packetMetadata.getCallStackMethodName())
                                            // TODO Assert in future client versions that the garbage value is always long and not another type.
                                            // .and(ElementMatchers.takesArguments(long.class))
                                            .and(ElementMatchers.returns(String.class))
                            )
                    )
                    .make()
                    .load(targetClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

            injected = true;
            log.info("CallStack hook load hooked into {}.{}",
                    packetMetadata.getCallStackClassName(),
                    packetMetadata.getCallStackMethodName());
        } catch (Exception e) {
            log.error("Failed to patch call stack interception", e);
        }
    }

    private Class<?> resolveHookClass(String className) throws ClassNotFoundException {
        if (client.getClass().getName().equals(className)) {
            return client.getClass();
        }

        return client.getClass().getClassLoader().loadClass(className);
    }

    public static class CallStackHookAdvice {

        @Advice.OnMethodExit
        public static void onExit(@Advice.Return(readOnly = false) String returnValue) {
            // Intercept and overwrite the native return string right before method returns
            if (PacketFactory.getPacketMetadata() != null) {
                returnValue = PacketFactory.getPacketMetadata().getCleanCallStackValue();
            }
        }
    }
}
