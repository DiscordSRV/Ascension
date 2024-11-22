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

package com.discordsrv.velocity.console;

import com.discordsrv.common.command.game.abstraction.executor.CommandExecutorProvider;
import com.discordsrv.common.core.logging.backend.LoggingBackend;
import com.discordsrv.common.core.logging.backend.impl.Log4JLoggerImpl;
import com.discordsrv.common.feature.console.Console;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.discordsrv.velocity.command.game.sender.VelocityCommandSender;
import com.discordsrv.velocity.console.executor.VelocityCommandExecutorProvider;

public class VelocityConsole extends VelocityCommandSender implements Console {

    private final LoggingBackend loggingBackend;
    private final VelocityCommandExecutorProvider executorProvider;

    public VelocityConsole(VelocityDiscordSRV discordSRV) {
        super(discordSRV, discordSRV.proxy().getConsoleCommandSource());
        this.loggingBackend = Log4JLoggerImpl.getRoot();
        this.executorProvider = new VelocityCommandExecutorProvider(discordSRV);
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
