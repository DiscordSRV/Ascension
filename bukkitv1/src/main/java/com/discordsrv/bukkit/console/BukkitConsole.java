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

package com.discordsrv.bukkit.console;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.command.game.sender.BukkitCommandSender;
import com.discordsrv.bukkit.console.executor.BukkitCommandExecutorProvider;
import com.discordsrv.common.command.game.abstraction.executor.CommandExecutorProvider;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.logging.backend.LoggingBackend;
import com.discordsrv.common.core.logging.backend.impl.JavaLoggerImpl;
import com.discordsrv.common.core.logging.backend.impl.Log4JLoggerImpl;
import com.discordsrv.common.feature.console.Console;

public class BukkitConsole extends BukkitCommandSender implements Console {

    private final LoggingBackend loggingBackend;
    private final CommandExecutorProvider executorProvider;

    public BukkitConsole(BukkitDiscordSRV discordSRV) {
        super(discordSRV, discordSRV.server().getConsoleSender(), () -> discordSRV.audiences().console());

        LoggingBackend logging;
        try {
            // Check that log4j is there
            Class.forName("org.apache.logging.log4j.core.Filter");
            logging = Log4JLoggerImpl.getRoot();
        } catch (ClassNotFoundException ignored) {
            // Log4j with Filter has been in the vanilla server since Minecraft 1.7,
            // this is mostly for Bukkit servers that don't use the vanilla server software
            new NamedLogger(discordSRV, "CONSOLE").debug("Not using Log4j for advanced console features");
            logging = JavaLoggerImpl.getRoot();
        }
        this.loggingBackend = logging;
        this.executorProvider = new BukkitCommandExecutorProvider(discordSRV);
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
