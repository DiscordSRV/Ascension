/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.logging.backend.impl;

import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.LogFilter;
import com.discordsrv.common.core.logging.backend.LoggingBackend;
import com.discordsrv.common.logging.LogAppender;
import com.discordsrv.common.logging.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class JavaLoggerImpl implements Logger, LoggingBackend {

    private static final Map<Level, LogLevel> LEVELS = new HashMap<>();
    private static final Map<LogLevel, Level> LEVELS_REVERSE = new HashMap<>();

    private static void put(Level level, LogLevel logLevel) {
        LEVELS.put(level, logLevel);
        LEVELS_REVERSE.put(logLevel, level);
    }

    static {
        put(Level.INFO, LogLevel.INFO);
        put(Level.WARNING, LogLevel.WARNING);
        put(Level.SEVERE, LogLevel.ERROR);
    }

    private final java.util.logging.Logger logger;
    private final Map<LogAppender, HandlerImpl> appenders = new HashMap<>();
    private FilterProxy filterProxy;

    public static JavaLoggerImpl getRoot() {
        return new JavaLoggerImpl(java.util.logging.Logger.getLogger(""));
    }

    public JavaLoggerImpl(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(@Nullable String loggerName, @NotNull LogLevel level, @Nullable String message, @Nullable Throwable throwable) {
        Level logLevel = LEVELS_REVERSE.get(level);

        boolean anythingAdded = false;
        StringBuilder stringBuilder = new StringBuilder(message != null ? message.length() : 0);

        // Unknown level
        if (logLevel == null) {
            logLevel = Level.INFO;
            stringBuilder.append("[").append(level.name()).append("] ");
            anythingAdded = true;
        }

        if (message != null) {
            stringBuilder.append(message);
            anythingAdded = true;
        }

        String finalMessage = anythingAdded ? stringBuilder.toString() : null;
        logger.log(logLevel, finalMessage, throwable);
    }

    @Override
    public boolean addFilter(LogFilter filter) {
        if (filterProxy == null) {
            // The Java logger only allows for *one* filter,
            // so we're going to proxy it in case somebody else is using it
            filterProxy = new FilterProxy(logger.getFilter());
            logger.setFilter(filterProxy);
        }
        return filterProxy.filters.add(filter);
    }

    @Override
    public boolean removeFilter(LogFilter filter) {
        if (filterProxy == null) {
            return false;
        }

        List<LogFilter> filters = filterProxy.filters;
        boolean success = filters.remove(filter);

        Filter currentFilter = logger.getFilter();
        if (filters.isEmpty() && currentFilter == filterProxy) {
            // If we don't have any filters, and the current filter is our proxy,
            // change the filter back to what it was before & discard the proxy
            logger.setFilter(filterProxy.parent);
            filterProxy = null;
        }

        return success;
    }

    @Override
    public boolean addAppender(LogAppender appender) {
        HandlerImpl handler = new HandlerImpl(appender);
        appenders.put(appender, handler);
        logger.addHandler(handler);
        return true;
    }

    @Override
    public boolean removeAppender(LogAppender appender) {
        HandlerImpl handler = appenders.get(appender);
        if (handler != null) {
            logger.removeHandler(handler);
            return true;
        }
        return false;
    }

    private static class FilterProxy implements Filter {

        private final Filter parent;
        protected final List<LogFilter> filters = new ArrayList<>();

        public FilterProxy(Filter parent) {
            this.parent = parent;
        }

        @Override
        public boolean isLoggable(LogRecord record) {
            if (filters.isEmpty()) {
                return true;
            }

            Level level = record.getLevel();
            LogLevel logLevel = LEVELS.computeIfAbsent(level, key -> LogLevel.of(key.getName()));

            String message = record.getMessage();
            Throwable throwable = record.getThrown();
            for (LogFilter filter : filters) {
                LogFilter.Result result = filter.filter(record.getLoggerName(), logLevel, message, throwable);
                if (result == LogFilter.Result.BLOCK) {
                    return false;
                } else if (result == LogFilter.Result.ACCEPT) {
                    return true;
                }
            }

            return parent == null || parent.isLoggable(record);
        }
    }

    private static class HandlerImpl extends Handler {

        private final LogAppender appender;

        public HandlerImpl(LogAppender appender) {
            this.appender = appender;
        }

        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            LogLevel logLevel = LEVELS.computeIfAbsent(level, key -> LogLevel.of(key.getName()));
            appender.append(record.getLoggerName(), logLevel, record.getMessage(), record.getThrown());
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
    }
}
