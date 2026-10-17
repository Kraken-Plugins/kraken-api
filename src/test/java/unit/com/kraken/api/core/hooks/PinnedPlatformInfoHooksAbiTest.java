package unit.com.kraken.api.core.hooks;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.PlatformInfoField;
import com.kraken.api.core.hooks.SecurityHooks;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline ABI checks of the PlatformInfo field list against the injected client pinned by the test runtime
 * dependencies. Kraken-Client finds the fields its agent and call stack patches replace only through this list, so
 * {@code hooks.json} must carry it.
 */
class PinnedPlatformInfoHooksAbiTest {

    private static List<PlatformInfoField> listedFields(SecurityHooks hooks) {
        assertNotNull(hooks.getPlatformInfoFields(), "hooks.json does not list the PlatformInfo fields; copy kraken-updater's mappings into it");
        return hooks.getPlatformInfoFields();
    }

    @Test
    void everyListedFieldIsAnInstanceFieldOfPlatformInfo() throws Exception {
        SecurityHooks hooks = HooksLoader.getSecurityHooks();
        Class<?> platformInfo = Class.forName(hooks.getPlatformInfoClassName(), false, PinnedPlatformInfoHooksAbiTest.class.getClassLoader());
        Set<String> listed = new HashSet<>();
        for (PlatformInfoField field : listedFields(hooks)) {
            assertNotNull(field.getName(), field + " has no name");
            assertTrue(listed.add(field.getFieldName()), field + " is listed twice");

            Field declared = platformInfo.getDeclaredField(field.getFieldName());
            assertFalse(Modifier.isStatic(declared.getModifiers()), field + " is static");
            if (field.getMultiplier() != null) {
                assertEquals(int.class, declared.getType(), field + " has a multiplier but is not an int");
                assertEquals(1, field.getMultiplier() & 1, field + " has an even multiplier");
            }
        }
    }

    @Test
    void thePatchedFieldsAreDistinctStringFields() throws Exception {
        SecurityHooks hooks = HooksLoader.getSecurityHooks();
        listedFields(hooks);
        Class<?> platformInfo = Class.forName(hooks.getPlatformInfoClassName(), false, PinnedPlatformInfoHooksAbiTest.class.getClassLoader());

        String agentField = hooks.getPlatformInfoFieldName(PlatformInfoField.DEVICE_NAME);
        String callStackField = hooks.getPlatformInfoFieldName(PlatformInfoField.CLIENT_NAME);
        assertNotNull(agentField, "no field is listed as " + PlatformInfoField.DEVICE_NAME);
        assertNotNull(callStackField, "no field is listed as " + PlatformInfoField.CLIENT_NAME);
        assertNotEquals(agentField, callStackField);
        assertEquals(String.class, platformInfo.getDeclaredField(agentField).getType());
        assertEquals(String.class, platformInfo.getDeclaredField(callStackField).getType());
    }
}
