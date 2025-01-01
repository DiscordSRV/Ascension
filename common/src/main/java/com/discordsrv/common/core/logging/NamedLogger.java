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

package com.discordsrv.common.core.logging;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NamedLogger implements Logger {

    private final Logger logger;
    private final String name;

    public NamedLogger(DiscordSRV discordSRV, String name) {
        this(discordSRV.logger(), name);
    }

    public NamedLogger(Logger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public void log(@Nullable String loggerName, @NotNull LogLevel logLevel, @Nullable String message, @Nullable Throwable throwable) {
        logger.log(name, logLevel, message, throwable);
    }
}
