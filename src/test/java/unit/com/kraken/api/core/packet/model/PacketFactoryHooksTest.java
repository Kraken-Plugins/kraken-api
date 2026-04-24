package unit.com.kraken.api.core.packet.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kraken.api.core.packet.model.LoginHooks;
import com.kraken.api.core.packet.PacketFactory;
import com.kraken.api.core.packet.model.PacketMetadata;
import com.kraken.api.util.JsonResourceUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PacketFactoryHooksTest {

    private final Gson gson = new Gson();

    @Test
    void loadsLoginHooksFromPacketsJson() {
        JsonObject loginHooksJson = loadPacketsJson().getAsJsonObject("loginHooks");
        LoginHooks hooks = PacketFactory.getLoginHooks();

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

    @Test
    void loadsMouseHookMetadataFromPacketsJson() {
        JsonObject metadataJson = loadPacketsJson().getAsJsonObject("reflectionHooks");
        PacketMetadata metadata = PacketFactory.getPacketMetadata();

        assertNotNull(metadata);
        assertEquals(metadataJson.get("mouseHookDllClassName").getAsString(), metadata.getMouseHookDllClassName());
        assertEquals(metadataJson.get("mouseHookDllMethodName").getAsString(), metadata.getMouseHookDllMethodName());
    }

    private JsonObject loadPacketsJson() {
        return JsonResourceUtils.loadJsonResource(
                PacketFactoryHooksTest.class,
                "/packets.json",
                gson,
                JsonObject.class
        );
    }
}
