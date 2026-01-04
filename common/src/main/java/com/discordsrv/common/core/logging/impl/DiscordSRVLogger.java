/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.logging.impl;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.DebugConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.exception.MessageException;
import com.discordsrv.common.logging.LogLevel;
import com.discordsrv.common.util.DiscordPermissionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class DiscordSRVLogger implements Logger {

    public static final String LOGS_DIRECTORY_NAME = "logs";

    private static final DateFormat ROTATED_DATE_TIME_FORMATTER = new SimpleDateFormat("EEE HH:mm:ss z");
    private static final DateFormat DAY_DATE_TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss z");
    private static final DateFormat DAY = new SimpleDateFormat("yyyy-MM-dd");
    private static final String ROTATED_LOG_LINE_FORMAT = "[%s] [%s] %s";
    private static final String DAY_LOG_LINE_FORMAT = "[%s] %s";
    private static final String LOG_FILE_NAME_FORMAT = "%s-%s.log";

    private static final List<String> DISABLE_DEBUG_BY_DEFAULT = Collections.singletonList("Hikari");

    private final DiscordSRV discordSRV;

    // Files
    private final Path logsDirectory;
    private final List<Path> debugLogs;

    // File writing
    private final Queue<LogEntry> linesToWrite = new ConcurrentLinkedQueue<>();
    private final Object lineProcessingLock = new Object();
    private Future<?> lineProcessingFuture;

    public DiscordSRVLogger(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logsDirectory = discordSRV.dataDirectory().resolve(LOGS_DIRECTORY_NAME);
        if (!Files.exists(logsDirectory)) {
            try {
                Files.createDirectory(logsDirectory);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        this.debugLogs = rotateLog("debug", 3);
    }

    public void shutdown() {
        lineProcessingFuture.cancel(false);
        flushLines();
    }

    public List<Path> getDebugLogs() {
        return debugLogs;
    }

    public void writeLogForCurrentDay(String label, String message) {
        Path log = logsDirectory.resolve(label + "_" + DAY.format(System.currentTimeMillis()) + ".log");
        scheduleWrite(new LogEntry(log, null, System.currentTimeMillis(), null, message, null));
    }

    @SuppressWarnings("SameParameterValue")
    private List<Path> rotateLog(String label, int amount) {
        try {
            List<Path> logs = new ArrayList<>(amount);
            for (int i = amount; i > 0; i--) {
                Path log = logsDirectory.resolve(String.format(LOG_FILE_NAME_FORMAT, label, i));
                logs.add(0, log);
                if (!Files.exists(log)) {
                    continue;
                }

                if (i == amount) {
                    Files.delete(log);
                    continue;
                }

                Path to = logsDirectory.resolve(String.format(LOG_FILE_NAME_FORMAT, label, i + 1));
                Files.move(log, to);
            }
            return logs;
        } catch (IOException e) {
            doLog("LOGGING", LogLevel.ERROR, "Failed to rotate log", e);
            return null;
        }
    }

    @Override
    public void log(@Nullable String loggerName, @NotNull LogLevel logLevel, @Nullable String message, @Nullable Throwable throwable) {
        StringBuilder stringBuilder = new StringBuilder(message != null ? message : "");

        if (throwable != null && throwable.getMessage() != null && throwable instanceof MessageException) {
            // Empty stack trace
            if (stringBuilder.length() > 0) {
                stringBuilder.append(": ");
            }
            stringBuilder.append(throwable.getMessage());

            for (Throwable suppressed : throwable.getSuppressed()) {
                stringBuilder.append("\n\t- ").append(
                        suppressed instanceof MessageException
                        ? suppressed.getMessage()
                        : ExceptionUtils.getStackTrace(suppressed)
                );
            }
            throwable = null;
        }
        if (throwable instanceof InsufficientPermissionException) {
            InsufficientPermissionException exception = (InsufficientPermissionException) throwable;
            Permission permission = exception.getPermission();

            JDA jda = discordSRV.jda();
            GuildChannel guildChannel = jda != null ? exception.getChannel(jda) : null;
            Guild guild = jda != null ? exception.getGuild(jda) : null;

            String msg = DiscordPermissionUtil.createErrorMessage(guildChannel, guild, EnumSet.of(permission));
            if (stringBuilder.length() > 0) {
                stringBuilder.append(": ");
            }
            stringBuilder.append(msg);

            doLog(loggerName, logLevel, stringBuilder.toString(), null);
            doLog(loggerName, LogLevel.DEBUG, null, throwable);
            return;
        }

        doLog(loggerName, logLevel, stringBuilder.toString(), throwable);
    }

    private void doLog(String loggerName, LogLevel logLevel, String message, Throwable throwable) {
        long time = System.currentTimeMillis();
        MainConfig config = discordSRV.config();
        DebugConfig debugConfig = config != null ? config.debug : null;

        if (logLevel == LogLevel.TRACE || (loggerName != null && logLevel == LogLevel.DEBUG && DISABLE_DEBUG_BY_DEFAULT.contains(loggerName))) {
            if (loggerName == null
                    || debugConfig == null
                    || debugConfig.additionalLevels == null
                    || !debugConfig.additionalLevels.getOrDefault(loggerName, Collections.emptyList()).contains(logLevel.name())) {
                return;
            }
        }

        boolean debugOrTrace = logLevel == LogLevel.DEBUG || logLevel == LogLevel.TRACE;
        boolean logToConsole = debugConfig != null && debugConfig.logToConsole;

        if (!debugOrTrace || logToConsole) {
            String consoleMessage = message;
            LogLevel consoleLevel = logLevel;
            if (debugOrTrace) {
                // Normally DEBUG/TRACE logging isn't enabled, so we convert it to INFO and add the level
                consoleMessage = "[" + logLevel.name() + "]" + (loggerName != null ? " [" + loggerName + "]" : "") + (message != null ? " " + message : "");
                consoleLevel = LogLevel.INFO;
            }
            discordSRV.platformLogger().log(null, consoleLevel, consoleMessage, throwable);
        }

        Path debugLog = debugLogs.isEmpty() ? null : debugLogs.get(0);
        if (debugLog == null) {
            return;
        }

        scheduleWrite(new LogEntry(debugLog, loggerName, time, logLevel, message, throwable));
    }

    @SuppressWarnings("resource")
    private void scheduleWrite(LogEntry entry) {
        if (discordSRV.scheduler().scheduledExecutorService().isShutdown()) {
            return;
        }

        linesToWrite.add(entry);
        synchronized (lineProcessingLock) {
            if (lineProcessingFuture == null || lineProcessingFuture.isDone()) {
                lineProcessingFuture = discordSRV.scheduler().runLater(this::flushLines, Duration.ofSeconds(2));
            }
        }
    }

    private void flushLines() {
        LogEntry entry;
        while ((entry = linesToWrite.poll()) != null) {
            writeToFile(entry.log(), entry.loggerName(), entry.time(), entry.logLevel(), entry.message(), entry.throwable());
        }
    }

    private void writeToFile(Path path, String loggerName, long time, LogLevel logLevel, String message, Throwable throwable) {
        try {
            if (message == null) {
                message = "";
            }
            if (loggerName != null) {
                message = "[" + loggerName + "] " + message;
            }

            String line;
            if (logLevel == null) {
                String timestamp = DAY_DATE_TIME_FORMATTER.format(time);
                line = String.format(DAY_LOG_LINE_FORMAT, timestamp, message) + "\n";
            } else {
                String timestamp = ROTATED_DATE_TIME_FORMATTER.format(time);
                line = String.format(ROTATED_LOG_LINE_FORMAT, timestamp, logLevel.name(), message) + "\n";
            }

            if (throwable != null) {
                line += ExceptionUtils.getStackTrace(throwable) + "\n";
            }

            Path parent = path.getParent();
            if (!Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            Files.write(path, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (Throwable e) {
            try {
                // Prevent infinite loop
                if (discordSRV.status() == DiscordSRV.Status.SHUTDOWN) {
                    return;
                }
                discordSRV.platformLogger().error("Failed to write to log", e);
            } catch (Throwable ignored) {}
        }
    }

    private static class LogEntry {

        private final Path log;
        private final String loggerName;
        private final long time;
        private final LogLevel logLevel;
        private final String message;
        private final Throwable throwable;

        public LogEntry(Path log, String loggerName, long time, LogLevel logLevel, String message, Throwable throwable) {
            this.log = log;
            this.loggerName = loggerName;
            this.time = time;
            this.logLevel = logLevel;
            this.message = message;
            this.throwable = throwable;
        }

        public Path log() {
            return log;
        }

        public String loggerName() {
            return loggerName;
        }

        public long time() {
            return time;
        }

        public LogLevel logLevel() {
            return logLevel;
        }

        public String message() {
            return message;
        }

        public Throwable throwable() {
            return throwable;
        }
    }
}
