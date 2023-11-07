package com.discordsrv.common.console;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.console.entry.LogEntry;
import com.discordsrv.common.logging.LogAppender;
import com.discordsrv.common.logging.LogLevel;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.logging.backend.LoggingBackend;
import com.discordsrv.common.module.type.AbstractModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ConsoleModule extends AbstractModule<DiscordSRV> implements LogAppender {

    private LoggingBackend backend;
    private final List<SingleConsoleHandler> handlers = new ArrayList<>();

    public ConsoleModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CONSOLE"));
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Collections.singletonList(DiscordGatewayIntent.MESSAGE_CONTENT);
    }

    @Override
    public void enable() {
        backend = discordSRV.console().loggingBackend();
        backend.addAppender(this);
    }

    @Override
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        for (SingleConsoleHandler handler : handlers) {
            handler.shutdown();
        }
        handlers.clear();

        List<ConsoleConfig> configs = discordSRV.config().console;
        for (ConsoleConfig config : configs) {
            DestinationConfig.Single destination = config.channel;
            if (destination.channelId == 0L && StringUtils.isEmpty(destination.threadName)) {
                logger().debug("Skipping a console handler due to lack of channel");
                continue;
            }
            if (config.appender.outputMode == ConsoleConfig.OutputMode.OFF && !config.commandExecution.enabled) {
                logger().debug("Skipping console handler because output mode is OFF and command execution is disabled");
                continue;
            }

            handlers.add(new SingleConsoleHandler(discordSRV, logger(), config));
        }
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
