package com.kraken.api.overlay.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

class OverlayAppender extends AppenderBase<ILoggingEvent> {

    private final PluginLogger pluginLogger;

    OverlayAppender(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
        setName("OverlayAppender");
    }

    @Override
    protected void append(ILoggingEvent event) {
        pluginLogger.push(LogEntry.from(event));
    }
}