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
import com.discordsrv.common.command.game.handler.util.BrigadierUtil;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * No avoiding basic handler on bukkit. Unfortunately it isn't possible to use brigadier for executing.
 */
public class CommodoreHandler extends AbstractBukkitCommandExecutor {

    private final Commodore commodore;
    private final Function<?, ICommandSender> senderFunction;

    public CommodoreHandler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
        this.commodore = CommodoreProvider.getCommodore(discordSRV.plugin());
        this.senderFunction = wrapper -> sender((CommandSender) wrapper); // This is probably wrong...
    }

    @Override
    public void registerCommand(GameCommand command) {
        logger.debug("Registering command " + command.getLabel() + " with commodore");

        PluginCommand pluginCommand = command(command);
        if (pluginCommand == null) {
            logger.error("Failed to create command " + command.getLabel());
            return;
        }

        handler.registerCommand(command);
        pluginCommand.setExecutor(this);

        List<LiteralCommandNode<?>> nodes = getAliases(command, pluginCommand);
        discordSRV.scheduler().runOnMainThread(() -> {
            for (LiteralCommandNode<?> node : nodes) {
                commodore.register(node);
            }
        });
    }

    private List<LiteralCommandNode<?>> getAliases(GameCommand command, PluginCommand pluginCommand) {
        String commandName = pluginCommand.getName();
        String pluginName = pluginCommand.getPlugin().getName().toLowerCase(Locale.ROOT);

        List<String> allAliases = new ArrayList<>();
        allAliases.add(commandName);
        allAliases.addAll(pluginCommand.getAliases());

        List<LiteralCommandNode<?>> nodes = new ArrayList<>();
        for (String alias : allAliases) {
            if (alias.equals(commandName)) {
                LiteralCommandNode<?> node = BrigadierUtil.convertToBrigadier(command, senderFunction);
                if (node.getRedirect() != null) {
                    throw new IllegalStateException("Cannot register a redirected node!");
                }
                nodes.add(node);
            } else {
                nodes.add(BrigadierUtil.convertToBrigadier(GameCommand.literal(alias).redirect(command), senderFunction));
            }

            // plugin:command
            nodes.add(BrigadierUtil.convertToBrigadier(GameCommand.literal(pluginName + ":" + alias).redirect(command), senderFunction));
        }
        return nodes;
    }
}
