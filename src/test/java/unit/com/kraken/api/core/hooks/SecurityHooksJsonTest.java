package unit.com.kraken.api.core.hooks;

import com.google.gson.Gson;
import com.kraken.api.core.hooks.PlatformInfoField;
import com.kraken.api.core.hooks.SecurityHooks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Parses the {@code securityHooks} JSON kraken-updater writes into {@link SecurityHooks}.
 */
class SecurityHooksJsonTest {

    @Test
    void parsesThePlatformInfoFieldsInSendOrder() {
        SecurityHooks hooks = new Gson().fromJson("{\"platformInfoClassName\":\"vo\",\"platformInfoFields\":["
                + "{\"name\":\"OsType\",\"fieldName\":\"bb\",\"multiplier\":757225699},"
                + "{\"name\":\"Is64Bit\",\"fieldName\":\"br\"}]}", SecurityHooks.class);

        assertEquals(List.of(new PlatformInfoField("OsType", "bb", 757225699), new PlatformInfoField("Is64Bit", "br", null)),
                hooks.getPlatformInfoFields());
    }

    @Test
    void leavesThePlatformInfoFieldsNullWhenTheJsonPredatesThem() {
        SecurityHooks hooks = new Gson().fromJson("{\"platformInfoClassName\":\"vo\"}", SecurityHooks.class);

        assertNull(hooks.getPlatformInfoFields());
    }

    @Test
    void findsAPlatformInfoFieldByItsProtocolName() {
        SecurityHooks hooks = new Gson().fromJson("{\"platformInfoFields\":["
                + "{\"name\":\"ClientName\",\"fieldName\":\"ci\"},{\"name\":\"DeviceName\",\"fieldName\":\"cx\"}]}", SecurityHooks.class);

        assertEquals("cx", hooks.getPlatformInfoFieldName(PlatformInfoField.DEVICE_NAME));
        assertEquals("ci", hooks.getPlatformInfoFieldName(PlatformInfoField.CLIENT_NAME));
        assertNull(hooks.getPlatformInfoFieldName("OsType"));
        assertNull(new Gson().fromJson("{}", SecurityHooks.class).getPlatformInfoFieldName(PlatformInfoField.DEVICE_NAME));
    }
}
