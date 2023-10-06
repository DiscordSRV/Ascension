package com.discordsrv.common.console.entry;

/**
 * A {@link LogEntry} with formatting.
 */
public class LogMessage {

    private final LogEntry entry;
    private final String formatted;

    public LogMessage(LogEntry entry, String formatted) {
        this.entry = entry;
        this.formatted = formatted;
    }

    public LogEntry entry() {
        return entry;
    }

    public String formatted() {
        return formatted;
    }
}
