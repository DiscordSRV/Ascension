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

package com.discordsrv.common.logging.backend.impl;

import com.discordsrv.common.logging.LogLevel;
import com.discordsrv.common.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SLF4JLoggerImpl implements Logger {

    private final com.discordsrv.unrelocate.org.slf4j.Logger logger;

    public SLF4JLoggerImpl(com.discordsrv.unrelocate.org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(@Nullable String loggerName, @NotNull LogLevel level, @Nullable String message, @Nullable Throwable throwable) {
        if (!(level instanceof LogLevel.StandardLogLevel)) {
            return;
        }
        switch ((LogLevel.StandardLogLevel) level) {
            case INFO: {
                logger.info(message, throwable);
                return;
            }
            case WARNING: {
                logger.warn(message, throwable);
                return;
            }
            case ERROR: {
                logger.error(message, throwable);
                return;
            }
            case DEBUG: {
                logger.debug(message, throwable);
                return;
            }
            case TRACE: {
                logger.trace(message, throwable);
                return;
            }
            default: throw new IllegalStateException(level.name() + " was not specified");
        }
    }
}
