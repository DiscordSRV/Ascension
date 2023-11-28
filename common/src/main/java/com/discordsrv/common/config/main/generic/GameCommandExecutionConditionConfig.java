package com.discordsrv.common.config.main.generic;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.common.command.game.GameCommandExecutionHelper;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ConfigSerializable
public class GameCommandExecutionConditionConfig {

    @SuppressWarnings("unused") // Configurate
    public GameCommandExecutionConditionConfig() {}

    public GameCommandExecutionConditionConfig(List<Long> roleAndUserIds, boolean blacklist, List<String> commands) {
        this.roleAndUserIds = roleAndUserIds;
        this.blacklist = blacklist;
        this.commands = commands;
    }

    @Comment("The role and user ids that should be allowed to run the commands specified in this condition")
    public List<Long> roleAndUserIds = new ArrayList<>();

    @Comment("true for blacklist (blocking commands), false for whitelist (allowing commands)")
    public boolean blacklist = true;

    @Comment("The commands and/or patterns that are allowed/blocked.\n" +
            "The command needs to start with input, this will attempt to normalize command aliases where possible (for the main command)\n" +
            "If the command start and ends with /, the input will be treated as a regular expression (regex) and it will pass if it matches the entire command")
    public List<String> commands = new ArrayList<>();

    public static boolean isCommandMatch(String configCommand, String command, boolean suggestions, GameCommandExecutionHelper helper) {
        if (configCommand.startsWith("/") && configCommand.endsWith("/")) {
            // Regex handling
            Pattern pattern = Pattern.compile(configCommand.substring(1, configCommand.length() - 1));

            Matcher matcher = pattern.matcher(command);
            return matcher.matches() && matcher.start() == 0 && matcher.end() == command.length();
        }

        // Normal handling
        configCommand = configCommand.toLowerCase(Locale.ROOT);
        command = command.toLowerCase(Locale.ROOT);

        List<String> parts = new ArrayList<>(Arrays.asList(configCommand.split(" ")));
        String rootCommand = parts.remove(0);

        Set<String> rootCommands = new LinkedHashSet<>();
        rootCommands.add(rootCommand);
        if (helper != null) {
            rootCommands.addAll(helper.getAliases(rootCommand));
        }

        if (suggestions) {
            // Allow suggesting the commands up to the allowed command
            for (String rootCmd : rootCommands) {
                if (command.matches("^" + Pattern.quote(rootCmd) + " ?$")) {
                    return true;
                }

                StringBuilder built = new StringBuilder(rootCmd);
                for (String part : parts) {
                    built.append(" ").append(part);
                    if (command.matches("^" + Pattern.quote(built.toString()) + " ?$")) {
                        return true;
                    }
                }
            }
        }

        String arguments = String.join(" ", parts);
        for (String rootCmd : rootCommands) {
            String joined = rootCmd + (arguments.isEmpty() ? "" : " " + arguments);

            // This part at the end prevents "command list" matching "command listsecrets"
            if (command.matches("^" + Pattern.quote(joined) + "(?:$| .+)")) {
                // Make sure it's the same command, the alias may be used by another command
                return helper == null || helper.isSameCommand(rootCommand, rootCmd);
            }
        }

        return false;
    }

    public boolean isAcceptableCommand(DiscordGuildMember member, DiscordUser user, String command, boolean suggestions, GameCommandExecutionHelper helper) {
        long userId = user.getId();
        List<Long> roleIds = new ArrayList<>();
        if (member != null) {
            for (DiscordRole role : member.getRoles()) {
                roleIds.add(role.getId());
            }
        }

        boolean match = false;
        for (Long id : roleAndUserIds) {
            if (id == userId || roleIds.contains(id)) {
                match = true;
                break;
            }
        }
        if (!match) {
            return false;
        }

        for (String configCommand : commands) {
            if (isCommandMatch(configCommand, command, suggestions, helper) != blacklist) {
                return true;
            }
        }
        return false;
    }
}
