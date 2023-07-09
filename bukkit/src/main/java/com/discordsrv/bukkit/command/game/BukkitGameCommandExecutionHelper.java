package com.discordsrv.bukkit.command.game;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.PaperCommandMap;
import com.discordsrv.common.command.game.GameCommandExecutionHelper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class BukkitGameCommandExecutionHelper implements GameCommandExecutionHelper {

    private final BukkitDiscordSRV discordSRV;

    public BukkitGameCommandExecutionHelper(BukkitDiscordSRV discordSRV) {
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
                    discordSRV.scheduler().runOnMainThread(discordSRV.server().getConsoleSender(), () -> {
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

        CommandSender commandSender = discordSRV.server().getConsoleSender();
        discordSRV.scheduler().runOnMainThread(commandSender, () -> {
            try {
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
