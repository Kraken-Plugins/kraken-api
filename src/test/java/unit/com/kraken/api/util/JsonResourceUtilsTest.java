package unit.com.kraken.api.util;

import com.google.gson.Gson;
import com.kraken.api.util.JsonResourceUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonResourceUtilsTest {

    private final Gson gson = new Gson();

    @Test
    void loadsJsonFromClasspathResource() {
        ExamplePayload payload = JsonResourceUtils.loadJsonResource(
                JsonResourceUtilsTest.class,
                "/unit/com/kraken/api/util/example-payload.json",
                gson,
                ExamplePayload.class
        );

        assertEquals("kraken", payload.name);
        assertEquals(7, payload.count);
    }

    @Test
    void failsWhenClasspathResourceIsMissing() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> JsonResourceUtils.loadJsonResource(
                        JsonResourceUtilsTest.class,
                        "/unit/com/kraken/api/util/missing.json",
                        gson,
                        ExamplePayload.class
                )
        );

        assertEquals("Resource not found: /unit/com/kraken/api/util/missing.json", exception.getMessage());
    }

    static class ExamplePayload {
        String name;
        int count;
    }
}
