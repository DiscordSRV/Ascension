/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.feature.mention;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.permission.game.Permissions;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class MentionUtil {

    private MentionUtil() {}

    public static boolean isAllowedToMention(MinecraftToDiscordChatConfig.Mentions config, IPlayer player, Mention mention) {
        Mention.Type type = mention.type();
        switch (type) {
            case CHANNEL: {
                return true;
            }
            case ROLE: {
                if (player.hasPermission(Permissions.MENTION_ROLE_ALL)) {
                    return true;
                }
                if (mention.mentionable() && player.hasPermission(Permissions.MENTION_ROLE_MENTIONABLE)) {
                    return true;
                }
                return player.hasPermission(mention.permission());
            }
            case USER: {
                return player.hasPermission(Permissions.MENTION_USER_ALL);
            }
            case EVERYONE:
            case HERE:
                return isEveryoneMentionAllowed(config, player);
            default: {
                return false;
            }
        }
    }

    private static boolean isEveryoneMentionAllowed(MinecraftToDiscordChatConfig.Mentions config, IPlayer player) {
        return config.everyone && player.hasPermission(Permissions.MENTION_EVERYONE);
    }

    public static List<AllowedMention> getAllowedMentions(
            MinecraftToDiscordChatConfig.Mentions config,
            IPlayer player,
            @Nullable List<Mention> mentions
    ) {
        List<AllowedMention> allowedMentions = new ArrayList<>();

        boolean users = config.users;
        boolean allUsers = users && player.hasPermission(Permissions.MENTION_USER_ALL);
        if (allUsers) {
            allowedMentions.add(AllowedMention.ALL_USERS);
        }

        boolean roles = config.roles;
        boolean allRoles = roles && player.hasPermission(Permissions.MENTION_ROLE_ALL);
        boolean mentionableRoles = roles && player.hasPermission(Permissions.MENTION_ROLE_MENTIONABLE);
        if (allRoles) {
            allowedMentions.add(AllowedMention.ALL_ROLES);
        }

        if (isEveryoneMentionAllowed(config, player)) {
            allowedMentions.add(AllowedMention.EVERYONE);
        }

        if (mentions == null) {
            return allowedMentions;
        }

        if (!allRoles && roles) {
            for (Mention mention : mentions) {
                if (mention.type() != Mention.Type.ROLE) {
                    continue;
                }

                if (mention.mentionable() && mentionableRoles) {
                    allowedMentions.add(mention.allowedMention());
                    continue;
                }
                if (player.hasPermission(mention.permission())) {
                    allowedMentions.add(mention.allowedMention());
                }
            }
        }

        return allowedMentions;
    }

    public static boolean canMentionChannel(@NotNull GuildChannel guildChannel, @Nullable DiscordUser requester) {
        Guild guild = guildChannel.getGuild();
        Member member = requester != null ? guild.getMemberById(requester.getId()) : null;

        // Base off of the user who mentioned (if available), otherwise the public role (@everyone)
        IPermissionHolder permissionHolder = (member != null ? member : guild.getPublicRole());

        // User can only mention this channel if they can see it
        return permissionHolder.hasPermission(guildChannel, Permission.VIEW_CHANNEL);
    }
}
