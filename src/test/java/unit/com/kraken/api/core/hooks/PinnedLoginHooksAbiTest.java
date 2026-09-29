package unit.com.kraken.api.core.hooks;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.LoginHooks;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

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

    @Test
    void mouseTimestampAndClientMillisFieldsRoundTripWithHookMultipliers() throws Exception {
        var reflection = HooksLoader.getReflectionHooks();
        ClassLoader loader = PinnedLoginHooksAbiTest.class.getClassLoader();
        Class<?> mouseClass = Class.forName(reflection.getMouseHandlerLastPressedClass(), false, loader);
        Class<?> clientClass = Class.forName("client", false, loader);

        Field mouseField = mouseClass.getDeclaredField(reflection.getMouseHandlerLastPressedField());
        Field millisField = clientClass.getDeclaredField(reflection.getClientMillisField());
        mouseField.setAccessible(true);
        millisField.setAccessible(true);

        long oldMouse = mouseField.getLong(null);
        long oldMillis = millisField.getLong(null);
        try {
            long logicalMouse = 123_456_789L;
            long logicalMillis = 98_765_432L;
            long mouseInverse = modInverse64(reflection.getMouseHandlerMultiplier());
            long millisInverse = modInverse64(reflection.getClientMillisMultiplier());

            mouseField.setLong(null, logicalMouse * mouseInverse);
            millisField.setLong(null, logicalMillis * millisInverse);

            assertEquals(logicalMouse, mouseField.getLong(null) * reflection.getMouseHandlerMultiplier());
            assertEquals(logicalMillis, millisField.getLong(null) * reflection.getClientMillisMultiplier());
        } finally {
            mouseField.setLong(null, oldMouse);
            millisField.setLong(null, oldMillis);
        }
    }

    @Test
    void loginIndexSetterAeUpdatesDecodedArCqAndStateIsRestored() throws Exception {
        LoginHooks hooks = HooksLoader.getLoginHooks();
        ClassLoader loader = PinnedLoginHooksAbiTest.class.getClassLoader();
        Class<?> loginClass = Class.forName(hooks.getLoginIndexClassName(), false, loader);
        Class<?> displayClass = Class.forName(hooks.getDisplayNameClassName(), false, loader);

        Method setter = loginClass.getDeclaredMethod(hooks.getLoginIndexMethodName(), int.class, int.class);
        setter.setAccessible(true);
        Field cq = displayClass.getDeclaredField("cq");
        cq.setAccessible(true);

        int original = cq.getInt(null);
        try {
            setter.invoke(null, 7, hooks.getLoginIndexGarbageValue());
            int decoded = cq.getInt(null) * -1568212415;
            assertEquals(7, decoded);
        } finally {
            cq.setInt(null, original);
        }
    }

    private static void assertStaticStringMethod(String hook, String className, String methodName, Class<?> returnType) throws Exception {
        assertNotNull(className, hook + " has no class name in hooks.json");
        assertNotNull(methodName, hook + " has no method name in hooks.json");

        Class<?> owner = Class.forName(className, false, PinnedLoginHooksAbiTest.class.getClassLoader());
        Method method = owner.getDeclaredMethod(methodName, String.class);
        assertTrue(Modifier.isStatic(method.getModifiers()), hook + " " + className + "." + methodName + " is not static");
        assertEquals(returnType, method.getReturnType(), hook + " " + className + "." + methodName + " return type");
    }

    private static long modInverse64(long oddValue) {
        BigInteger modulus = BigInteger.ONE.shiftLeft(64);
        BigInteger unsigned = BigInteger.valueOf(oddValue).and(modulus.subtract(BigInteger.ONE));
        return unsigned.modInverse(modulus).longValue();
    }
}
