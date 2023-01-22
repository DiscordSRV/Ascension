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

package com.discordsrv.bukkit.console.executor;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.command.game.executor.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandSenderExecutor implements CommandExecutor {

    private final BukkitDiscordSRV discordSRV;
    private final CommandSender commandSender;

    public CommandSenderExecutor(BukkitDiscordSRV discordSRV, CommandSender commandSender) {
        this.discordSRV = discordSRV;
        this.commandSender = commandSender;
    }

    @Override
    public void runCommand(String command) {
        discordSRV.scheduler().runOnMainThread(() -> discordSRV.server().dispatchCommand(commandSender, command));
    }
}
