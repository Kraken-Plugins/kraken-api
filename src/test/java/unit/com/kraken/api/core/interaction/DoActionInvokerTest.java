package com.kraken.api.core.interaction;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.ReflectionHooks;
import net.runelite.api.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the engine hook is selected by exact name and structure and that the configured
 * garbage value is passed verbatim at the declared width, never defaulted.
 */
class DoActionInvokerTest {

    private static final int GARBAGE = 242848078;

    private ReflectionHooks original;
    private DoActionInvoker invoker;

    /** The hooked engine method alongside a same-named overload of an unrelated shape. */
    public static class Engine {
        static Object[] received;

        public static void ef(int param0, int param1, int opcode, int identifier, int itemId, int worldView,
                              String option, String target, int canvasX, int canvasY, byte garbage) {
            received = new Object[]{param0, param1, opcode, identifier, itemId, worldView, option, target, canvasX, canvasY, garbage};
        }

        public int ef(int unrelated) {
            return unrelated;
        }
    }

    public static class CaseVariantEngine {
        static boolean called;

        public static void EF(int param0, int param1, int opcode, int identifier, int itemId, int worldView,
                              String option, String target, int canvasX, int canvasY, byte garbage) {
            called = true;
        }
    }

    public static class AmbiguousEngine {
        static boolean called;

        public static void ef(int param0, int param1, int opcode, int identifier, int itemId, int worldView,
                              String option, String target, int canvasX, int canvasY, byte garbage) {
            called = true;
        }

        public static void ef(int param0, int param1, int opcode, int identifier, int itemId, int worldView,
                              String option, String target, int canvasX, int canvasY, short garbage) {
            called = true;
        }
    }

    /** Declarations exercised by the signature predicate alone; none are ever invoked. */
    public static class Shapes {
        public static void withGarbage(int a, int b, int c, int d, int e, int f, String g, String h, int i, int j, byte k) {}
        public static void withoutGarbage(int a, int b, int c, int d, int e, int f, String g, String h, int i, int j) {}
        public void instance(int a, int b, int c, int d, int e, int f, String g, String h, int i, int j, byte k) {}
        public static void objectGarbage(int a, int b, int c, int d, int e, int f, String g, String h, int i, int j, Object k) {}
        public static void tooMany(int a, int b, int c, int d, int e, int f, String g, String h, int i, int j, byte k, byte l) {}
        public static void wrongLeadingType(long a, int b, int c, int d, int e, int f, String g, String h, int i, int j) {}
    }

    @BeforeEach
    void setup() throws Exception {
        original = HooksLoader.getReflectionHooks();
        Engine.received = null;
        CaseVariantEngine.called = false;
        AmbiguousEngine.called = false;

        Client client = mock(Client.class);
        Context context = mock(Context.class);
        when(context.getClient()).thenReturn(client);
        when(context.runOnClientThreadOptional(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return Optional.ofNullable(callable.call());
        });

        invoker = new DoActionInvoker();
        Field provider = DoActionInvoker.class.getDeclaredField("ctxProvider");
        provider.setAccessible(true);
        provider.set(invoker, (Provider<Context>) () -> context);
    }

    @AfterEach
    void restore() throws Exception {
        install(original);
    }

    @Test
    void invokesTheStructuralMatchWithTheConfiguredGarbageAtDeclaredWidth() throws Exception {
        install(Engine.class, "ef", GARBAGE);

        assertTrue(invoke());
        assertNotNull(Engine.received);
        assertEquals("Attack", Engine.received[6]);
        assertEquals((byte) GARBAGE, Engine.received[10]);
    }

    @Test
    void ignoresCaseVariantsOfTheHookedName() throws Exception {
        install(CaseVariantEngine.class, "ef", GARBAGE);

        assertFalse(invoke());
        assertFalse(CaseVariantEngine.called);
    }

    @Test
    void refusesWhenSeveralOverloadsShareTheHookedShape() throws Exception {
        install(AmbiguousEngine.class, "ef", GARBAGE);

        assertFalse(invoke());
        assertFalse(AmbiguousEngine.called);
    }

    @Test
    void refusesToInvokeWhenTheGarbageValueIsMissing() throws Exception {
        install(Engine.class, "ef", null);

        assertFalse(invoke());
        assertNull(Engine.received);
    }

    @Test
    void signatureAcceptsOnlyStaticMethodsWithTheEngineParameterLayout() {
        assertTrue(DoActionInvoker.isDoActionSignature(shape("withGarbage")));
        assertTrue(DoActionInvoker.isDoActionSignature(shape("withoutGarbage")));
        assertFalse(DoActionInvoker.isDoActionSignature(shape("instance")));
        assertFalse(DoActionInvoker.isDoActionSignature(shape("objectGarbage")));
        assertFalse(DoActionInvoker.isDoActionSignature(shape("tooMany")));
        assertFalse(DoActionInvoker.isDoActionSignature(shape("wrongLeadingType")));
    }

    private boolean invoke() {
        return invoker.invoke(1, 2, 3, 4, 5, 6, "Attack", "Goblin", 7, 8);
    }

    private static Method shape(String name) {
        return Arrays.stream(Shapes.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private void install(Class<?> engine, String methodName, Integer garbageValue) throws Exception {
        Gson gson = new Gson();
        JsonObject hooks = gson.toJsonTree(original).getAsJsonObject();
        hooks.addProperty("doActionClassName", engine.getName());
        hooks.addProperty("doActionMethodName", methodName);
        if (garbageValue == null) {
            hooks.remove("doActionGarbageValue");
        } else {
            hooks.addProperty("doActionGarbageValue", garbageValue);
        }
        install(gson.fromJson(hooks, ReflectionHooks.class));
    }

    private static void install(ReflectionHooks hooks) throws Exception {
        Field field = HooksLoader.class.getDeclaredField("reflectionHooks");
        field.setAccessible(true);
        field.set(null, hooks);
    }
}
