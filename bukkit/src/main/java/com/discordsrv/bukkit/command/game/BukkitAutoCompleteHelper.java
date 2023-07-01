package com.discordsrv.bukkit.command.game;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.PaperCommandMap;
import com.discordsrv.common.command.discord.commands.subcommand.ExecuteCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BukkitAutoCompleteHelper implements ExecuteCommand.AutoCompleteHelper {

    private final BukkitDiscordSRV discordSRV;

    public BukkitAutoCompleteHelper(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public CompletableFuture<List<String>> suggestCommands(List<String> parts) {
        String commandName = !parts.isEmpty() ? parts.remove(0) : null;
        Command command = commandName != null ? discordSRV.server().getPluginCommand(commandName) : null;
        if (command == null) {
            if (parts.size() > 1) {
                // Command is not known but there are arguments, nothing to auto complete...
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                // List out commands
                List<String> suggestions = new ArrayList<>();

                if (PaperCommandMap.IS_AVAILABLE) {
                    // If Paper's CommandMap is available we can list out 'root' commands
                    CompletableFuture<List<String>> future = new CompletableFuture<>();
                    discordSRV.scheduler().runOnMainThread(() -> {
                        try {
                            for (String cmd : PaperCommandMap.getKnownCommands(discordSRV.server())) {
                                if (commandName == null || cmd.startsWith(commandName)) {
                                    suggestions.add(cmd);
                                }
                            }
                            future.complete(suggestions);
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                    return future;
                }

                return CompletableFuture.completedFuture(suggestions);
            }
        }

        // Get the arguments minus the last one (if any)
        String prefix = String.join(" ", parts.subList(0, parts.size() - (!parts.isEmpty() ? 1 : 0)));
        if (!prefix.isEmpty()) {
            prefix = prefix + " ";
        }

        CompletableFuture<List<String>> future = new CompletableFuture<>();
        String finalPrefix = prefix;
        discordSRV.scheduler().runOnMainThread(() -> {
            try {
                CommandSender commandSender = discordSRV.server().getConsoleSender();
                List<String> completions = command.tabComplete(commandSender, commandName, parts.toArray(new String[0]));

                List<String> suggestions = new ArrayList<>();
                for (String suggestion : completions) {
                    suggestions.add(commandName + " " + finalPrefix + suggestion);
                }
                future.complete(suggestions);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}
