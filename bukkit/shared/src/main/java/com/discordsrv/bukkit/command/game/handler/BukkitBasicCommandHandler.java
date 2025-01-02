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

package com.discordsrv.bukkit.command.game.handler;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.command.game.sender.BukkitCommandSender;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.handler.BasicCommandHandler;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class BukkitBasicCommandHandler extends BasicCommandHandler implements TabExecutor, ICommandHandler {

    protected final BukkitDiscordSRV discordSRV;
    protected final Logger logger;

    public BukkitBasicCommandHandler(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "COMMAND_HANDLER");
    }

    protected ICommandSender sender(CommandSender commandSender) {
        if (commandSender instanceof Player) {
            return discordSRV.playerProvider().player((Player) commandSender);
        } else if (commandSender instanceof ConsoleCommandSender) {
            return discordSRV.console();
        } else {
            return new BukkitCommandSender(discordSRV, commandSender, () -> discordSRV.audiences().sender(commandSender));
        }
    }

    protected PluginCommand command(GameCommand gameCommand) {
        String label = gameCommand.getLabel();
        return discordSRV.plugin().getCommand(label);
    }

    protected void registerPluginCommand(PluginCommand pluginCommand, GameCommand gameCommand) {
        logger.debug("Registering command " + gameCommand.getLabel() + " with basic handler");

        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    public void registerCommand(GameCommand command) {
        super.registerCommand(command);
        discordSRV.scheduler().runOnMainThread(() -> {
            PluginCommand pluginCommand = command(command);
            if (pluginCommand == null) {
                logger.error("Failed to create command " + command.getLabel());
                return;
            }

            registerPluginCommand(pluginCommand, command);
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        execute(sender(sender), label, Arrays.asList(args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return suggest(sender(sender), alias, Arrays.asList(args));
    }
}
