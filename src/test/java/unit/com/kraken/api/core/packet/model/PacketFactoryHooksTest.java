package unit.com.kraken.api.core.packet.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.LoginHooks;
import com.kraken.api.util.JsonResourceUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PacketFactoryHooksTest {

    private final Gson gson = new Gson();

    @Test
    void loadsLoginHooksFromPacketsJson() {
        JsonObject loginHooksJson = loadPacketsJson().getAsJsonObject("loginHooks");
        LoginHooks hooks = HooksLoader.getLoginHooks();

        assertNotNull(hooks);
        assertEquals(loginHooksJson.get("loginIndexMethodName").getAsString(), hooks.getLoginIndexMethodName());
        assertEquals(loginHooksJson.get("loginIndexClassName").getAsString(), hooks.getLoginIndexClassName());
        assertEquals(loginHooksJson.get("loginIndexGarbageValue").getAsInt(), hooks.getLoginIndexGarbageValue());
        assertEquals(loginHooksJson.get("sessionFieldName").getAsString(), hooks.getSessionFieldName());
        assertEquals(loginHooksJson.get("sessionClassName").getAsString(), hooks.getSessionClassName());
        assertEquals(loginHooksJson.get("accountCheckFieldName").getAsString(), hooks.getAccountCheckFieldName());
        assertEquals(loginHooksJson.get("jagexValueFieldName").getAsString(), hooks.getJagexValueFieldName());
        assertEquals(loginHooksJson.get("legacyValueFieldName").getAsString(), hooks.getLegacyValueFieldName());
    }


    private JsonObject loadPacketsJson() {
        return JsonResourceUtils.loadJsonResource(
                PacketFactoryHooksTest.class,
                "/hooks.json",
                gson,
                JsonObject.class
        );
    }
}
