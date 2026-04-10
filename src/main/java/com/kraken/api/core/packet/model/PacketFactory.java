package com.kraken.api.core.packet.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();


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

    /**
     * Attempts to load packet definitions from a local JSON file located within the application's resources.
     * <p>
     * This method reads the JSON file specified by the {@literal LOCAL_PACKETS_PATH} constant, parses its contents,
     * and updates the internal packet definition structures accordingly. If successful, it logs the completion
     * and returns {@code true}. If the file is missing, unreadable, or an exception occurs during the operation,
     * the method logs the issue and returns {@code false}.
     * </p>
     *
     * <p>Key steps performed by this method:</p>
     * <ul>
     *   <li>Access the JSON file using the {@code getResourceAsStream} method.</li>
     *   <li>Wrap the resource's {@code InputStream} in an {@code InputStreamReader} for parsing operations.</li>
     *   <li>Parse the JSON content using the {@code parseJson} helper method to populate packet definitions.</li>
     *   <li>Log any errors that occur and ensure all resources are safely closed using try-with-resources.</li>
     * </ul>
     *
     * @return {@code true} if the packets were successfully loaded and parsed from local resources;
     *         {@code false} if the file was not found or an error occurred during processing.
     */
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

    /**
     * Loads packet definitions from a remote JSON file specified by the {@literal PACKETS_URL}.
     * <p>
     * This method establishes an HTTP connection to the remote resource, retrieves the JSON file
     * containing packet definitions, and parses its contents to update internal packet mappings
     * and metadata. In case of failure (e.g., network issues or invalid responses), an error
     * is logged, and the process is aborted gracefully.
     * </p>
     *
     * <p>Key operations performed by this method:</p>
     * <ul>
     *   <li>Configures an HTTP connection to the specified URL with appropriate headers and timeouts.</li>
     *   <li>Validates the HTTP response code to ensure a successful fetch (status code 200).</li>
     *   <li>Parses the remote JSON file using the {@code parseJson(InputStreamReader)} helper method
     *       to populate packet-related structures.</li>
     *   <li>Uses try-with-resources to ensure the proper closure of resources such as input streams.</li>
     *   <li>Logs success upon successful JSON parsing or logs errors if an exception occurs during
     *       the fetch or parsing process.</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method relies on the following:</p>
     * <ul>
     *   <li>{@literal PACKETS_URL} - A static URL constant pointing to the JSON resource.</li>
     *   <li>{@code parseJson(InputStreamReader)} - A helper method that handles parsing and validation
     *       of the JSON content.</li>
     *   <li>Custom logging utility {@code log} to log messages and errors.</li>
     * </ul>
     *
     * <p>Exceptions thrown during the fetch or parsing process (e.g., {@code IOException},
     * {@code MalformedURLException}, or JSON parsing errors) are caught and logged as errors.</p>
     */
    private static void loadFromRemote() {
        try {
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

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                log.info("Loaded packets.json from remote.");
                parseJson(reader);
            }
        } catch (Exception e) {
            log.error("Failed to initialize PacketFactory from remote JSON", e);
        }
    }

    /**
     * Parses the JSON data from the provided {@link InputStreamReader} and updates internal packet structures.
     * <p>
     * This method deserializes a JSON input stream into a {@code MappedPackets} object and validates its contents.
     * If the parsed data is null or its packet map is missing, the method throws an {@code IllegalStateException}.
     * Otherwise, it extracts and assigns the relevant data, including packet mappings, metadata, and client version,
     * to corresponding internal fields.
     * </p>
     *
     * <p><strong>Important notes:</strong></p>
     * <ul>
     *   <li>The {@code MappedPackets} object must contain valid and non-null packet definitions.</li>
     *   <li>Validation ensures the integrity of the internal state before further processing.</li>
     * </ul>
     *
     * @param reader an {@link InputStreamReader} providing access to the JSON data to be parsed.
     *               The stream should contain a valid JSON representation of {@code MappedPackets}.
     * @throws IllegalStateException if the parsed {@code MappedPackets} is null or its packet map is missing.
     */
    private static void parseJson(InputStreamReader reader) {
        MappedPackets mappedPackets = gson.fromJson(reader, MappedPackets.class);

        if (mappedPackets == null || mappedPackets.getPackets() == null) {
            throw new IllegalStateException("Parsed MappedPackets or its packet map was null.");
        }

        packets = mappedPackets.getPackets();
        packetMetadata = mappedPackets.getReflectionHooks();
        clientVersion = mappedPackets.getClientVersion();
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

    // Interface/Widget packets
    public static PacketDefinition getIfSubOp() { return getPacket("IF_SUBOP"); }

    // Resume packets for dialogues with NPC's in the chatbox.
    public static PacketDefinition getResumePausebutton() { return getPacket("RESUME_PAUSEBUTTON"); }
    public static PacketDefinition getResumeCountDialog() { return getPacket("RESUME_COUNTDIALOG"); }
    public static PacketDefinition getResumeObjDialog() { return getPacket("RESUME_OBJDIALOG"); }
    public static PacketDefinition getResumeNameDialog() { return getPacket("RESUME_NAMEDIALOG"); }
    public static PacketDefinition getResumeStringDialog() { return getPacket("RESUME_STRINGDIALOG"); }

    // Movement and event packets
    public static PacketDefinition getMoveGameClick() { return getPacket("MOVE_GAMECLICK"); }
    public static PacketDefinition getEventMouseClick() { return getPacket("EVENT_MOUSE_CLICK"); }
}