/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.logging.impl;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.LogLevel;
import com.discordsrv.common.logging.Logger;
import net.dv8tion.jda.api.Permission;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class DiscordSRVLogger implements Logger {

    private static final DateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("EEE HH:mm:ss z");
    private static final String LOG_LINE_FORMAT = "[%s] [%s] %s";
    private static final String LOG_FILE_NAME_FORMAT = "%s-%s.log";

    private final Queue<LogEntry> linesToAdd = new ConcurrentLinkedQueue<>();

    private final DiscordSRV discordSRV;
    private final Path logsDirectory;
    private final List<Path> debugLogs;

    public DiscordSRVLogger(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logsDirectory = discordSRV.dataDirectory().resolve("logs");
        if (!Files.exists(logsDirectory)) {
            try {
                Files.createDirectory(logsDirectory);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        this.debugLogs = rotateLog("debug", 3);
        discordSRV.scheduler().runAtFixedRate(this::processLines, 0, 2, TimeUnit.SECONDS);
    }

    public List<Path> getDebugLogs() {
        return debugLogs;
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
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void log(@Nullable String loggerName, @NotNull LogLevel logLevel, @Nullable String message, @Nullable Throwable throwable) {
        if (throwable != null && throwable.getMessage() != null
                && (throwable.getStackTrace() == null || throwable.getStackTrace().length == 0)) {
            // Empty stack trace
            message = (message != null ? message + ": " : "") + throwable.getMessage();
            throwable = null;
        }
        if (throwable instanceof InsufficientPermissionException) {
            Permission permission = ((InsufficientPermissionException) throwable).getPermission();
            String msg = "The bot is missing the \"" + permission.getName() + "\" permission";
            if (message == null) {
                message = msg;
            } else {
                message += ": " + msg;
            }
            doLog(loggerName, logLevel, message, null);
            doLog(loggerName, LogLevel.DEBUG, null, throwable);
            return;
        }

        doLog(loggerName, logLevel, message, throwable);
    }

    private void doLog(String loggerName, LogLevel logLevel, String message, Throwable throwable) {
        if (logLevel != LogLevel.DEBUG && logLevel != LogLevel.TRACE) {
            discordSRV.platformLogger().log(null, logLevel, message, throwable);
        }

        // TODO: handle trace & hikari
        Path debugLog = debugLogs.isEmpty() ? null : debugLogs.get(0);
        if (debugLog == null || logLevel == LogLevel.TRACE/* || loggerName.equals("Hikari")*/) {
            return;
        }
        long time = System.currentTimeMillis();
        linesToAdd.add(new LogEntry(debugLog, loggerName, time, logLevel, message, throwable));
    }

    private void processLines() {
        LogEntry entry;
        while ((entry = linesToAdd.poll()) != null) {
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

            String timestamp = DATE_TIME_FORMATTER.format(time);
            String line = String.format(LOG_LINE_FORMAT, timestamp, logLevel.name(), message) + "\n";
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
                discordSRV.platformLogger().error("Failed to write to debug log", e);
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
