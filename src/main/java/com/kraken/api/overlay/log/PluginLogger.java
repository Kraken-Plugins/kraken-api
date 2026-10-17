package com.kraken.api.overlay.log;

import ch.qos.logback.classic.Logger;
import com.google.inject.Singleton;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Keeps the most recent log lines logged under a plugin's package so they can be drawn on an overlay.
 * Call {@link #attach(String)} from {@code Plugin#startUp} and {@link #detach()} from {@code Plugin#shutDown}.
 */
@Singleton
public class PluginLogger {

    private static final int DEFAULT_MAX_ENTRIES = 6;

    private final int maxEntries;
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private OverlayAppender appender;
    private String attachedPackage;

    /**
     * Creates a logger that keeps the 6 most recent entries.
     */
    public PluginLogger() {
        this(DEFAULT_MAX_ENTRIES);
    }

    /**
     * Creates a logger that keeps a limited number of recent entries. When the buffer is full, the oldest
     * entry is dropped to make room for a new one.
     *
     * @param maxEntries The maximum number of entries to keep
     */
    public PluginLogger(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Call from {@code Plugin#startUp}. Attaches to the Logback logger for the given
     * package, capturing every @Slf4j call in any class underneath it.
     *
     * @param pluginPackage Root package of the plugin, e.g. "com.example.myplugin"
     */
    public synchronized void attach(String pluginPackage) {
        if (appender != null) {
            throw new IllegalStateException("PluginLogger is already attached — call detach() first.");
        }
        this.attachedPackage = pluginPackage;
        appender = new OverlayAppender(this);
        appender.start();
        getLogbackLogger(pluginPackage).addAppender(appender);
    }

    /**
     * Call from {@code Plugin#shutDown}. Removes the appender and clears the buffer.
     */
    public synchronized void detach() {
        if (appender == null) return;
        getLogbackLogger(attachedPackage).detachAppender(appender);
        appender.stop();
        appender = null;
        attachedPackage = null;
        entries.clear();
    }

    /**
     * Returns a copy of the buffered entries in the order they were logged, oldest first.
     * Changes to the logger after the call do not affect the returned list.
     *
     * @return A new list of the buffered log entries, possibly empty
     */
    public synchronized List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Removes all buffered entries. The logger stays attached if it was attached.
     */
    public synchronized void clear() {
        entries.clear();
    }

    synchronized void push(LogEntry entry) {
        if (entries.size() >= maxEntries) entries.pollFirst();
        entries.addLast(entry);
    }

    private static Logger getLogbackLogger(String packageName) {
        return (Logger) LoggerFactory.getLogger(packageName);
    }
}