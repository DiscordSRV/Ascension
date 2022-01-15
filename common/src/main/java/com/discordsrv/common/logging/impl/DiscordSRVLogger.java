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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiscordSRVLogger implements Logger {

    private final DiscordSRV discordSRV;

    public DiscordSRVLogger(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void log(@NotNull LogLevel logLevel, @Nullable String message, @Nullable Throwable throwable) {
        if (logLevel == LogLevel.DEBUG || logLevel == LogLevel.TRACE || logLevel instanceof LogLevel.CustomLogLevel) {
            // TODO: handle debug/trace
            return;
        }

        if (throwable instanceof InsufficientPermissionException) {
            Permission permission = ((InsufficientPermissionException) throwable).getPermission();
            String msg = "The bot is missing the \"" + permission.getName() + "\" permission";
            if (message == null) {
                message = msg;
            } else {
                message += ": " + msg;
            }
            discordSRV.platformLogger().log(logLevel, message, null);
            discordSRV.logger().debug(throwable);
            return;
        }

        discordSRV.platformLogger().log(logLevel, message, throwable);
    }
}
