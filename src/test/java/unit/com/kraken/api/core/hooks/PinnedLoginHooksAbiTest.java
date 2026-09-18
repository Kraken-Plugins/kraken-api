package unit.com.kraken.api.core.hooks;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.LoginHooks;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offline ABI checks of the JX_* credential hooks against the injected client pinned by the test runtime dependencies. */
class PinnedLoginHooksAbiTest {

    @Test
    void credentialSettersAreStaticStringSetters() throws Exception {
        LoginHooks hooks = HooksLoader.getLoginHooks();

        assertStaticStringMethod("session", hooks.getSessionClassName(), hooks.getSessionMethodName(), void.class);
        assertStaticStringMethod("accountId", hooks.getAccountIdClassName(), hooks.getAccountIdMethodName(), void.class);
        assertStaticStringMethod("accessToken", hooks.getAccessTokenClassName(), hooks.getAccessTokenMethodName(), void.class);
        assertStaticStringMethod("refreshToken", hooks.getRefreshTokenClassName(), hooks.getRefreshTokenMethodName(), void.class);
    }

    @Test
    void credentialLookupReadsOneValueByName() throws Exception {
        LoginHooks hooks = HooksLoader.getLoginHooks();

        assertStaticStringMethod("credentialLookup", hooks.getCredentialLookupClassName(), hooks.getCredentialLookupMethodName(), String.class);
    }

    private static void assertStaticStringMethod(String hook, String className, String methodName, Class<?> returnType) throws Exception {
        assertNotNull(className, hook + " has no class name in hooks.json");
        assertNotNull(methodName, hook + " has no method name in hooks.json");

        Class<?> owner = Class.forName(className, false, PinnedLoginHooksAbiTest.class.getClassLoader());
        Method method = owner.getDeclaredMethod(methodName, String.class);
        assertTrue(Modifier.isStatic(method.getModifiers()), hook + " " + className + "." + methodName + " is not static");
        assertEquals(returnType, method.getReturnType(), hook + " " + className + "." + methodName + " return type");
    }
}
