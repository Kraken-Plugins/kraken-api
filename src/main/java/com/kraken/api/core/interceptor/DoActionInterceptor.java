package com.kraken.api.core.interceptor;

import com.kraken.api.service.util.reflect.hooks.DoActionHooks;
import com.kraken.api.service.util.reflect.hooks.HookRegistry;
import com.kraken.api.service.util.reflect.hooks.loader.HookLoader;
import com.kraken.api.service.util.reflect.hooks.model.MethodHook;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class DoActionInterceptor {

    public final Client client;
    public final DoActionHooks hooks;
    public boolean injected = false;

    @Inject
    public DoActionInterceptor(Client client) {
        this.client = client;

        DoActionHooks loadedHooks = null;
        try {
            HookRegistry registry = HookLoader.load();
            loadedHooks = registry.getDoActionHooks();
        } catch (Exception e) {
            log.error("Failed to load reflection hooks. doAction mapping will be unavailable.", e);
        }

        this.hooks = loadedHooks;
    }

    /**
     * Redefines the obfuscated client doAction method to log arguments on entry
     * without interrupting the original execution.
     */
    public void injectHook() {
        if (injected) {
            log.debug("doAction hook already injected, skipping");
            return;
        }

        if (hooks == null || hooks.getDoAction() == null) {
            log.warn("doAction hook reflection mapping is unavailable, skipping injection");
            return;
        }

        try {
            MethodHook doActionHook = hooks.getDoAction();
            Class<?> targetClass = resolveHookClass(doActionHook);

            new ByteBuddy()
                    .redefine(targetClass)
                    .visit(Advice.to(DoActionAdvice.class).on(ElementMatchers.named(doActionHook.getMethodName())))
                    .make()
                    .load(targetClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

            injected = true;
            log.info("doAction mapping patched into {}.{}", doActionHook.getClassName(), doActionHook.getMethodName());
        } catch (Exception e) {
            log.error("Failed to patch doAction interception", e);
        }
    }

    private Class<?> resolveHookClass(MethodHook doActionHook) throws ClassNotFoundException {
        if (client.getClass().getName().equals(doActionHook.getClassName())) {
            return client.getClass();
        }

        return client.getClass().getClassLoader().loadClass(doActionHook.getClassName());
    }

    /**
     * Advice injected into the obfuscated doAction method.
     * We do NOT skip the method here; we just snoop the parameters for mapping.
     */
    public static class DoActionAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(
                @Advice.Argument(0) int p0,
                @Advice.Argument(1) int p1,
                @Advice.Argument(2) int opcode,
                @Advice.Argument(3) int id,
                @Advice.Argument(4) int p4,
                @Advice.Argument(5) int p5,
                @Advice.Argument(6) String option,
                @Advice.Argument(7) String target,
                @Advice.Argument(8) int p8,
                @Advice.Argument(9) int p9,
                @Advice.Argument(10) int p10) {

            // Using System.out is safer inside Advice as SLF4J might not be
            // accessible depending on the target class's classloader hierarchy.
            System.out.printf("[MAPPER] doAction -> Op: %s | Target: %s | Opcode: %d | ID: %d | P0: %d | P1: %d | P4: %d | P5: %d | P8: %d | P9: %d | P10: %d\n",
                    option, target, opcode, id, p0, p1, p4, p5, p8, p9, p10);
        }
    }
}