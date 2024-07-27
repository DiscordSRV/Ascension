/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.feature.console;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.message.DiscordMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.ConsoleConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.logging.backend.LoggingBackend;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.console.entry.LogEntry;
import com.discordsrv.common.logging.LogAppender;
import com.discordsrv.common.logging.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class ConsoleModule extends AbstractModule<DiscordSRV> implements LogAppender {

    private LoggingBackend backend;
    private final List<SingleConsoleHandler> handlers = new ArrayList<>();

    public ConsoleModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CONSOLE"));
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        boolean anyExecutors = discordSRV.config().console.stream().anyMatch(config -> config.commandExecution.enabled);
        if (anyExecutors) {
            return EnumSet.of(DiscordGatewayIntent.GUILD_MESSAGES, DiscordGatewayIntent.MESSAGE_CONTENT);
        }

        return Collections.emptySet();
    }

    @Override
    public boolean canEnableBeforeReady() {
        return discordSRV.config() != null;
    }

    @Override
    public void enable() {
        backend = discordSRV.console().loggingBackend();
        backend.addAppender(this);
    }

    @Override
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        List<ConsoleConfig> configs = discordSRV.config().console;
        Set<ConsoleConfig> uncheckedConfigs = new LinkedHashSet<>(configs);

        for (int i = handlers.size() - 1; i >= 0; i--) {
            SingleConsoleHandler handler = handlers.get(i);

            ConsoleConfig matchingConfig = null;
            for (ConsoleConfig config : configs) {
                if (config.channel.equals(handler.getConfig().channel)) {
                    matchingConfig = config;
                    break;
                }
            }
            if (matchingConfig != null) {
                handler.setConfig(matchingConfig);
                uncheckedConfigs.remove(matchingConfig);
            } else {
                handlers.remove(i);
                discordSRV.scheduler().run(handler::shutdown);
            }
        }

        for (ConsoleConfig config : uncheckedConfigs) {
            DestinationConfig.Single destination = config.channel;
            if (destination.channelId == 0L) {
                logger().debug("Skipping a console handler due to lack of channel");
                continue;
            }
            if (config.appender.outputMode == ConsoleConfig.OutputMode.OFF && !config.commandExecution.enabled) {
                logger().debug("Skipping console handler because output mode is OFF and command execution is disabled");
                continue;
            }

            handlers.add(new SingleConsoleHandler(discordSRV, logger(), config));
        }
        logger().debug(handlers.size() + " console handlers active");
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

    @Subscribe
    public void onDiscordMessageReceived(DiscordMessageReceiveEvent event) {
        for (SingleConsoleHandler handler : handlers) {
            handler.handleDiscordMessageReceived(event);
        }
    }
}
