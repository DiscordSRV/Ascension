/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import java.util.ArrayList;
import java.util.List;

public class DependencyLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger {

    private static LogAppender APPENDER;

    public static void setAppender(LogAppender appender) {
        APPENDER = appender;
    }

    private final String name;

    public DependencyLoggerAdapter(String name) {
        this.name = name;
    }

    private String format(String message, Object[] arguments) {
        return MessageFormatter.arrayFormat(message, arguments).getMessage();
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

    private void doLog(LogLevel logLevel, String message, Object[] args) {
        List<Object> arguments = new ArrayList<>(args.length);
        Throwable throwable = null;
        for (Object arg : args) {
            if (arg instanceof Throwable) {
                throwable = (Throwable) arg;
                continue;
            }
            arguments.add(arg);
        }
        doLog(logLevel, format(message, arguments.toArray(new Object[0])), throwable);
    }

    private void doLog(LogLevel logLevel, String message, Throwable throwable) {
        if (APPENDER == null) {
            // Adapter isn't set, do nothing
            return;
        }
        APPENDER.append(name, logLevel, message, throwable);
    }

    @Override
    public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t) {
        doLog(getLevel(level), format(message, argArray), t);
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
        doLog(LogLevel.TRACE, format, new Object[] {arg});
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        doLog(LogLevel.TRACE, format, new Object[] {arg1, arg2});
    }

    @Override
    public void trace(String format, Object... arguments) {
        doLog(LogLevel.TRACE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        doLog(LogLevel.TRACE, msg, t);
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
        doLog(LogLevel.DEBUG, format, new Object[] {arg});
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        doLog(LogLevel.DEBUG, format, new Object[] {arg1, arg2});
    }

    @Override
    public void debug(String format, Object... arguments) {
        doLog(LogLevel.DEBUG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        doLog(LogLevel.DEBUG, msg, t);
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
        doLog(LogLevel.INFO, format, new Object[] {arg});
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        doLog(LogLevel.INFO, format, new Object[] {arg1, arg2});
    }

    @Override
    public void info(String format, Object... arguments) {
        doLog(LogLevel.INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        doLog(LogLevel.INFO, msg, t);
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
        doLog(LogLevel.WARNING, format, new Object[] {arg});
    }

    @Override
    public void warn(String format, Object... arguments) {
        doLog(LogLevel.WARNING, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        doLog(LogLevel.WARNING, format, new Object[] {arg1, arg2});
    }

    @Override
    public void warn(String msg, Throwable t) {
        doLog(LogLevel.WARNING, msg, t);
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
        doLog(LogLevel.ERROR, format, new Object[] {arg});
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        doLog(LogLevel.ERROR, format, new Object[] {arg1, arg2});
    }

    @Override
    public void error(String format, Object... arguments) {
        doLog(LogLevel.ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        doLog(LogLevel.ERROR, msg, t);
    }
}
