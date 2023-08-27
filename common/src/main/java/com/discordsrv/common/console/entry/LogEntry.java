package com.discordsrv.common.console.entry;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.common.logging.LogLevel;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

public class LogEntry {

    @Placeholder("logger_name")
    private final String loggerName;
    @Placeholder("log_level")
    private final LogLevel level;
    private final String message;
    private final Throwable throwable;
    private final Instant logTime;

    public LogEntry(String loggerName, LogLevel level, String message, Throwable throwable) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.throwable = throwable;
        this.logTime = Instant.now();
    }

    public String loggerName() {
        return loggerName;
    }

    public LogLevel level() {
        return level;
    }

    public String message() {
        return message;
    }

    public Throwable throwable() {
        return throwable;
    }

    public Instant logTime() {
        return logTime;
    }

    @Placeholder("log_time")
    public PlaceholderLookupResult _logTimePlaceholder(@PlaceholderRemainder String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format); // TODO: cache

        Set<Object> extras = new LinkedHashSet<>();
        extras.add(formatter);
        extras.add(logTime);

        return PlaceholderLookupResult.newLookup("date", extras);
    }
}
