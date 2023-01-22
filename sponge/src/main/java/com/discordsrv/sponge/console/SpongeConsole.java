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

package com.discordsrv.sponge.console;

import com.discordsrv.common.command.game.executor.CommandExecutorProvider;
import com.discordsrv.common.console.Console;
import com.discordsrv.common.logging.backend.LoggingBackend;
import com.discordsrv.common.logging.backend.impl.Log4JLoggerImpl;
import com.discordsrv.sponge.SpongeDiscordSRV;
import com.discordsrv.sponge.command.game.sender.SpongeCommandSender;
import com.discordsrv.sponge.console.executor.SpongeCommandExecutorProvider;

public class SpongeConsole extends SpongeCommandSender implements Console {

    private final LoggingBackend loggingBackend;
    private final SpongeCommandExecutorProvider executorProvider;

    public SpongeConsole(SpongeDiscordSRV discordSRV) {
        super(discordSRV, () -> discordSRV.game().systemSubject(), () -> discordSRV.game().systemSubject());
        this.loggingBackend = Log4JLoggerImpl.getRoot();
        this.executorProvider = new SpongeCommandExecutorProvider(discordSRV);
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
