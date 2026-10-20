package com.kraken.api.overlay.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.Getter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A single captured log line with its time, level, source logger, thread and message.
 */
@Getter
public class LogEntry {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String timestamp;
    private final LogLevel level;
    private final String source;   // abbreviated class name
    private final String thread;
    private final String message;

    private LogEntry(LogLevel level, String source, String thread, String message) {
        this.timestamp = LocalTime.now().format(FORMATTER);
        this.level = level;
        this.source = source;
        this.thread = thread;
        this.message = message;
    }

    /**
     * Creates an entry from a Logback event. The level is {@link LogLevel#ERROR} for ERROR and above,
     * {@link LogLevel#WARN} for WARN, and {@link LogLevel#INFO} for anything lower. The source is the logger
     * name abbreviated to 36 characters, and the thread and message are taken from the event.
     *
     * @param event The logging event to convert
     * @return A new entry describing the event
     */
    public static LogEntry from(ILoggingEvent event) {
        LogLevel level = mapLevel(event.getLevel());
        String source = abbreviate(event.getLoggerName(), 36);
        return new LogEntry(level, source, event.getThreadName(), event.getFormattedMessage());
    }

    /**
     * Creates an entry for a message that was not logged through Logback. The entry is timestamped
     * with the current time and records the name of the calling thread.
     *
     * @param level The severity of the message
     * @param source The name shown as the source of the message
     * @param message The message text
     * @return A new entry for the message
     */
    public static LogEntry of(LogLevel level, String source, String message) {
        return new LogEntry(level, source, Thread.currentThread().getName(), message);
    }

    private static LogLevel mapLevel(ch.qos.logback.classic.Level level) {
        if (level.isGreaterOrEqual(ch.qos.logback.classic.Level.ERROR)) return LogLevel.ERROR;
        if (level.isGreaterOrEqual(ch.qos.logback.classic.Level.WARN))  return LogLevel.WARN;
        return LogLevel.INFO;
    }

    /**
     * Mimics Logback's %logger{targetLen} abbreviation.
     * "com.krakenplugins.example.fishing.script.state.karamja.FishKaramja"
     *  → "c.k.e.f.s.state.karamja.FishKaramja"
     */
    private static String abbreviate(String fqcn, int targetLen) {
        if (fqcn.length() <= targetLen) return fqcn;
        String[] parts = fqcn.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 2; i++) {
            sb.append(parts[i].charAt(0)).append('.');
        }
        sb.append(parts[parts.length - 2]).append('.').append(parts[parts.length - 1]);
        return sb.toString();
    }
}
