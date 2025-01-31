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

package com.discordsrv.fabric.command.game;

import com.discordsrv.common.command.game.abstraction.GameCommandExecutionHelper;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FabricGameCommandExecutionHelper implements GameCommandExecutionHelper {
    protected final FabricDiscordSRV discordSRV;
    private final CommandDispatcher<ServerCommandSource> dispatcher;

    public FabricGameCommandExecutionHelper(FabricDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.dispatcher = discordSRV.getServer().getCommandManager().getDispatcher();
    }

    @Override
    public CompletableFuture<List<String>> suggestCommands(List<String> parts) {
        String fullCommand = String.join(" ", parts);
        if (parts.isEmpty() || fullCommand.isBlank()) {
            return getRootCommands();
        }
        try {
            ParseResults<ServerCommandSource> parse = dispatcher.parse(fullCommand, discordSRV.getServer().getCommandSource());
            if (!parse.getExceptions().isEmpty()) {
                // There's an error with the command syntax, return the full command and the error message for the user.
                List<String> data = new ArrayList<>();
                data.add(fullCommand);
                parse.getExceptions().values().stream().map(Exception::getMessage).map(this::splitErrorMessage).forEach(data::addAll);

                return CompletableFuture.completedFuture(data);
            }

            if (!parse.getContext().getNodes().isEmpty()) {
                CommandNode<ServerCommandSource> lastNode = parse.getContext().getNodes().getLast().getNode();
                if (lastNode.getChildren().isEmpty() && lastNode.getRedirect() == null) {
                    // We reached the end of the command tree. Suggest the full command as a valid command.
                    return CompletableFuture.completedFuture(Collections.singletonList(fullCommand));
                }
            }

            Suggestions suggestions = dispatcher.getCompletionSuggestions(parse).get();
            List<String> data = suggestions.getList().stream()
                    .map(suggestion -> fullCommand.substring(0, suggestion.getRange().getStart()) + suggestion.getText())
                    .collect(Collectors.toList());
            if (data.isEmpty()) {
                // Suggestions are empty, Likely the user is still typing an argument.
                // If the context is empty, We search all commands from the root.
                CommandNode<ServerCommandSource> lastNode = !parse.getContext().getNodes().isEmpty() ? parse.getContext().getNodes().getLast().getNode() : parse.getContext().getRootNode();

                for (CommandNode<ServerCommandSource> child : lastNode.getChildren()) {
                    if (child.getName().toLowerCase().startsWith(parts.getLast().toLowerCase())) {
                        if (lastNode instanceof RootCommandNode<ServerCommandSource>) {
                            data.add(child.getName());
                            continue;
                        }

                        String commandWithoutLastPart = fullCommand.substring(0, fullCommand.length() - parts.getLast().length());
                        data.add(commandWithoutLastPart + child.getName());
                    }
                }
            }
            data = data.stream().map(String::trim).distinct().collect(Collectors.toList());
            return CompletableFuture.completedFuture(data);
        } catch (InterruptedException | ExecutionException e) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public List<String> getAliases(String command) {
        return Collections.emptyList();
    }

    @Override
    public boolean isSameCommand(String command1, String command2) {
        CommandNode<ServerCommandSource> commandNode1 = dispatcher.findNode(Collections.singleton(command1));
        CommandNode<ServerCommandSource> commandNode2 = dispatcher.findNode(Collections.singleton(command2));
        if (commandNode1 != null && commandNode2 != null) {
            return commandNode1.equals(commandNode2);
        }
        return false;
    }

    private CompletableFuture<List<String>> getRootCommands() {
        return CompletableFuture.completedFuture(dispatcher.getRoot().getChildren().stream().map(CommandNode::getName).collect(Collectors.toList()));
    }

    // Split the error message if it's too long on a period or a comma. If the message reached 97 characters, split at that point and continue.
    private List<String> splitErrorMessage(String message) {
        List<String> parts = new ArrayList<>();
        int start = 0;

        while (start < message.length()) {
            // Maximum line length (100 - 7 for "Error: " = 93)
            int end = Math.min(start + 93, message.length());
            String chunk = message.substring(start, end);

            int splitIndex = Math.max(chunk.lastIndexOf('.'), chunk.lastIndexOf(','));
            if (splitIndex != -1 && start + splitIndex < end) {
                parts.add("Error: " + message.substring(start, start + splitIndex + 1));
                start += splitIndex + 1;
            } else {
                // Split at 90 characters (leaving room for "Error: " and "...")
                if (end < message.length()) {
                    parts.add("Error: " + message.substring(start, start + 90) + "...");
                    start += 90;
                } else {
                    parts.add("Error: " + message.substring(start));
                    break;
                }
            }
        }

        return parts;
    }

}
