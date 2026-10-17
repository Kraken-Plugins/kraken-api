package com.kraken.api.overlay.log;

/**
 * The severity of a captured log line.
 */
public enum LogLevel {
    /** Routine information, also used for levels below WARN. */
    INFO,
    /** A condition that may need attention. */
    WARN,
    /** A failure. */
    ERROR
}