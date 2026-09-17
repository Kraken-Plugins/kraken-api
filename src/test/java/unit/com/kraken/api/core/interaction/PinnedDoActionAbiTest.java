package com.kraken.api.core.interaction;

import com.kraken.api.core.hooks.HookResolver;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline ABI check of the doAction hook against the injected client pinned by the test runtime
 * dependencies. Compiling against RuneLite does not validate obfuscated handles, so this is the
 * gate that catches a hooks file that no longer matches the pinned client.
 */
class PinnedDoActionAbiTest {

    @Test
    void doActionHookResolvesToTheSingleStaticEngineMethod() throws Exception {
        ReflectionHooks hooks = HooksLoader.getReflectionHooks();
        Class<?> owner = Class.forName(hooks.getDoActionClassName());

        Method doAction = HookResolver.requireUnique(owner, hooks.getDoActionMethodName(), DoActionInvoker::isDoActionSignature);

        assertTrue(Modifier.isStatic(doAction.getModifiers()));
        assertEquals(11, doAction.getParameterCount());
        assertEquals(byte.class, doAction.getParameterTypes()[10]);
        assertNotNull(hooks.getDoActionGarbageValue(), "the pinned signature declares a garbage parameter");
    }
}
