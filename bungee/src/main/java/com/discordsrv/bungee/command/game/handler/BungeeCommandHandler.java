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

package com.discordsrv.bungee.command.game.handler;

import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.bungee.command.game.sender.BungeeCommandSender;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.handler.BasicCommandHandler;
import com.discordsrv.common.command.game.sender.ICommandSender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;

/**
 * BungeeCord has not concept of Brigadier.
 */
public class BungeeCommandHandler extends BasicCommandHandler {

    private final BungeeDiscordSRV discordSRV;

    public BungeeCommandHandler(BungeeDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void registerCommand(GameCommand command) {
        super.registerCommand(command);

        discordSRV.proxy().getPluginManager().registerCommand(discordSRV.plugin(), new BungeeCommand(command));
    }

    public ICommandSender getSender(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return discordSRV.playerProvider().player((ProxiedPlayer) sender);
        } else if (sender == discordSRV.proxy().getConsole()) {
            return discordSRV.console();
        } else {
            return new BungeeCommandSender(discordSRV, sender, () -> discordSRV.audiences().sender(sender));
        }
    }

    public class BungeeCommand extends Command implements TabExecutor {

        private final GameCommand command;

        public BungeeCommand(GameCommand command) {
            super(command.getLabel(), command.getRequiredPermission());
            this.command = command;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            BungeeCommandHandler.this.execute(getSender(sender), command.getLabel(), Arrays.asList(args));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return BungeeCommandHandler.this.suggest(getSender(sender), command.getLabel(), Arrays.asList(args));
        }
    }
}
