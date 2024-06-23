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

package com.discordsrv.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

public interface Logger {

    default void info(String message) {
        log(null, LogLevel.INFO, message, null);
    }

    default void warning(String message) {
        warning(message, null);
    }
    default void warning(Throwable throwable) {
        warning(null, throwable);
    }
    default void warning(String message, Throwable throwable) {
        log(null, LogLevel.WARNING, message, throwable);
    }

    default void error(String message) {
        error(message, null);
    }
    default void error(Throwable throwable) {
        error(null, throwable);
    }
    default void error(String message, Throwable throwable) {
        log(null, LogLevel.ERROR, message, throwable);
    }

    default void debug(String message) {
        debug(message, null);
    }
    default void debug(Throwable throwable) {
        debug(null, throwable);
    }
    default void debug(String message, Throwable throwable) {
        log(null, LogLevel.DEBUG, message, throwable);
    }

    default void trace(String message) {
        trace(message, null);
    }
    default void trace(Throwable throwable) {
        trace(null, throwable);
    }
    default void trace(String message, Throwable throwable) {
        log(null, LogLevel.TRACE, message, throwable);
    }
    
    void log(@Nullable String loggerName, @NotNull LogLevel logLevel, @Nullable String message, @Nullable Throwable throwable);

    default String getStackTrace(Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter, true);
        throwable.printStackTrace(printWriter);
        return stringWriter.getBuffer().toString();
    }

}
