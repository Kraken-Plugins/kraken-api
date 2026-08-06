package unit.com.kraken.api.util;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.util.GarbageValueUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GarbageValueUtilsTest {

    @Test
    void coercesToEachSupportedPrimitiveWidth() {
        assertEquals((byte) -111, GarbageValueUtils.coerceToParameterType(byte.class, -111));
        assertEquals((short) -111, GarbageValueUtils.coerceToParameterType(short.class, -111));
        assertEquals(-111, GarbageValueUtils.coerceToParameterType(int.class, -111));
        assertEquals(-111L, GarbageValueUtils.coerceToParameterType(long.class, -111));
    }

    @Test
    void narrowingKeepsTheLowOrderBitsExactlyLikeAutoNarrowing() {
        int value = -1301097035;
        assertEquals((byte) value, GarbageValueUtils.coerceToParameterType(byte.class, value));
        assertEquals((short) value, GarbageValueUtils.coerceToParameterType(short.class, value));
        assertEquals(value, GarbageValueUtils.coerceToParameterType(int.class, value));
        assertEquals((long) value, GarbageValueUtils.coerceToParameterType(long.class, value));
    }

    @Test
    void unsupportedTypesReturnNull() {
        assertNull(GarbageValueUtils.coerceToParameterType(boolean.class, 1));
        assertNull(GarbageValueUtils.coerceToParameterType(char.class, 1));
        assertNull(GarbageValueUtils.coerceToParameterType(float.class, 1));
        assertNull(GarbageValueUtils.coerceToParameterType(double.class, 1));
        assertNull(GarbageValueUtils.coerceToParameterType(Integer.class, 1));
        assertNull(GarbageValueUtils.coerceToParameterType(Object.class, 1));
    }

    @Test
    void supportedTypePredicateMatchesCoercibleTypes() {
        assertTrue(GarbageValueUtils.isSupportedParameterType(byte.class));
        assertTrue(GarbageValueUtils.isSupportedParameterType(short.class));
        assertTrue(GarbageValueUtils.isSupportedParameterType(int.class));
        assertTrue(GarbageValueUtils.isSupportedParameterType(long.class));
        assertFalse(GarbageValueUtils.isSupportedParameterType(boolean.class));
        assertFalse(GarbageValueUtils.isSupportedParameterType(Integer.class));
    }

    /**
     * For the current hooks values, boxing at the width the historical magnitude heuristic
     * selected must produce the identical boxed argument. This pins the coercion math against the
     * behavior the client was verified with; the declared-type lookup can only ever change the
     * width when the magnitude heuristic would have guessed wrong (and failed the invoke).
     */
    @Test
    void matchesTheMagnitudeHeuristicForCurrentHookValues() {
        assertMagnitudeEquivalence(HooksLoader.getReflectionHooks().getPacketBufferNodeGarbageValue());
        assertMagnitudeEquivalence(HooksLoader.getReflectionHooks().getAddNodeGarbageValue());
        assertMagnitudeEquivalence(HooksLoader.getReflectionHooks().getDoActionGarbageValue());
    }

    private static void assertMagnitudeEquivalence(Integer garbageValue) {
        long magnitude = Math.abs(garbageValue.longValue());
        if (magnitude < 256) {
            assertEquals(garbageValue.byteValue(), GarbageValueUtils.coerceToParameterType(byte.class, garbageValue));
        } else if (magnitude < 32768) {
            assertEquals(garbageValue.shortValue(), GarbageValueUtils.coerceToParameterType(short.class, garbageValue));
        } else {
            assertEquals(garbageValue.intValue(), GarbageValueUtils.coerceToParameterType(int.class, garbageValue));
        }
    }
}
