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

package com.discordsrv.bungee.console;

import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.bungee.command.game.sender.BungeeCommandSender;
import com.discordsrv.bungee.console.executor.BungeeCommandExecutorProvider;
import com.discordsrv.common.command.game.abstraction.executor.CommandExecutorProvider;
import com.discordsrv.common.core.logging.backend.LoggingBackend;
import com.discordsrv.common.core.logging.backend.impl.JavaLoggerImpl;
import com.discordsrv.common.feature.console.Console;

public class BungeeConsole extends BungeeCommandSender implements Console {

    private final LoggingBackend loggingBackend;
    private final BungeeCommandExecutorProvider executorProvider;

    public BungeeConsole(BungeeDiscordSRV discordSRV) {
        super(discordSRV, discordSRV.proxy().getConsole(), () -> discordSRV.audiences().console());
        this.loggingBackend = JavaLoggerImpl.getRoot();
        this.executorProvider = new BungeeCommandExecutorProvider(discordSRV);
    }

    @Override
    public LoggingBackend loggingBackend() {
        return loggingBackend;
    }

    @Override
    public CommandExecutorProvider commandExecutorProvider() {
        return executorProvider;
    }
}
