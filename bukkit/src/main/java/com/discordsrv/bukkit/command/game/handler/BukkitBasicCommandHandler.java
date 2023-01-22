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

package com.discordsrv.bukkit.command.game.handler;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class BukkitBasicCommandHandler extends AbstractBukkitCommandExecutor implements TabCompleter {

    public BukkitBasicCommandHandler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        return handler.suggest(sender(sender), alias, Arrays.asList(args));
    }

    @Override
    public void registerCommand(GameCommand command) {
        PluginCommand pluginCommand = command(command);
        if (pluginCommand == null) {
            logger.error("Failed to create command " + command.getLabel());
            return;
        }

        logger.debug("Registering command " + command.getLabel() + " with basic handler");

        handler.registerCommand(command);
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }
}
