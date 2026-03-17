package com.kraken.api.overlay.log;

import ch.qos.logback.classic.Logger;
import com.google.inject.Singleton;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Singleton
public class PluginLogger {

    private static final int DEFAULT_MAX_ENTRIES = 6;

    private final int maxEntries;
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private OverlayAppender appender;
    private String attachedPackage;

    public PluginLogger() {
        this(DEFAULT_MAX_ENTRIES);
    }

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

    public synchronized List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }

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