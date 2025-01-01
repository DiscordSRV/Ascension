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

package com.discordsrv.common.feature.console.entry;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.common.logging.LogLevel;

import java.time.ZonedDateTime;

/**
 * A raw log entry from a platform logger. May be parsed to become a {@link LogMessage}.
 */
public class LogEntry {

    private final String loggerName;
    private final LogLevel level;
    private final String message;
    private final Throwable throwable;
    private final ZonedDateTime logTime;

    public LogEntry(String loggerName, LogLevel level, String message, Throwable throwable) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.throwable = throwable;
        this.logTime = ZonedDateTime.now();
    }

    @Placeholder("logger_name")
    public String loggerName() {
        return loggerName;
    }

    @Placeholder("log_level")
    public LogLevel level() {
        return level;
    }

    public String message() {
        return message;
    }

    public Throwable throwable() {
        return throwable;
    }

    @Placeholder(value = "log_time", relookup = "date")
    public ZonedDateTime logTime() {
        return logTime;
    }

}
