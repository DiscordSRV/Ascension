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

package com.discordsrv.common.util;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.DiscordCommandExecution;
import com.discordsrv.common.command.combined.abstraction.GameCommandExecution;
import com.discordsrv.common.command.game.abstraction.command.GameCommandSuggester;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.profile.ProfileImpl;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.permission.game.Permissions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class CommandUtil {

    private static final String NONE = "--null";

    private CommandUtil() {}

    public static GameCommandSuggester targetSuggestions(DiscordSRV discordSRV, boolean users, boolean players, boolean linked) {
        return CommandUtil.targetSuggestions(discordSRV,  users ? user -> {
            ProfileImpl profile = discordSRV.profileManager().getProfile(user.getIdLong());
            return profile == null || profile.isLinked() == linked;
        } : null, players ? player -> {
            ProfileImpl profile = discordSRV.profileManager().getProfile(player.uniqueId());
            return profile == null || profile.isLinked() == linked;
        } : null, false);
    }

    public static GameCommandSuggester targetSuggestions(
            DiscordSRV discordSRV,
            @Nullable Predicate<User> userPredicate,
            @Nullable Predicate<IPlayer> playerPredicate,
            boolean includeNoneSuggestion) {
        return (sender, previousArguments, input) -> {
            List<String> suggestions = new ArrayList<>();
            if (includeNoneSuggestion) {
                suggestions.add(NONE);
            }
            input = input.toLowerCase(Locale.ROOT);

            if (userPredicate != null) {
                JDA jda = discordSRV.jda();
                if (jda != null && (input.startsWith("@") || playerPredicate == null)) {
                    String inputCheck = input.isEmpty() ? input : input.substring(1);
                    List<String> usernames = jda.getUserCache().stream()
                            .filter(userPredicate)
                            .filter(user -> !user.isBot())
                            .map(User::getName)
                            .filter(username -> username.toLowerCase(Locale.ROOT).startsWith(inputCheck))
                            .limit(100)
                            .map(username -> "@" + username)
                            .collect(Collectors.toList());

                    suggestions.addAll(usernames);
                }
            }
            if (playerPredicate != null && !input.startsWith("@")) {
                for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
                    if (!playerPredicate.test(player)) {
                        continue;
                    }

                    String playerName = player.username();
                    if (playerName.toLowerCase(Locale.ROOT).startsWith(input)) {
                        suggestions.add(playerName);
                    }
                }
            }
            return suggestions;
        };
    }

    public static void basicStatusCheck(DiscordSRV discordSRV, ICommandSender sender) {
        if (discordSRV.status().isError() && sender.hasPermission(Permissions.COMMAND_DEBUG)) {
            if (discordSRV.status() == DiscordSRV.Status.NOT_CONFIGURED) {
                sender.sendMessage(
                        Component.text("DiscordSRV has not been fully configured yet, please check your server log for more details")
                                .color(NamedTextColor.RED)
                );
            } else {
                sender.sendMessage(
                        Component.text("DiscordSRV did not start correctly, please check your server log for more details")
                                .color(NamedTextColor.RED)
                );
            }
        }
    }

    public static Task<UUID> lookupPlayer(
            DiscordSRV discordSRV,
            Logger logger,
            CommandExecution execution,
            boolean selfPermitted,
            String target,
            @Nullable Permission otherPermission,
            boolean optional
    ) {
        return lookupTarget(discordSRV, logger, execution, target, selfPermitted, true, false, otherPermission, optional)
                .thenApply((result) -> {
                    if (result != null && result.isValid()) {
                        return result.getPlayerUUID();
                    }
                    return null;
                });
    }

    public static Task<Long> lookupUser(
            DiscordSRV discordSRV,
            Logger logger,
            CommandExecution execution,
            boolean selfPermitted,
            String target,
            @Nullable Permission otherPermission,
            boolean optional
    ) {
        return lookupTarget(discordSRV, logger, execution, target, selfPermitted, false, true, otherPermission, optional)
                .thenApply(result -> {
                    if (result != null && result.isValid()) {
                        return result.getUserId();
                    }
                    return null;
                });
    }

    public static Task<TargetLookupResult> lookupTarget(
            DiscordSRV discordSRV,
            Logger logger,
            CommandExecution execution,
            boolean selfPermitted,
            @Nullable Permission otherPermission,
            boolean optional
    ) {
        String target = execution.getString("target");
        if (target == null) {
            target = execution.getString("user");
        }
        if (target == null) {
            target = execution.getString("player");
        }
        if (target != null && target.equals(NONE)) {
            target = null;
        }
        return lookupTarget(discordSRV, logger, execution, target, selfPermitted, true, true, otherPermission, optional);
    }

    private static Task<TargetLookupResult> lookupTarget(
            DiscordSRV discordSRV,
            Logger logger,
            CommandExecution execution,
            String target,
            boolean selfPermitted,
            boolean lookupPlayer,
            boolean lookupUser,
            @Nullable Permission otherPermission,
            boolean optional
    ) {
        MessagesConfig messages = discordSRV.messagesConfig(execution.locale());

        boolean self = false;
        if (execution instanceof GameCommandExecution) {
            ICommandSender sender = ((GameCommandExecution) execution).getSender();
            if (target != null) {
                if (otherPermission != null && !sender.hasPermission(otherPermission)) {
                    execution.messages().noPermission.sendTo(execution);
                    return Task.completed(TargetLookupResult.INVALID);
                }
            } else if (sender instanceof IPlayer && selfPermitted && lookupPlayer) {
                target = ((IPlayer) sender).uniqueId().toString();
                self = true;
            }
        } else if (execution instanceof DiscordCommandExecution) {
            if (target == null) {
                if (selfPermitted && lookupUser) {
                    target = Long.toUnsignedString(((DiscordCommandExecution) execution).getUser().getId());
                    self = true;
                } else {
                    if (!optional) {
                        messages.pleaseSpecifyUser.sendTo(execution);
                    }
                    return Task.completed(TargetLookupResult.INVALID);
                }
            }
        } else {
            throw new IllegalStateException("Unexpected CommandExecution");
        }

        if (target == null) {
            return Task.completed(requireTarget(execution, lookupUser, lookupPlayer, messages, optional));
        }

        boolean isSelf = self;
        if (lookupUser) {
            if (target.matches("\\d{17,22}")) {
                // Discord user id
                long id;
                try {
                    id = MiscUtil.parseLong(target);
                } catch (IllegalArgumentException ignored) {
                    messages.userNotFound.sendTo(execution);
                    return Task.completed(TargetLookupResult.INVALID);
                }

                return Task.completed(new TargetLookupResult(isSelf, null, id));
            } else if (target.startsWith("@")) {
                // Discord username
                String username = target.substring(1);
                JDA jda = discordSRV.jda();
                if (jda != null) {
                    List<User> users = jda.getUsersByName(username, true);

                    User matchingUser = null;
                    if (users.size() == 1) {
                        matchingUser = users.get(0);
                    } else {
                        for (User user : users) {
                            if (target.equalsIgnoreCase("@" + user.getAsTag())) {
                                matchingUser = user;
                                break;
                            }
                        }
                    }
                    if (matchingUser != null) {
                        return Task.completed(new TargetLookupResult(isSelf, null, users.get(0).getIdLong()));
                    }
                }
            }
        }

        if (lookupPlayer) {
            UUID uuid;
            boolean shortUUID;
            if ((shortUUID = target.length() == 32) || target.length() == 36) {
                // Player UUID
                try {
                    if (shortUUID) {
                        uuid = UUIDUtil.fromShort(target);
                    } else {
                        uuid = UUID.fromString(target);
                    }
                } catch (IllegalArgumentException ignored) {
                    messages.playerNotFound.sendTo(execution);
                    return Task.completed(TargetLookupResult.INVALID);
                }
                return Task.completed(new TargetLookupResult(isSelf, uuid, 0L));
            } else if (target.matches("[a-zA-Z0-9_]{1,16}")) {
                // Player name
                IPlayer playerByName = discordSRV.playerProvider().player(target);
                if (playerByName != null) {
                    uuid = playerByName.uniqueId();
                } else {
                    return discordSRV.playerProvider().lookupOfflinePlayer(target)
                            .thenApply(offlinePlayer -> new TargetLookupResult(isSelf, offlinePlayer.uniqueId(), 0L))
                            .mapException(t -> {
                                logger.error("Failed to lookup offline player by username", t);
                                messages.playerLookupFailed.sendTo(execution);
                                return TargetLookupResult.INVALID;
                            });
                }
                return Task.completed(new TargetLookupResult(isSelf, uuid, 0L));
            }
        }

        return Task.completed(requireTarget(execution, lookupUser, lookupPlayer, messages, optional));
    }

    private static TargetLookupResult requireTarget(CommandExecution execution, boolean lookupUser, boolean lookupPlayer, MessagesConfig messages, boolean optional) {
        if (optional) {
            return TargetLookupResult.INVALID;
        }

        if (lookupPlayer && lookupUser) {
            messages.pleaseSpecifyPlayerOrUser.sendTo(execution);
        } else if (lookupPlayer) {
            messages.pleaseSpecifyPlayer.sendTo(execution);
        } else if (lookupUser) {
            messages.pleaseSpecifyUser.sendTo(execution);
        } else {
            throw new IllegalStateException("lookupPlayer & lookupUser are false");
        }
        return TargetLookupResult.INVALID;
    }

    public static class TargetLookupResult {

        public static TargetLookupResult INVALID = new TargetLookupResult(false);

        private final boolean valid;
        private final boolean self;
        private final UUID playerUUID;
        private final long userId;

        private TargetLookupResult(boolean valid) {
            this(valid, false, null, 0L);
        }

        public TargetLookupResult(boolean self, UUID playerUUID, long userId) {
            this(true, self, playerUUID, userId);
        }

        private TargetLookupResult(boolean valid, boolean self, UUID playerUUID, long userId) {
            this.valid = valid;
            this.self = self;
            this.playerUUID = playerUUID;
            this.userId = userId;
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isSelf() {
            return self;
        }

        public boolean isPlayer() {
            return playerUUID != null;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

        public long getUserId() {
            return userId;
        }
    }
}
