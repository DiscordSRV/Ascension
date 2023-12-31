package com.discordsrv.common.console.entry;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.common.logging.LogLevel;

import java.time.ZonedDateTime;

/**
 * A raw log entry from a platform logger. May be parsed to become a {@link LogMessage}.
 */
public class LogEntry {

    private final String loggerName;
    private final LogLevel level;
    private final String message;
    private final Throwable throwable;
    private final ZonedDateTime logTime;

    public LogEntry(String loggerName, LogLevel level, String message, Throwable throwable) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.throwable = throwable;
        this.logTime = ZonedDateTime.now();
    }

    @Placeholder("logger_name")
    public String loggerName() {
        return loggerName;
    }

    @Placeholder("log_level")
    public LogLevel level() {
        return level;
    }

    public String message() {
        return message;
    }

    public Throwable throwable() {
        return throwable;
    }

    @Placeholder(value = "log_time", relookup = "date")
    public ZonedDateTime logTime() {
        return logTime;
    }

}
