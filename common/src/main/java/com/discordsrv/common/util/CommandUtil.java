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
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.permission.game.Permissions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class CommandUtil {

    private CommandUtil() {}

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
            @Nullable Permission otherPermission
    ) {
        return lookupTarget(discordSRV, logger, execution, target, selfPermitted, true, false, otherPermission)
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
            @Nullable Permission otherPermission
    ) {
        return lookupTarget(discordSRV, logger, execution, target, selfPermitted, false, true, otherPermission)
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
            @Nullable Permission otherPermission
    ) {
        String target = execution.getArgument("target");
        if (target == null) {
            target = execution.getArgument("user");
        }
        if (target == null) {
            target = execution.getArgument("player");
        }
        return lookupTarget(discordSRV, logger, execution, target, selfPermitted, true, true, otherPermission);
    }

    private static Task<TargetLookupResult> lookupTarget(
            DiscordSRV discordSRV,
            Logger logger,
            CommandExecution execution,
            String target,
            boolean selfPermitted,
            boolean lookupPlayer,
            boolean lookupUser,
            @Nullable Permission otherPermission
    ) {
        MessagesConfig messages = discordSRV.messagesConfig(execution.locale());

        if (execution instanceof GameCommandExecution) {
            ICommandSender sender = ((GameCommandExecution) execution).getSender();
            if (target != null) {
                if (otherPermission != null && !sender.hasPermission(otherPermission)) {
                    sender.sendMessage(discordSRV.messagesConfig(sender).noPermission.asComponent());
                    return Task.completed(TargetLookupResult.INVALID);
                }
            } else if (sender instanceof IPlayer && selfPermitted && lookupPlayer) {
                target = ((IPlayer) sender).uniqueId().toString();
            }
        } else if (execution instanceof DiscordCommandExecution) {
            if (target == null) {
                if (selfPermitted && lookupUser) {
                    target = Long.toUnsignedString(((DiscordCommandExecution) execution).getUser().getId());
                } else {
                    execution.send(
                            messages.minecraft.pleaseSpecifyUser.asComponent(),
                            messages.discord.pleaseSpecifyUser.get()
                    );
                    return Task.completed(TargetLookupResult.INVALID);
                }
            }
        } else {
            throw new IllegalStateException("Unexpected CommandExecution");
        }

        if (target == null) {
            return Task.completed(requireTarget(execution, lookupUser, lookupPlayer, messages));
        }

        if (lookupUser) {
            if (target.matches("\\d{17,22}")) {
                // Discord user id
                long id;
                try {
                    id = MiscUtil.parseLong(target);
                } catch (IllegalArgumentException ignored) {
                    execution.send(
                            messages.minecraft.userNotFound.asComponent(),
                            messages.discord.userNotFound.get()
                    );
                    return Task.completed(TargetLookupResult.INVALID);
                }

                return Task.completed(new TargetLookupResult(true, null, id));
            } else if (target.startsWith("@")) {
                // Discord username
                String username = target.substring(1);
                JDA jda = discordSRV.jda();
                if (jda != null) {
                    List<User> users = jda.getUsersByName(username, true);

                    if (users.size() == 1) {
                        return Task.completed(new TargetLookupResult(true, null, users.get(0).getIdLong()));
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
                    execution.send(
                            messages.minecraft.playerNotFound.asComponent(),
                            messages.discord.playerNotFound.get()
                    );
                    return Task.completed(TargetLookupResult.INVALID);
                }
                return Task.completed(new TargetLookupResult(true, uuid, 0L));
            } else if (target.matches("[a-zA-Z0-9_]{1,16}")) {
                // Player name
                IPlayer playerByName = discordSRV.playerProvider().player(target);
                if (playerByName != null) {
                    uuid = playerByName.uniqueId();
                } else {
                    return discordSRV.playerProvider().lookupOfflinePlayer(target)
                            .thenApply(offlinePlayer -> new TargetLookupResult(true, offlinePlayer.uniqueId(), 0L))
                            .mapException(t -> {
                                logger.error("Failed to lookup offline player by username", t);
                                return TargetLookupResult.INVALID;
                            });
                }
                return Task.completed(new TargetLookupResult(true, uuid, 0L));
            }
        }

        return Task.completed(requireTarget(execution, lookupUser, lookupPlayer, messages));
    }

    private static TargetLookupResult requireTarget(CommandExecution execution, boolean lookupUser, boolean lookupPlayer, MessagesConfig messages) {
        if (lookupPlayer && lookupUser) {
            execution.send(
                    messages.minecraft.pleaseSpecifyPlayerOrUser.asComponent(),
                    messages.discord.pleaseSpecifyPlayerOrUser.get()
            );
            return TargetLookupResult.INVALID;
        } else if (lookupPlayer) {
            execution.send(
                    messages.minecraft.pleaseSpecifyPlayer.asComponent(),
                    messages.discord.pleaseSpecifyPlayer.get()
            );
            return TargetLookupResult.INVALID;
        } else if (lookupUser) {
            execution.send(
                    messages.minecraft.pleaseSpecifyUser.asComponent(),
                    messages.discord.pleaseSpecifyUser.get()
            );
            return TargetLookupResult.INVALID;
        } else {
            throw new IllegalStateException("lookupPlayer & lookupUser are false");
        }
    }

    public static class TargetLookupResult {

        public static TargetLookupResult INVALID = new TargetLookupResult(false, null, 0L);

        private final boolean valid;
        private final UUID playerUUID;
        private final long userId;

        public TargetLookupResult(boolean valid, UUID playerUUID, long userId) {
            this.valid = valid;
            this.playerUUID = playerUUID;
            this.userId = userId;
        }

        public boolean isValid() {
            return valid;
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
