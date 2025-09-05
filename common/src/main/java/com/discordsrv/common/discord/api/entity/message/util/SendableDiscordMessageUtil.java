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

package com.discordsrv.common.discord.api.entity.message.util;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SendableDiscordMessageUtil {

    private SendableDiscordMessageUtil() {}

    @SuppressWarnings("unchecked")
    private static <T extends AbstractMessageBuilder<?, ?>> T jdaBuilder(@NotNull SendableDiscordMessage message, T builder) {
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

        List<FileUpload> uploads = new ArrayList<>();
        for (Map.Entry<InputStream, String> attachment : message.getAttachments().entrySet()) {
            uploads.add(FileUpload.fromData(attachment.getKey(), attachment.getValue()));
        }

        if (!allowedUsers.isEmpty()) {
            builder = (T) builder.mentionUsers(allowedUsers.stream().mapToLong(l -> l).toArray());
        }
        if (!allowedRoles.isEmpty()) {
            builder = (T) builder.mentionRoles(allowedRoles.stream().mapToLong(l -> l).toArray());
        }

        return (T) builder
                .setContent(message.getContent())
                .setEmbeds(embeds)
                .setAllowedMentions(allowedTypes)
                .setSuppressEmbeds(message.isSuppressedEmbeds())
                .setFiles(uploads);
    }

    private static List<MessageTopLevelComponent> components(SendableDiscordMessage message) {
        return message.getComponents().stream()
                .map(JDAEntity::asJDA)
                .map(entity -> (MessageTopLevelComponent) entity)
                .collect(Collectors.toList());
    }

    public static MessageCreateData toJDASend(@NotNull SendableDiscordMessage message) {
        return jdaBuilder(message, new MessageCreateBuilder())
                .addComponents(components(message))
                .setSuppressedNotifications(message.isSuppressedNotifications())
                .build();
    }

    public static MessageEditData toJDAEdit(@NotNull SendableDiscordMessage message) {
        return jdaBuilder(message, new MessageEditBuilder())
                .setComponents(components(message))
                .build();
    }
}
