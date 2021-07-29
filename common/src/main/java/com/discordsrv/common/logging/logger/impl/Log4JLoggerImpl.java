/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.logging.logger.impl;

import com.discordsrv.common.logging.logger.LogLevel;
import com.discordsrv.common.logging.logger.backend.LogAppender;
import com.discordsrv.common.logging.logger.backend.LogFilter;
import com.discordsrv.common.logging.logger.backend.LoggingBackend;
import com.discordsrv.common.logging.logger.Logger;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.message.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Log4JLoggerImpl implements Logger, LoggingBackend {

    private static final DualHashBidiMap<Level, LogLevel> LEVELS = new DualHashBidiMap<>();

    static {
        LEVELS.put(Level.INFO, LogLevel.INFO);
        LEVELS.put(Level.WARN, LogLevel.WARNING);
        LEVELS.put(Level.ERROR, LogLevel.ERROR);
        LEVELS.put(Level.DEBUG, LogLevel.DEBUG);
        LEVELS.put(Level.TRACE, LogLevel.TRACE);
    }

    private final org.apache.logging.log4j.Logger logger;
    private final Map<LogFilter, Filter> filters = new HashMap<>();
    private final Map<LogAppender, Appender> appenders = new HashMap<>();

    public static Log4JLoggerImpl getRoot() {
        return new Log4JLoggerImpl(LogManager.getRootLogger());
    }

    public Log4JLoggerImpl(org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(@NotNull LogLevel level, @Nullable String message, @Nullable Throwable throwable) {
        Level logLevel = LEVELS.getKey(level);
        logger.log(logLevel, message, throwable);
    }

    @Override
    public boolean addFilter(LogFilter filter) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) logger;
            Log4jFilter log4jFilter = new Log4jFilter(filter);
            loggerImpl.addFilter(log4jFilter);
            filters.put(filter, log4jFilter);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeFilter(LogFilter filter) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            // A simple little workaround for removing filters
            try {
                Field configField = null;
                Class<?> targetClass = logger.getClass();

                // Get a field named config or privateConfig from the logger class or any of it's super classes
                while (targetClass != null) {
                    try {
                        configField = targetClass.getDeclaredField("config");
                        break;
                    } catch (NoSuchFieldException ignored) {}

                    try {
                        configField = targetClass.getDeclaredField("privateConfig");
                        break;
                    } catch (NoSuchFieldException ignored) {}

                    targetClass = targetClass.getSuperclass();
                }

                if (configField != null) {
                    configField.setAccessible(true);

                    Object config = configField.get(logger);
                    Field configField2 = config.getClass().getDeclaredField("config");
                    configField2.setAccessible(true);

                    Object config2 = configField2.get(config);
                    if (config2 instanceof org.apache.logging.log4j.core.filter.Filterable) {
                        Filter log4jFilter = filters.remove(filter);
                        if (log4jFilter != null) {
                            ((org.apache.logging.log4j.core.filter.Filterable) config2)
                                    .removeFilter(log4jFilter);
                            return true;
                        }
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException("Failed to remove filter", t);
            }
        }
        return false;
    }

    @Override
    public boolean addAppender(LogAppender appender) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) logger;
            Appender log4jAppender = new Log4jAppender(appender);
            loggerImpl.addAppender(log4jAppender);
            appenders.put(appender, log4jAppender);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAppender(LogAppender appender) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) logger;
            Appender log4jAppender = appenders.get(appender);
            loggerImpl.addAppender(log4jAppender);
            return true;
        }
        return false;
    }

    private static class Log4jFilter implements Filter {

        private final LogFilter filter;

        public Log4jFilter(LogFilter filter) {
            this.filter = filter;
        }

        @Override
        public Result getOnMismatch() {
            return Result.NEUTRAL;
        }

        @Override
        public Result getOnMatch() {
            return Result.NEUTRAL;
        }

        @Override
        public Result filter(org.apache.logging.log4j.core.Logger logger, Level level, Marker marker, String msg, Object... params) {
            return filter(
                    logger.getName(),
                    level,
                    msg,
                    null
            );
        }

        @Override
        public Result filter(org.apache.logging.log4j.core.Logger logger, Level level, Marker marker, Object msg, Throwable t) {
            return filter(
                    logger.getName(),
                    level,
                    msg.toString(),
                    t
            );
        }

        @Override
        public Result filter(org.apache.logging.log4j.core.Logger logger, Level level, Marker marker, Message msg, Throwable t) {
            return filter(
                    logger.getName(),
                    level,
                    msg.getFormattedMessage(),
                    t
            );
        }

        @Override
        public Result filter(LogEvent event) {
            return filter(
                    event.getLoggerName(),
                    event.getLevel(),
                    event.getMessage().getFormattedMessage(),
                    event.getThrown()
            );
        }

        private Result filter(String loggerName, Level level, String message, Throwable throwable) {
            LogLevel logLevel = LEVELS.computeIfAbsent(level, key -> LogLevel.of(key.name()));
            LogFilter.Result result = filter.filter(loggerName, logLevel, message, throwable);
            switch (result) {
                case BLOCK: return Result.DENY;
                case ACCEPT: return Result.ACCEPT;
                default:
                case IGNORE: return Result.NEUTRAL;
            }
        }
    }

    private static class Log4jAppender extends AbstractAppender {

        private final LogAppender appender;

        protected Log4jAppender(LogAppender appender) {
            super("DiscordSRV Appender", null, null, false);
            this.appender = appender;
        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public void append(LogEvent event) {
            LogLevel level = LEVELS.computeIfAbsent(event.getLevel(), key -> LogLevel.of(key.name()));
            appender.append(event.getLoggerName(), level, event.getMessage().getFormattedMessage(), event.getThrown());
        }
    }
}
