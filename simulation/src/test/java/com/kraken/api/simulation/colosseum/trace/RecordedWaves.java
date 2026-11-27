package com.kraken.api.simulation.colosseum.trace;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** The seven live waves Auto Colosseum V2 recorded on 2026-08-25, copied under the test resources. */
final class RecordedWaves {
    private RecordedWaves() {
    }

    /** The live trace files, in name order. */
    static List<Path> files() throws IOException {
        URL dir = RecordedWaves.class.getResource("/colosseum/traces");
        if (dir == null) {
            throw new IllegalStateException("no /colosseum/traces on the test classpath");
        }
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dir.toURI()), "*.live.jsonl")) {
            for (Path file : files) {
                out.add(file);
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        out.sort(null);
        return out;
    }
}
