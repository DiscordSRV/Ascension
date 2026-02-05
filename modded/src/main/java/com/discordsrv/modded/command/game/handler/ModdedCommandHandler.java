/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.modded.command.game.handler;

import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.command.game.abstraction.handler.util.BrigadierUtil;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public class ModdedCommandHandler implements ICommandHandler {

    private final ModdedDiscordSRV discordSRV;
    private final Set<GameCommand> commands = new HashSet<>();

    public ModdedCommandHandler(ModdedDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;

        //? if fabric
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerAll(dispatcher));
        //? if neoforge
        //net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> registerAll(event.getDispatcher()));
    }

    private ICommandSender getSender(SharedSuggestionProvider source) {
        if (source instanceof CommandSourceStack) {
            Entity playerEntity = ((CommandSourceStack) source).getEntity();
            if (playerEntity instanceof ServerPlayer player) {
                return discordSRV.playerProvider().player(player);
            } else {
                return discordSRV.console();
            }
        }
        return null;
    }

    @Override
    public void registerCommand(GameCommand command) {
        commands.add(command);
        LiteralCommandNode<CommandSourceStack> node = BrigadierUtil.convertToBrigadier(discordSRV, command, this::getSender);
        discordSRV.getServer().getCommands().getDispatcher().getRoot().addChild(node);
    }

    private void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (GameCommand command : commands) {
            LiteralCommandNode<CommandSourceStack> node = BrigadierUtil.convertToBrigadier(discordSRV, command, this::getSender);
            dispatcher.getRoot().addChild(node);
        }
    }
}
