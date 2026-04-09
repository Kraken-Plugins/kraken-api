package com.kraken.api.core.packet.model;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>A factory class for creating and managing {@link PacketDefinition} instances for various packet types and actions.
 * This class initializes packet definitions by fetching and parsing a JSON configuration file locally first,
 * then falling back to a remote URL.</p>
 */
@Slf4j
public class PacketFactory {

    @Getter
    private static Map<String, PacketDefinition> packets = new HashMap<>();

    @Getter
    private static PacketMetadata packetMetadata = null;

    @Getter
    private static String clientVersion = "";

    private static final String PACKETS_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/packets.json";
    private static final String LOCAL_PACKETS_PATH = "/packets.json";

    static {
        if (!loadFromLocalResources()) {
            log.info("Local packets.json not found or failed to load. Falling back to remote URL: {}", PACKETS_URL);
            loadFromRemote();
        } else {
            log.debug("Successfully loaded packets.json from local resources.");
        }
    }

    private static boolean loadFromLocalResources() {
        try (InputStream is = PacketFactory.class.getResourceAsStream(LOCAL_PACKETS_PATH)) {
            if (is == null) {
                return false; // File not found in resources
            }
            try (InputStreamReader reader = new InputStreamReader(is)) {
                log.info("Loaded packets.json from local resources.");
                parseJson(reader);
                return true;
            }
        } catch (Exception e) {
            log.warn("Exception while trying to load packets.json locally", e);
            return false;
        }
    }

    private static void loadFromRemote() {
        try {
            HttpURLConnection connection = getHttpURLConnection();
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                log.info("Loaded packets.json from remote.");
                parseJson(reader);
            }
        } catch (Exception e) {
            log.error("Failed to initialize PacketFactory from remote JSON", e);
        }
    }

    private static void parseJson(InputStreamReader reader) {
        Gson gson = new Gson();
        MappedPackets mappedPackets = gson.fromJson(reader, MappedPackets.class);

        if (mappedPackets == null || mappedPackets.getPackets() == null) {
            throw new IllegalStateException("Parsed MappedPackets or its packet map was null.");
        }

        packets = mappedPackets.getPackets();
        packetMetadata = mappedPackets.getReflectionHooks();
        clientVersion = mappedPackets.getClientVersion();
    }

    private static @NonNull HttpURLConnection getHttpURLConnection() throws IOException {
        URL url = new URL(PACKETS_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Kraken-Client");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Failed to fetch packets.json. HTTP Code: " + connection.getResponseCode());
        }
        return connection;
    }

    /**
     * Helper method to safely retrieve a packet and fail fast if the JSON is missing definitions.
     */
    private static PacketDefinition getPacket(String packetName) {
        PacketDefinition def = packets.get(packetName);
        if (def == null) {
            throw new IllegalStateException("Packet definition not found in mapped JSON for: " + packetName);
        }
        return def;
    }

    // OPOBJ packets (1-5)
    public static PacketDefinition getOpObj1() { return getPacket("OPOBJ1"); }
    public static PacketDefinition getOpObj2() { return getPacket("OPOBJ2"); }
    public static PacketDefinition getOpObj3() { return getPacket("OPOBJ3"); }
    public static PacketDefinition getOpObj4() { return getPacket("OPOBJ4"); }
    public static PacketDefinition getOpObj5() { return getPacket("OPOBJ5"); }

    // OPLOC packets (1-5)
    public static PacketDefinition getOpLoc1() { return getPacket("OPLOC1"); }
    public static PacketDefinition getOpLoc2() { return getPacket("OPLOC2"); }
    public static PacketDefinition getOpLoc3() { return getPacket("OPLOC3"); }
    public static PacketDefinition getOpLoc4() { return getPacket("OPLOC4"); }
    public static PacketDefinition getOpLoc5() { return getPacket("OPLOC5"); }

    // OPNPC packets (1-5)
    public static PacketDefinition getOpNpc1() { return getPacket("OPNPC1"); }
    public static PacketDefinition getOpNpc2() { return getPacket("OPNPC2"); }
    public static PacketDefinition getOpNpc3() { return getPacket("OPNPC3"); }
    public static PacketDefinition getOpNpc4() { return getPacket("OPNPC4"); }
    public static PacketDefinition getOpNpc5() { return getPacket("OPNPC5"); }

    // OPPLAYER packets (1-8)
    public static PacketDefinition getOpPlayer1() { return getPacket("OPPLAYER1"); }
    public static PacketDefinition getOpPlayer2() { return getPacket("OPPLAYER2"); }
    public static PacketDefinition getOpPlayer3() { return getPacket("OPPLAYER3"); }
    public static PacketDefinition getOpPlayer4() { return getPacket("OPPLAYER4"); }
    public static PacketDefinition getOpPlayer5() { return getPacket("OPPLAYER5"); }
    public static PacketDefinition getOpPlayer6() { return getPacket("OPPLAYER6"); }
    public static PacketDefinition getOpPlayer7() { return getPacket("OPPLAYER7"); }
    public static PacketDefinition getOpPlayer8() { return getPacket("OPPLAYER8"); }

    // Special operation packets with items
    public static PacketDefinition getOpLocT() { return getPacket("OPLOCT"); }
    public static PacketDefinition getOpNpcT() { return getPacket("OPNPCT"); }
    public static PacketDefinition getOpPlayerT() { return getPacket("OPPLAYERT"); }
    public static PacketDefinition getOpObjT() { return getPacket("OPOBJT"); }

    // Interface/Widget packets
    public static PacketDefinition getIfButtonT() { return getPacket("IF_BUTTONT"); }
    public static PacketDefinition getIfButtonX() { return getPacket("IF_BUTTONX"); }
    public static PacketDefinition getIfSubOp() { return getPacket("IF_SUBOP"); }
    public static PacketDefinition getOpHeldd() { return getPacket("OPHELDD"); }

    // Resume packets for dialogues with NPC's in the chatbox.
    public static PacketDefinition getResumePausebutton() { return getPacket("RESUME_PAUSEBUTTON"); }
    public static PacketDefinition getResumeCountDialog() { return getPacket("RESUME_COUNTDIALOG"); }
    public static PacketDefinition getResumeObjDialog() { return getPacket("RESUME_OBJDIALOG"); }
    public static PacketDefinition getResumeNameDialog() { return getPacket("RESUME_NAMEDIALOG"); }
    public static PacketDefinition getResumeStringDialog() { return getPacket("RESUME_STRINGDIALOG"); }

    // Movement and event packets
    public static PacketDefinition getMoveGameClick() { return getPacket("MOVE_GAMECLICK"); }
    public static PacketDefinition getEventMouseClick() { return getPacket("EVENT_MOUSE_CLICK"); }
    public static PacketDefinition getSetHeading() { return getPacket("SET_HEADING"); }

    public static PacketDefinition getDefinitionForType(PacketType type) {
        switch (type) {
            case OPOBJ: return getOpObj1();
            case OPLOC: return getOpLoc1();
            case OPNPC: return getOpNpc1();
            case OPPLAYER: return getOpPlayer1();
            case OPLOCT: return getOpLocT();
            case OPNPCT: return getOpNpcT();
            case OPPLAYERT: return getOpPlayerT();
            case OPOBJT: return getOpObjT();
            case IF_BUTTONT: return getIfButtonT();
            case IF_BUTTONX:
            case IF_BUTTON:
                return getIfButtonX();
            case IF_SUBOP: return getIfSubOp();
            case OPHELDD: return getOpHeldd();
            case RESUME_PAUSEBUTTON: return getResumePausebutton();
            case RESUME_COUNTDIALOG: return getResumeCountDialog();
            case RESUME_OBJDIALOG: return getResumeObjDialog();
            case RESUME_NAMEDIALOG: return getResumeNameDialog();
            case RESUME_STRINGDIALOG: return getResumeStringDialog();
            case MOVE_GAMECLICK: return getMoveGameClick();
            case EVENT_MOUSE_CLICK: return getEventMouseClick();
            case SET_HEADING: return getSetHeading();
            default: throw new IllegalArgumentException("Unknown packet type: " + type);
        }
    }

    public static PacketDefinition getDefinitionForType(PacketType type, int action) {
        switch (type) {
            case OPOBJ: return getOpObj(action);
            case OPLOC: return getOpLoc(action);
            case OPNPC: return getOpNpc(action);
            case OPPLAYER: return getOpPlayer(action);
            case OPLOCT: return getOpLocT();
            case OPNPCT: return getOpNpcT();
            case OPPLAYERT: return getOpPlayerT();
            case OPOBJT: return getOpObjT();
            case IF_BUTTON:
            case IF_BUTTONX: return getIfButtonX();
            case IF_BUTTONT: return getIfButtonT();
            case IF_SUBOP: return getIfSubOp();
            case OPHELDD: return getOpHeldd();
            case RESUME_PAUSEBUTTON: return getResumePausebutton();
            case RESUME_COUNTDIALOG: return getResumeCountDialog();
            case RESUME_OBJDIALOG: return getResumeObjDialog();
            case RESUME_NAMEDIALOG: return getResumeNameDialog();
            case RESUME_STRINGDIALOG: return getResumeStringDialog();
            case MOVE_GAMECLICK: return getMoveGameClick();
            case EVENT_MOUSE_CLICK: return getEventMouseClick();
            case SET_HEADING: return getSetHeading();
            default: throw new IllegalArgumentException("Unknown packet type: " + type);
        }
    }

    public static PacketDefinition getOpObj(int action) {
        switch (action) {
            case 1: return getOpObj1();
            case 2: return getOpObj2();
            case 3: return getOpObj3();
            case 4: return getOpObj4();
            case 5: return getOpObj5();
            default: throw new IllegalArgumentException("Invalid OPOBJ action (supports 1-5): " + action);
        }
    }

    public static PacketDefinition getOpLoc(int action) {
        switch (action) {
            case 1: return getOpLoc1();
            case 2: return getOpLoc2();
            case 3: return getOpLoc3();
            case 4: return getOpLoc4();
            case 5: return getOpLoc5();
            default: throw new IllegalArgumentException("Invalid OPLOC action (supports 1-5): " + action);
        }
    }

    public static PacketDefinition getOpNpc(int action) {
        switch (action) {
            case 1: return getOpNpc1();
            case 2: return getOpNpc2();
            case 3: return getOpNpc3();
            case 4: return getOpNpc4();
            case 5: return getOpNpc5();
            default: throw new IllegalArgumentException("Invalid OPNPC action (supports 1-5): " + action);
        }
    }

    public static PacketDefinition getOpPlayer(int action) {
        switch (action) {
            case 1: return getOpPlayer1();
            case 2: return getOpPlayer2();
            case 3: return getOpPlayer3();
            case 4: return getOpPlayer4();
            case 5: return getOpPlayer5();
            case 6: return getOpPlayer6();
            case 7: return getOpPlayer7();
            case 8: return getOpPlayer8();
            default: throw new IllegalArgumentException("Invalid OPPLAYER action (supports 1-8): " + action);
        }
    }
}