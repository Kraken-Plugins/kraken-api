package unit.com.kraken.api.core.hooks;

import com.kraken.api.core.hooks.HookResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that hooked methods are selected by exact name and structure, and that the resolver
 * refuses to guess when a class declares zero or several candidates.
 */
class HookResolverTest {

    /** Mirrors an obfuscated class: one hooked name reused across arities and widths, plus a case variant. */
    static class Obfuscated {
        static Class<?> lastGarbageWidth;

        private static void af(int value, byte garbage) {
            lastGarbageWidth = byte.class;
        }

        private static void af(int value, short garbage) {
            lastGarbageWidth = short.class;
        }

        private int af(int value) {
            return value;
        }

        private static void AF(int value, byte garbage) {
            lastGarbageWidth = Object.class;
        }
    }

    @Test
    void resolvesTheSingleStructuralMatchAndMakesItInvocable() throws Exception {
        Method method = HookResolver.requireUnique(Obfuscated.class, "af",
                m -> m.getParameterCount() == 2 && m.getParameterTypes()[1] == short.class);

        assertArrayEquals(new Class<?>[]{int.class, short.class}, method.getParameterTypes());
        method.invoke(null, 1, (short) 2);
        assertEquals(short.class, Obfuscated.lastGarbageWidth);
    }

    @Test
    void doesNotCountCaseVariantsOfTheHookedName() {
        Method method = HookResolver.requireUnique(Obfuscated.class, "af",
                m -> m.getParameterCount() == 2 && m.getParameterTypes()[1] == byte.class);

        assertEquals("af", method.getName());
    }

    @Test
    void failsWhenSeveralCandidatesMatch() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> HookResolver.requireUnique(Obfuscated.class, "af", m -> m.getParameterCount() == 2));

        assertTrue(thrown.getMessage().contains("found 2"), thrown.getMessage());
    }

    @Test
    void failsWhenNothingMatches() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> HookResolver.requireUnique(Obfuscated.class, "missing", m -> true));

        assertTrue(thrown.getMessage().contains("found 0"), thrown.getMessage());
    }
}
