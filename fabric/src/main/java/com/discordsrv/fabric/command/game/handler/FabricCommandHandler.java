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

package com.discordsrv.fabric.command.game.handler;

import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.command.game.abstraction.handler.util.BrigadierUtil;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

public class FabricCommandHandler implements ICommandHandler {

    private final FabricDiscordSRV discordSRV;

    public FabricCommandHandler(FabricDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    private ICommandSender getSender(CommandSource source) {
        if (source instanceof ServerCommandSource) {
            if (((ServerCommandSource) source).getPlayer() != null) {
                return discordSRV.playerProvider().player(((ServerCommandSource) source).getPlayer());
            } else {
                return discordSRV.console();
            }
        }
        return null;
    }

    @Override
    public void registerCommand(GameCommand command) {
        LiteralCommandNode<ServerCommandSource> node = BrigadierUtil.convertToBrigadier(command, this::getSender);
        discordSRV.getServer().getCommandManager().getDispatcher().getRoot().addChild(node);
    }
}
