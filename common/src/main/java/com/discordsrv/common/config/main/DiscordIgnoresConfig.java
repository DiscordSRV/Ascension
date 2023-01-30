/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.main;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigSerializable
public class DiscordIgnoresConfig {

    @Comment("User, bot and webhook ids to ignore")
    public IDs usersAndWebhookIds = new IDs();

    @Comment("Role ids for users and bots to ignore")
    public IDs roleIds = new IDs();

    @Comment("If all bots (webhooks not included) should be ignored")
    public boolean bots = false;

    @Comment("If all webhooks should be ignored (webhook messages sent by this DiscordSRV instance will always be ignored)")
    public boolean webhooks = true;

    @ConfigSerializable
    public static class IDs {

        public List<Long> ids = new ArrayList<>();

        @Comment("true for whitelisting the provided ids, false for blacklisting them")
        public boolean whitelist = false;
    }

    public boolean shouldBeIgnored(boolean webhookMessage, DiscordUser author, DiscordGuildMember member) {
        if (webhooks && webhookMessage) {
            return true;
        } else if (bots && (author.isBot() && !webhookMessage)) {
            return true;
        }

        DiscordIgnoresConfig.IDs users = usersAndWebhookIds;
        if (users != null && users.ids.contains(author.getId()) != users.whitelist) {
            return true;
        }

        DiscordIgnoresConfig.IDs roles = roleIds;
        return roles != null && Optional.ofNullable(member)
                .map(m -> m.getRoles().stream().anyMatch(role -> roles.ids.contains(role.getId())))
                .map(hasRole -> hasRole != roles.whitelist)
                .orElse(false);
    }
}
