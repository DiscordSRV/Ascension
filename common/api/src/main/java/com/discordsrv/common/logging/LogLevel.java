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

package com.discordsrv.common.logging;

import java.util.Locale;

public interface LogLevel {

    LogLevel INFO = StandardLogLevel.INFO;
    LogLevel WARNING = StandardLogLevel.WARNING;
    LogLevel ERROR = StandardLogLevel.ERROR;
    LogLevel DEBUG = StandardLogLevel.DEBUG;
    LogLevel TRACE = StandardLogLevel.TRACE;

    static LogLevel of(String name) {
        try {
            return StandardLogLevel.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return new CustomLogLevel(name);
        }
    }

    String name();

    enum StandardLogLevel implements LogLevel {
        INFO,
        WARNING,
        ERROR,
        DEBUG,
        TRACE
    }

    class CustomLogLevel implements LogLevel {

        private final String name;

        public CustomLogLevel(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

}
