package com.kraken.api.core.mapping;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientDownloader {

    private static final String CLIENT_BASE_URL = "https://repo.runelite.net/net/runelite/injected-client/";

    /**
     * Downloads the injected client JAR file for a specified version to the given destination path.
     *
     * <p>This method retrieves the injected client file from a predefined base URL. It validates the
     * version string, ensuring it meets compatibility requirements, and processes {@literal @SNAPSHOT}
     * versions by removing the snapshot suffix. If the destination directory does not exist, it
     * attempts to create the necessary directory structure.
     *
     * <p>If the version does not meet predefined compatibility criteria, an
     * {@link UnsupportedOperationException} is thrown. During the download or file writing process,
     * any {@link IOException} encountered is logged but not rethrown.
     *
     * @param version the version of the injected client to download. This must follow the expected
     *                versioning scheme and meet the compatibility criteria.
     * @param destination the {@link Path} where the injected client JAR will be downloaded. The path
     *                    must include the file name for the target file, and parent directories will
     *                    be created if they do not exist.
     */
    public static Path downloadInjectedClient(String version, Path destination) {
        if(Files.exists(destination)) return destination;

        try {
            Files.createDirectories(destination.getParent());

            if (version.contains("SNAPSHOT")) {
                String snapshotVersion = version;
                version = version.replace("-SNAPSHOT", "");
                log.info("Treating SNAPSHOT version {} as base version: {}", snapshotVersion, version);
            }


            String[] versionSplits = version.split("\\.");
            int length = versionSplits.length;
            if (!((length > 0 && Integer.parseInt(versionSplits[0]) > 1) ||
                    (length > 1 && Integer.parseInt(versionSplits[1]) > 10) ||
                    (length > 2 && Integer.parseInt(versionSplits[2]) > 34))) {
                throw new UnsupportedOperationException("Unsupported RuneLite version: " + version);
            }

            URL injectedURL = new URL(CLIENT_BASE_URL + version + "/injected-client-" + version + ".jar");
            log.info("Downloading injected client from {}", injectedURL);
            try (InputStream clientStream = injectedURL.openStream()) {
                Files.copy(clientStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return destination;
        } catch (IOException e) {
            log.error("IOException thrown while downloading injected client to destination: {}, error:", destination, e);
        }
        return null;
    }

    /**
     * Downloads the injected client JAR file to the specified destination path. If the destination
     * file already exists, the method skips the download process.
     *
     * <p>The method determines the required RuneLite version, constructs the URL for the injected
     * client, and downloads the file to the specified location. If the RuneLite version is a
     * {@literal @SNAPSHOT} version, it handles the version accordingly by removing the
     * {@literal @SNAPSHOT} suffix. Unsupported versions will result in an exception.
     *
     * <p>Exceptions such as {@link IOException} during the file operation are logged.
     *
     * @param destination the {@link Path} where the injected client will be downloaded.
     *                    This must include both the path and the file name of the target file.
     */
    public static void downloadInjectedClient(Path destination) {
        if (Files.exists(destination)) {
            log.info("Injected client already exists at {}, skipping download", destination);
            return;
        }

        String version = RuneLiteProperties.getVersion();
        downloadInjectedClient(version, destination);
    }
}
