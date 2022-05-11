/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.entity.message.util;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.discordsrv.api.discord.api.entity.message.AllowedMention;
import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SendableDiscordMessageUtil {

    private SendableDiscordMessageUtil() {}

    public static Message toJDA(@NotNull SendableDiscordMessage message) {
        List<Message.MentionType> allowedTypes = new ArrayList<>();
        List<Long> allowedUsers = new ArrayList<>();
        List<Long> allowedRoles = new ArrayList<>();

        Set<AllowedMention> allowedMentions = message.getAllowedMentions();
        for (AllowedMention allowedMention : allowedMentions) {
            if (allowedMention instanceof AllowedMention.Snowflake) {
                long id = ((AllowedMention.Snowflake) allowedMention).getId();
                if (((AllowedMention.Snowflake) allowedMention).isUser()) {
                    allowedUsers.add(id);
                } else {
                    allowedRoles.add(id);
                }
            } else if (allowedMention instanceof AllowedMention.Standard) {
                allowedTypes.add(((AllowedMention.Standard) allowedMention).getMentionType());
            }
        }

        List<MessageEmbed> embeds = new ArrayList<>();
        for (DiscordMessageEmbed embed : message.getEmbeds()) {
            embeds.add(embed.toJDA());
        }

        MessageBuilder builder = new MessageBuilder();
        message.getContent().ifPresent(builder::setContent);

        return new MessageBuilder()
                .setContent(message.getContent().orElse(null))
                .setEmbeds(embeds)
                .setAllowedMentions(allowedTypes)
                .mentionUsers(allowedUsers.stream().mapToLong(l -> l).toArray())
                .mentionRoles(allowedRoles.stream().mapToLong(l -> l).toArray())
                .build();
    }

    public static WebhookMessageBuilder toWebhook(@NotNull SendableDiscordMessage message) {
        return WebhookMessageBuilder.fromJDA(toJDA(message))
                .setUsername(message.getWebhookUsername().orElse(null))
                .setAvatarUrl(message.getWebhookAvatarUrl().orElse(null));
    }
}
