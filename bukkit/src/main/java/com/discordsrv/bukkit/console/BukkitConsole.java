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

package com.discordsrv.bukkit.console;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.console.Console;
import com.discordsrv.common.logging.backend.LoggingBackend;
import com.discordsrv.common.logging.backend.impl.JavaLoggerImpl;
import com.discordsrv.common.logging.backend.impl.Log4JLoggerImpl;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class BukkitConsole implements Console {

    private final BukkitDiscordSRV discordSRV;
    private final LoggingBackend loggingBackend;

    public BukkitConsole(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;

        LoggingBackend logging;
        try {
            // Check that log4j is there
            Class.forName("org.apache.logging.log4j.core.Filter");
            logging = Log4JLoggerImpl.getRoot();
        } catch (ClassNotFoundException ignored) {
            // Log4j with Filter has been in the vanilla server since Minecraft 1.7,
            // this is mostly for Bukkit servers that don't use the vanilla server software
            discordSRV.logger().debug("Not using Log4j for advanced console features");
            logging = JavaLoggerImpl.getRoot();
        }
        this.loggingBackend = logging;
    }

    @Override
    public void sendMessage(Identity identity, @NotNull Component message) {
        discordSRV.audiences().console().sendMessage(identity, message);
    }

    @Override
    public void runCommand(String command) {
        discordSRV.scheduler().runOnMainThread(() ->
                discordSRV.server().dispatchCommand(
                        discordSRV.server().getConsoleSender(), command));
    }

    @Override
    public LoggingBackend loggingBackend() {
        return loggingBackend;
    }
}
