package com.kraken.api.core.interceptor;

import com.google.inject.Singleton;
import com.kraken.api.service.util.reflect.hooks.HookRegistry;
import com.kraken.api.service.util.reflect.hooks.MouseHooks;
import com.kraken.api.service.util.reflect.hooks.loader.HookLoader;
import com.kraken.api.service.util.reflect.hooks.model.MethodHook;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import net.runelite.api.Client;

import javax.inject.Inject;

@Slf4j
@Singleton
public class MouseHookInterceptor {

    public final Client client;
    public MouseHooks hooks;
    public boolean injected = false;

    @Inject
    public MouseHookInterceptor(Client client) {
        this.client = client;

        MouseHooks loadedHooks = null;
        try {
            HookRegistry registry = HookLoader.load();

            if(registry == null) {
                log.error("Failed to load mouse hook from registry. Cannot inject mouse hook DLL patch.");
                return;
            }

            loadedHooks = registry.getMouse();
        } catch (Exception e) {
            log.error("Failed to load reflection hooks. Mouse hook patching will be unavailable.", e);
        }

        this.hooks = loadedHooks;
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

        if (hooks == null || hooks.getMouseHook() == null) {
            log.warn("Mouse hook reflection mapping is unavailable, skipping injection");
            return;
        }

        try {
            MethodHook mouseHook = hooks.getMouseHook();
            Class<?> targetClass = resolveHookClass(mouseHook);

            new ByteBuddy()
                    .redefine(targetClass)
                    // Replace the field read with our custom method call
                    .visit(MemberSubstitution.strict()
                            .field(ElementMatchers.named("llimc"))
                            .onRead()
                            .replaceWith(MouseHookInterceptor.class.getMethod("provideZero"))
                            .on(ElementMatchers.named(mouseHook.getMethodName())))
                    .make()
                    .load(targetClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

            injected = true;
            log.info("Mouse hook DLL load patched into {}.{}", mouseHook.getClassName(), mouseHook.getMethodName());
        } catch (Exception e) {
            log.error("Failed to patch mouse hook interception", e);
        }
    }

    private Class<?> resolveHookClass(MethodHook mouseHook) throws ClassNotFoundException {
        if (client.getClass().getName().equals(mouseHook.getClassName())) {
            return client.getClass();
        }

        return client.getClass().getClassLoader().loadClass(mouseHook.getClassName());
    }

    /**
     * Delegate method injected by ByteBuddy.
     * Whenever the client tries to read 'llimc', it will call this instead.
     */
    public static int provideZero() {
        return 0;
    }
}
