package com.discordsrv.common.console.entry;

public class LogMessage {

    private final LogEntry entry;
    private String formatted;

    public LogMessage(LogEntry entry) {
        this.entry = entry;
    }

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }
}
