package com.discordsrv.common.console.entry;

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
