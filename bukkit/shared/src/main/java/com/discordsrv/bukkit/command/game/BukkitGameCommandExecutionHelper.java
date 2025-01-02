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

package com.discordsrv.bukkit.command.game;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutionHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class BukkitGameCommandExecutionHelper implements GameCommandExecutionHelper {

    protected final BukkitDiscordSRV discordSRV;

    public BukkitGameCommandExecutionHelper(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public CompletableFuture<List<String>> getRootCommands(CommandSender commandSender) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<String>> suggestCommands(List<String> parts) {
        CommandSender commandSender = discordSRV.server().getConsoleSender();

        String commandName = !parts.isEmpty() ? parts.remove(0) : null;
        Command command = commandName != null ? discordSRV.server().getPluginCommand(commandName) : null;
        if (command == null) {
            if (parts.size() > 1) {
                // Command is not known but there are arguments, nothing to auto complete...
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return getRootCommands(commandSender).thenApply(commands -> {
                    List<String> suggestions = new ArrayList<>(commands.size());
                    for (String cmd : commands) {
                        if (commandName == null || cmd.startsWith(commandName)) {
                            suggestions.add(cmd);
                        }
                    }
                    return suggestions;
                });
            }
        }

        // Get the arguments minus the last one (if any)
        String prefix = parts.isEmpty() ? "" : String.join(" ", parts.subList(0, parts.size() - 1)) + " ";
        return discordSRV.scheduler().supplyOnMainThread(commandSender, () -> {
            List<String> completions = command.tabComplete(commandSender, commandName, parts.toArray(new String[0]));

            List<String> suggestions = new ArrayList<>();
            for (String suggestion : completions) {
                suggestions.add(commandName + " " + prefix + suggestion);
            }
            return suggestions;
        });
    }

    @Override
    public List<String> getAliases(String command) {
        PluginCommand pluginCommand = discordSRV.server().getPluginCommand(command);
        if (pluginCommand == null) {
            return Collections.emptyList();
        }

        List<String> aliases = new ArrayList<>(pluginCommand.getAliases());
        aliases.add(pluginCommand.getName());

        String pluginName = pluginCommand.getName().toLowerCase(Locale.ROOT);
        int originalMax = aliases.size();
        for (int i = 0; i < originalMax; i++) {
            // plugin:command
            aliases.add(pluginName + ":" + aliases.get(i));
        }
        return aliases;
    }

    @Override
    public boolean isSameCommand(String command1, String command2) {
        PluginCommand pluginCommand1 = discordSRV.server().getPluginCommand(command1);
        PluginCommand pluginCommand2 = discordSRV.server().getPluginCommand(command2);

        return pluginCommand1 == pluginCommand2;
    }
}
