/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.message.util;

import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.discordsrv.api.discord.api.message.AllowedMention;
import com.discordsrv.api.discord.api.message.SendableDiscordMessage;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SendableDiscordMessageUtil {

    private SendableDiscordMessageUtil() {}

    public static Message toJDA(SendableDiscordMessage message) {
        List<Message.MentionType> allowedTypes = new ArrayList<>();
        List<String> allowedUsers = new ArrayList<>();
        List<String> allowedRoles = new ArrayList<>();

        for (AllowedMention allowedMention : Collections.singletonList(AllowedMention.ALL_USERS)) { // TODO
            if (allowedMention instanceof AllowedMention.Snowflake) {
                String id = ((AllowedMention.Snowflake) allowedMention).getId();
                if (((AllowedMention.Snowflake) allowedMention).isUser()) {
                    allowedUsers.add(id);
                } else {
                    allowedRoles.add(id);
                }
            } else if (allowedMention instanceof AllowedMention.Standard) {
                allowedTypes.add(((AllowedMention.Standard) allowedMention).getMentionType());
            }
        }

        // Always allow these
        allowedTypes.add(Message.MentionType.EMOTE);
        allowedTypes.add(Message.MentionType.CHANNEL);

        MessageBuilder messageBuilder = new MessageBuilder()
                .setContent(message.getContent())
                //.setEmbeds() // TODO
                .setAllowedMentions(allowedTypes)
                .mentionUsers(allowedUsers.toArray(new String[0]))
                .mentionRoles(allowedRoles.toArray(new String[0]));

        return messageBuilder.build();
    }

    public static WebhookMessage toWebhook(SendableDiscordMessage message) {
        return WebhookMessageBuilder.fromJDA(toJDA(message))
                .setUsername(message.getWebhookUsername())
                .setAvatarUrl(message.getWebhookAvatarUrl())
                .build();
    }
}
