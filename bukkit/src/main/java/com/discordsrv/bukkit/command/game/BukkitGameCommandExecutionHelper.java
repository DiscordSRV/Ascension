package com.discordsrv.bukkit.command.game;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.command.PaperCommandMap;
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
                    return discordSRV.scheduler().supplyOnMainThread(discordSRV.server().getConsoleSender(), () -> {
                        for (String cmd : PaperCommandMap.getKnownCommands(discordSRV.server())) {
                            if (commandName == null || cmd.startsWith(commandName)) {
                                suggestions.add(cmd);
                            }
                        }
                        return suggestions;
                    });
                }

                return CompletableFuture.completedFuture(suggestions);
            }
        }

        // Get the arguments minus the last one (if any)
        String prefix = parts.isEmpty() ? "" : String.join(" ", parts.subList(0, parts.size() - 1)) + " ";

        CommandSender commandSender = discordSRV.server().getConsoleSender();
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
