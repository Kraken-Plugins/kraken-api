package com.kraken.api.core.packet.model;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Singleton
public class PacketDefFactory {

    private final Map<String, PacketDefinition> packets;
    private static final String PACKETS_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/packets.json";

    public PacketDefFactory() {
        try {
            HttpURLConnection connection = getHttpURLConnection();

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                Gson gson = new Gson();
                MappedPackets mappedPackets = gson.fromJson(reader, MappedPackets.class);

                if (mappedPackets == null || mappedPackets.getPackets() == null) {
                    throw new IllegalStateException("Parsed MappedPackets or its packet map was null.");
                }

                this.packets = mappedPackets.getPackets();
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize PacketDefFactory from remote JSON", e);
        }
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
    private PacketDefinition getPacket(String packetName) {
        PacketDefinition def = packets.get(packetName);
        if (def == null) {
            throw new IllegalStateException("Packet definition not found in mapped JSON for: " + packetName);
        }
        return def;
    }

    // OPOBJ packets (1-5)
    public PacketDefinition getOpObj1() { return getPacket("OPOBJ1"); }
    public PacketDefinition getOpObj2() { return getPacket("OPOBJ2"); }
    public PacketDefinition getOpObj3() { return getPacket("OPOBJ3"); }
    public PacketDefinition getOpObj4() { return getPacket("OPOBJ4"); }
    public PacketDefinition getOpObj5() { return getPacket("OPOBJ5"); }

    // OPLOC packets (1-5)
    public PacketDefinition getOpLoc1() { return getPacket("OPLOC1"); }
    public PacketDefinition getOpLoc2() { return getPacket("OPLOC2"); }
    public PacketDefinition getOpLoc3() { return getPacket("OPLOC3"); }
    public PacketDefinition getOpLoc4() { return getPacket("OPLOC4"); }
    public PacketDefinition getOpLoc5() { return getPacket("OPLOC5"); }

    // OPNPC packets (1-5)
    public PacketDefinition getOpNpc1() { return getPacket("OPNPC1"); }
    public PacketDefinition getOpNpc2() { return getPacket("OPNPC2"); }
    public PacketDefinition getOpNpc3() { return getPacket("OPNPC3"); }
    public PacketDefinition getOpNpc4() { return getPacket("OPNPC4"); }
    public PacketDefinition getOpNpc5() { return getPacket("OPNPC5"); }

    // OPPLAYER packets (1-8)
    public PacketDefinition getOpPlayer1() { return getPacket("OPPLAYER1"); }
    public PacketDefinition getOpPlayer2() { return getPacket("OPPLAYER2"); }
    public PacketDefinition getOpPlayer3() { return getPacket("OPPLAYER3"); }
    public PacketDefinition getOpPlayer4() { return getPacket("OPPLAYER4"); }
    public PacketDefinition getOpPlayer5() { return getPacket("OPPLAYER5"); }
    public PacketDefinition getOpPlayer6() { return getPacket("OPPLAYER6"); }
    public PacketDefinition getOpPlayer7() { return getPacket("OPPLAYER7"); }
    public PacketDefinition getOpPlayer8() { return getPacket("OPPLAYER8"); }

    // Special operation packets with items
    public PacketDefinition getOpLocT() { return getPacket("OPLOCT"); }
    public PacketDefinition getOpNpcT() { return getPacket("OPNPCT"); }
    public PacketDefinition getOpPlayerT() { return getPacket("OPPLAYERT"); }
    public PacketDefinition getOpObjT() { return getPacket("OPOBJT"); }

    // Interface/Widget packets
    public PacketDefinition getIfButtonT() { return getPacket("IF_BUTTONT"); }
    public PacketDefinition getIfButtonX() { return getPacket("IF_BUTTONX"); }
    public PacketDefinition getIfSubOp() { return getPacket("IF_SUBOP"); }
    public PacketDefinition getOpHeldd() { return getPacket("OPHELDD"); }

    // Resume packets for dialogues with NPC's in the chatbox.
    public PacketDefinition getResumePausebutton() { return getPacket("RESUME_PAUSEBUTTON"); }
    public PacketDefinition getResumeCountDialog() { return getPacket("RESUME_COUNTDIALOG"); }
    public PacketDefinition getResumeObjDialog() { return getPacket("RESUME_OBJDIALOG"); }
    public PacketDefinition getResumeNameDialog() { return getPacket("RESUME_NAMEDIALOG"); }
    public PacketDefinition getResumeStringDialog() { return getPacket("RESUME_STRINGDIALOG"); }

    // Movement and event packets
    public PacketDefinition getMoveGameClick() { return getPacket("MOVE_GAMECLICK"); }
    public PacketDefinition getEventMouseClick() { return getPacket("EVENT_MOUSE_CLICK"); }
    public PacketDefinition getSetHeading() { return getPacket("SET_HEADING"); }

    public PacketDefinition getDefinitionForType(PacketType type) {
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

    public PacketDefinition getDefinitionForType(PacketType type, int action) {
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

    public PacketDefinition getOpObj(int action) {
        switch (action) {
            case 1: return getOpObj1();
            case 2: return getOpObj2();
            case 3: return getOpObj3();
            case 4: return getOpObj4();
            case 5: return getOpObj5();
            default: throw new IllegalArgumentException("Invalid OPOBJ action (supports 1-5): " + action);
        }
    }

    public PacketDefinition getOpLoc(int action) {
        switch (action) {
            case 1: return getOpLoc1();
            case 2: return getOpLoc2();
            case 3: return getOpLoc3();
            case 4: return getOpLoc4();
            case 5: return getOpLoc5();
            default: throw new IllegalArgumentException("Invalid OPLOC action (supports 1-5): " + action);
        }
    }

    public PacketDefinition getOpNpc(int action) {
        switch (action) {
            case 1: return getOpNpc1();
            case 2: return getOpNpc2();
            case 3: return getOpNpc3();
            case 4: return getOpNpc4();
            case 5: return getOpNpc5();
            default: throw new IllegalArgumentException("Invalid OPNPC action (supports 1-5): " + action);
        }
    }

    public PacketDefinition getOpPlayer(int action) {
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