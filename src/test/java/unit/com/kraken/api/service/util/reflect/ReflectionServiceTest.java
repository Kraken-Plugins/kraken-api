package unit.com.kraken.api.service.util.reflect;

import com.kraken.api.service.util.ReflectionService;
import net.runelite.api.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReflectionServiceTest {

    private ReflectionService reflectionService;

    @BeforeEach
    void setUp() {
        reflectionService = new ReflectionService(createClientProxy());
        ReflectionTarget.reset();
    }

    @Test
    void readsAndWritesMappedFields() {
        ReflectionTarget target = new ReflectionTarget();

        reflectionService.setFieldValue(
                ReflectionTarget.class.getName(),
                "instanceValue",
                target,
                "updated"
        );

        String value = reflectionService.getFieldValue(
                ReflectionTarget.class.getName(),
                "instanceValue",
                target
        );

        assertEquals("updated", value);
    }

    @Test
    void invokesMethodsWithByteGarbageValue() {
        reflectionService.invoke(
                ReflectionTarget.class.getName(),
                "applyByteGarbage",
                7,
                null,
                42
        );

        assertEquals(42, ReflectionTarget.loginIndex);
        assertEquals((byte) 7, ReflectionTarget.lastByteGarbage);
    }

    @Test
    void invokesMethodsWithShortGarbageValue() {
        reflectionService.invoke(
                ReflectionTarget.class.getName(),
                "applyShortGarbage",
                512,
                null,
                91
        );

        assertEquals(91, ReflectionTarget.loginIndex);
        assertEquals((short) 512, ReflectionTarget.lastShortGarbage);
    }

    @Test
    void invokesMethodsWithIntGarbageValue() {
        reflectionService.invoke(
                ReflectionTarget.class.getName(),
                "applyIntGarbage",
                70000,
                null,
                13
        );

        assertEquals(13, ReflectionTarget.loginIndex);
        assertEquals(70000, ReflectionTarget.lastIntGarbage);
    }

    private Client createClientProxy() {
        return (Client) Proxy.newProxyInstance(
                Client.class.getClassLoader(),
                new Class<?>[]{Client.class},
                (proxy, method, args) -> null
        );
    }

    static class ReflectionTarget {
        private static int loginIndex;
        private static byte lastByteGarbage;
        private static short lastShortGarbage;
        private static int lastIntGarbage;

        private String instanceValue = "initial";

        private static void applyByteGarbage(int value, byte garbage) {
            loginIndex = value;
            lastByteGarbage = garbage;
        }

        private static void applyShortGarbage(int value, short garbage) {
            loginIndex = value;
            lastShortGarbage = garbage;
        }

        private static void applyIntGarbage(int value, int garbage) {
            loginIndex = value;
            lastIntGarbage = garbage;
        }

        private static void reset() {
            loginIndex = -1;
            lastByteGarbage = 0;
            lastShortGarbage = 0;
            lastIntGarbage = 0;
        }
    }
}
