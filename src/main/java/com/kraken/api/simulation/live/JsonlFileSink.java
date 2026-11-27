package com.kraken.api.simulation.live;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * JSONL file sink: opened lazily on the first line, flushed every {@code flushEvery} lines so a client crash loses at
 * most a few, and latched shut on close. Writes are synchronized (lines may arrive from a search worker and the client
 * thread). An IO failure disables the sink for its lifetime rather than spamming the log.
 */
@Slf4j
public final class JsonlFileSink implements Consumer<String>, AutoCloseable {
    private final File file;
    private final String label;
    private final int flushEvery;
    private BufferedWriter writer;
    private boolean failed;
    private boolean closed;
    private int sinceFlush;

    /**
     * A sink for one file.
     * @param file the file, created with its directories on the first line
     * @param label what the file holds, for the log
     * @param flushEvery lines between flushes
     */
    public JsonlFileSink(File file, String label, int flushEvery) {
        this.file = file;
        this.label = label;
        this.flushEvery = flushEvery;
    }

    @Override
    public synchronized void accept(String line) {
        if (failed || closed) {
            return;
        }
        try {
            if (writer == null) {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
                log.info("Simulation {}: {}", label, file.getAbsolutePath());
            }
            writer.write(line);
            writer.newLine();
            if (++sinceFlush >= flushEvery) {
                sinceFlush = 0;
                writer.flush();
            }
        } catch (IOException e) {
            failed = true;
            log.warn("Simulation {} write failed; disabling", label, e);
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log.warn("Simulation {} close failed", label, e);
            }
            writer = null;
        }
    }
}
