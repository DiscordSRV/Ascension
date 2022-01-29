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

package com.discordsrv.velocity.console;

import com.discordsrv.common.console.Console;
import com.discordsrv.common.logging.backend.LoggingBackend;
import com.discordsrv.common.logging.backend.impl.Log4JLoggerImpl;
import com.discordsrv.velocity.VelocityDiscordSRV;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

public class VelocityConsole implements Console {

    private final VelocityDiscordSRV discordSRV;
    private final LoggingBackend loggingBackend;

    public VelocityConsole(VelocityDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.loggingBackend = Log4JLoggerImpl.getRoot();
    }

    @Override
    public boolean hasPermission(String permission) {
        return discordSRV.proxy().getConsoleCommandSource().hasPermission(permission);
    }

    @Override
    public void runCommand(String command) {
        discordSRV.proxy().getCommandManager().executeAsync(
                discordSRV.proxy().getConsoleCommandSource(), command);
    }

    @Override
    public LoggingBackend loggingBackend() {
        return loggingBackend;
    }

    @Override
    public @NotNull Audience audience() {
        return discordSRV.proxy().getConsoleCommandSource();
    }
}
