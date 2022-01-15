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

package com.discordsrv.common.logging.adapter;

import com.discordsrv.common.logging.LogAppender;
import com.discordsrv.common.logging.LogLevel;
import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

public class DependencyLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger {

    private static LogAppender APPENDER;

    public static void setAppender(LogAppender appender) {
        APPENDER = appender;
    }

    private final String name;

    public DependencyLoggerAdapter(String name) {
        this.name = name;
    }

    private String format(String message, Object arg1) {
        return MessageFormatter.format(message, arg1).getMessage();
    }

    private String format(String message, Object arg1, Object arg2) {
        return MessageFormatter.format(message, arg1, arg2).getMessage();
    }

    private String format(String message, Object[] arguments) {
        return MessageFormatter.arrayFormat(message, arguments).getMessage();
    }

    @Override
    public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t) {
        if (APPENDER == null) {
            // Adapter isn't set, do nothing
            return;
        }
        APPENDER.append(name, getLevel(level), format(message, argArray), t);
    }

    private LogLevel getLevel(int level) {
        switch (level) {
            case LocationAwareLogger.TRACE_INT:
                return LogLevel.TRACE;
            case LocationAwareLogger.DEBUG_INT:
                return LogLevel.DEBUG;
            case LocationAwareLogger.INFO_INT:
                return LogLevel.INFO;
            case LocationAwareLogger.WARN_INT:
                return LogLevel.WARNING;
            case LocationAwareLogger.ERROR_INT:
                return LogLevel.ERROR;
            default:
                throw new IllegalStateException("Level number " + level + " is not recognized.");
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
        trace(msg, (Throwable) null);
    }

    @Override
    public void trace(String format, Object arg) {
        trace(format(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        trace(format(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        trace(format(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        APPENDER.append(name, LogLevel.TRACE, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        debug(msg, (Throwable) null);
    }

    @Override
    public void debug(String format, Object arg) {
        debug(format(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        debug(format(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        debug(format(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        APPENDER.append(name, LogLevel.DEBUG, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        info(msg, (Throwable) null);
    }

    @Override
    public void info(String format, Object arg) {
        info(format(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        info(format(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        info(format(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        APPENDER.append(name, LogLevel.INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        warn(msg, (Throwable) null);
    }

    @Override
    public void warn(String format, Object arg) {
        warn(format(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        warn(format(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        warn(format(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        APPENDER.append(name, LogLevel.WARNING, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        error(msg, (Throwable) null);
    }

    @Override
    public void error(String format, Object arg) {
        error(format(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        error(format(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        error(format(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        APPENDER.append(name, LogLevel.ERROR, msg, t);
    }
}
