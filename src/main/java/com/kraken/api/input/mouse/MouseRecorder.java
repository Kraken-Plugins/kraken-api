package com.kraken.api.input.mouse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kraken.api.input.mouse.model.MouseGesture;
import com.kraken.api.input.mouse.model.RecordedPoint;
import com.kraken.api.input.mouse.strategy.replay.PathLibrary;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Records mouse gestures (the movement leading up to each click) and appends them as JSON lines to a
 * file named after the recording label, for later replay by the REPLAY movement strategy.
 *
 * <p>Listener callbacks arrive on the AWT event dispatch thread; {@link #start} and {@link #stop} may be
 * called from any thread. All in-memory state is guarded by this object's monitor. Disk writes go through
 * a single-threaded writer that the recorder owns for the lifetime of one recording: each job carries its
 * own target path and batch, jobs run in submission order, and {@link #stop} does not return until every
 * job for that recording has been written and the writer thread has exited.</p>
 */
@Slf4j
@Singleton
public class MouseRecorder implements MouseListener {

    /** Completed gestures held in memory before a write is scheduled. */
    public static final int BATCH_SIZE = 500;

    /** Longest movement kept for a single gesture; longer hovers are discarded and restarted. */
    public static final int MAX_POINTS_PER_GESTURE = 5_000;

    /** Batches allowed to wait for the writer before the submitting thread writes one itself. */
    private static final int MAX_PENDING_BATCHES = 4;

    private final MouseManager mouseManager;
    private final Path dataDir;
    private final Gson gson = new GsonBuilder().create();

    private volatile boolean isRecording = false;
    private String currentLabel = "default";
    private Path currentFile;
    private ThreadPoolExecutor writer;

    private final List<RecordedPoint> movementBuffer = new ArrayList<>();
    private final List<MouseGesture> gestureBuffer = new ArrayList<>();
    private long gestureStartTime = -1;

    @Inject
    public MouseRecorder(MouseManager mouseManager) {
        this(mouseManager, Paths.get(PathLibrary.DATA_DIR));
    }

    /**
     * Creates a recorder writing into the given directory.
     * @param mouseManager RuneLite's mouse manager to listen on
     * @param dataDir Directory that receives one JSON-lines file per label
     */
    public MouseRecorder(MouseManager mouseManager, Path dataDir) {
        this.mouseManager = mouseManager;
        this.dataDir = dataDir.toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.dataDir);
        } catch (IOException e) {
            log.error("Failed to create mouse data directory", e);
        }
    }

    /**
     * Resolves the file a label is written to. Every character outside {@code [A-Za-z0-9._-]} becomes an
     * underscore so a label can only ever name a file directly inside the data directory.
     * @param label The recording label
     * @return The path of the label's JSON-lines file
     */
    public Path fileFor(String label) {
        String safe = label == null ? "" : label.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isEmpty() || safe.chars().allMatch(c -> c == '.')) safe = "default";
        Path file = dataDir.resolve(safe + ".json").normalize();
        if (!file.getParent().equals(dataDir)) {
            throw new IllegalArgumentException("Label escapes the mouse data directory: " + label);
        }
        return file;
    }

    /**
     * Starts recording mouse gestures under the given label. Does nothing when already recording.
     * @param label The label naming the output file; characters outside {@code [A-Za-z0-9._-]} become underscores
     */
    public synchronized void start(String label) {
        if (isRecording) return;

        currentFile = fileFor(label);
        currentLabel = currentFile.getFileName().toString().replaceAll("\\.json$", "");
        movementBuffer.clear();
        gestureBuffer.clear();
        gestureStartTime = System.currentTimeMillis();
        writer = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_PENDING_BATCHES),
                runnable -> {
                    Thread thread = new Thread(runnable, "kraken-mouse-recorder");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        isRecording = true;

        mouseManager.registerMouseListener(this);
        log.info("Mouse Recording STARTED: {}", currentLabel);
    }

    /**
     * Stops recording, writes any buffered gestures, and blocks until every pending write for this
     * recording has completed.
     */
    public void stop() {
        ThreadPoolExecutor toDrain;
        synchronized (this) {
            if (!isRecording) return;
            isRecording = false;
            mouseManager.unregisterMouseListener(this);
            flushGestures();
            toDrain = writer;
            writer = null;
        }

        toDrain.shutdown();
        try {
            if (!toDrain.awaitTermination(30, TimeUnit.SECONDS)) {
                log.error("Timed out waiting for mouse gestures to be written: {}", currentLabel);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Mouse Recording STOPPED: {}", currentLabel);
    }

    @Override
    public synchronized MouseEvent mousePressed(MouseEvent e) {
        if (!isRecording) return e;

        saveGesture(e);
        movementBuffer.clear();
        gestureStartTime = System.currentTimeMillis();
        return e;
    }

    @Override
    public synchronized MouseEvent mouseMoved(MouseEvent e) {
        if (isRecording) recordPoint(e);
        return e;
    }

    @Override
    public synchronized MouseEvent mouseDragged(MouseEvent e) {
        if (isRecording) recordPoint(e);
        return e;
    }

    /**
     * Appends the event's position to the current gesture, skipping repeats of the last point. When the
     * gesture reaches {@link #MAX_POINTS_PER_GESTURE} it is discarded and a fresh one begins, so a click
     * always ends a gesture whose tail is contiguous.
     *
     * @param e The mouse event to record
     */
    private void recordPoint(MouseEvent e) {
        if (movementBuffer.size() >= MAX_POINTS_PER_GESTURE) {
            movementBuffer.clear();
            gestureStartTime = System.currentTimeMillis();
        }
        if (gestureStartTime == -1) {
            gestureStartTime = System.currentTimeMillis();
        }

        if (!movementBuffer.isEmpty()) {
            RecordedPoint last = movementBuffer.get(movementBuffer.size() - 1);
            if (last.getX() == e.getX() && last.getY() == e.getY()) {
                return;
            }
        }

        movementBuffer.add(new RecordedPoint(e.getX(), e.getY(), System.currentTimeMillis() - gestureStartTime));
    }

    /**
     * Turns the buffered movement into a gesture ending at the triggering click and schedules a write
     * once {@link #BATCH_SIZE} gestures have accumulated.
     *
     * @param triggerEvent The click that ended the gesture
     */
    private void saveGesture(MouseEvent triggerEvent) {
        if (movementBuffer.isEmpty()) return;

        RecordedPoint startPoint = movementBuffer.get(0);
        gestureBuffer.add(new MouseGesture(
                currentLabel,
                System.currentTimeMillis() - gestureStartTime,
                startPoint.getX(), startPoint.getY(),
                triggerEvent.getX(), triggerEvent.getY(),
                triggerEvent.getButton(),
                new ArrayList<>(movementBuffer)
        ));

        if (gestureBuffer.size() >= BATCH_SIZE) {
            flushGestures();
        }
    }

    /**
     * Hands the buffered gestures to the writer as one job bound to the current file. Called with the
     * monitor held; the job itself touches no recorder state.
     */
    private void flushGestures() {
        if (gestureBuffer.isEmpty()) return;
        List<MouseGesture> batch = new ArrayList<>(gestureBuffer);
        gestureBuffer.clear();
        Path target = currentFile;
        writer.execute(() -> write(target, batch));
    }

    private void write(Path target, List<MouseGesture> batch) {
        try (BufferedWriter out = Files.newBufferedWriter(target, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (MouseGesture gesture : batch) {
                out.write(gson.toJson(gesture));
                out.newLine();
            }
        } catch (IOException e) {
            log.error("Failed to write mouse gestures to {}", target, e);
        }
    }

    @Override public MouseEvent mouseReleased(MouseEvent e) { return e; }
    @Override public MouseEvent mouseClicked(MouseEvent e) { return e; }
    @Override public MouseEvent mouseEntered(MouseEvent e) { return e; }
    @Override public MouseEvent mouseExited(MouseEvent e) { return e; }
}
