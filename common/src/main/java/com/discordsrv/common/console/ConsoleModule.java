package com.discordsrv.common.console;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.console.entry.LogEntry;
import com.discordsrv.common.logging.LogAppender;
import com.discordsrv.common.logging.LogLevel;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.logging.backend.LoggingBackend;
import com.discordsrv.common.module.type.AbstractModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConsoleModule extends AbstractModule<DiscordSRV> implements LogAppender {

    private LoggingBackend backend;
    private final List<SingleConsoleHandler> handlers = new ArrayList<>();

    public ConsoleModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CONSOLE"));
    }

    @Override
    public void enable() {
        backend = discordSRV.console().loggingBackend();
        backend.addAppender(this);

        reload();
    }

    @Override
    public void reloadNoResult() {
        handlers.add(new SingleConsoleHandler(discordSRV, new ConsoleConfig())); // TODO
    }

    @Override
    public void disable() {
        if (backend != null) {
            backend.removeAppender(this);
        }
    }

    @Override
    public void append(
            @Nullable String loggerName,
            @NotNull LogLevel logLevel,
            @Nullable String message,
            @Nullable Throwable throwable
    ) {
        LogEntry entry = new LogEntry(loggerName, logLevel, message, throwable);
        for (SingleConsoleHandler handler : handlers) {
            handler.queue(entry);
        }
    }
}
