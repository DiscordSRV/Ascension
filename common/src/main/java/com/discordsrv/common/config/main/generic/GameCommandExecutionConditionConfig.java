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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutionHelper;
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
            "If the command starts and ends with /, the input will be treated as a regular expression (regex) and it will pass if it matches the entire command")
    public List<String> commands = new ArrayList<>();

    /**
     * Security check for matching console commands.
     *
     * @param configCommand the allowed pattern
     * @param command the input to be checked
     * @param suggestions Enables suggesting the commands leading up to allowed commands for example "discord" if "discord link" were an allowed command
     * @param helper command helper for root command aliases handling
     * @return {@code true} if the input command is accepted with the given input parameters
     */
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

    public boolean isAcceptableCommand(
            DiscordGuildMember member,
            DiscordUser user,
            String command,
            boolean suggestions,
            GameCommandExecutionHelper helper
    ) {
        long userId = user.getId();
        List<Long> roleIds = new ArrayList<>();
        if (member != null) {
            for (DiscordRole role : member.getRoles()) {
                roleIds.add(role.getId());
            }
        }

        return isAcceptableCommand(roleIds, userId, command, suggestions, helper);
    }

    public boolean isAcceptableCommand(
            List<Long> roleIds,
            long userId,
            String command,
            boolean suggestions,
            GameCommandExecutionHelper helper
    ) {
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
            boolean anyMatch = isCommandMatch(configCommand, command, suggestions, helper);
            if (anyMatch) {
                return !blacklist;
            }
        }

        // none match
        return blacklist;
    }
}
