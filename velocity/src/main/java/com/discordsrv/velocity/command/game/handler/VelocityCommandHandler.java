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

package com.discordsrv.velocity.command.game.handler;

import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.command.game.handler.util.BrigadierUtil;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.discordsrv.velocity.command.game.sender.VelocityCommandSender;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;

public class VelocityCommandHandler implements ICommandHandler {

    private final VelocityDiscordSRV discordSRV;

    public VelocityCommandHandler(VelocityDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    private ICommandSender getSender(CommandSource source) {
        if (source instanceof Player) {
            return discordSRV.playerProvider().player((Player) source);
        } else if (source instanceof ConsoleCommandSource) {
            return discordSRV.console();
        } else {
            return new VelocityCommandSender(discordSRV, source);
        }
    }

    @Override
    public void registerCommand(GameCommand command) {
        LiteralCommandNode<CommandSource> node = BrigadierUtil.convertToBrigadier(command, this::getSender);
        discordSRV.proxy().getCommandManager().register(new BrigadierCommand(node));
    }
}
