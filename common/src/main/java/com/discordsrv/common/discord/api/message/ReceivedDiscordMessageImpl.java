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

package com.discordsrv.common.discord.api.message;

import club.minnced.discord.webhook.receive.ReadonlyEmbed;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.receive.ReadonlyUser;
import club.minnced.discord.webhook.send.WebhookEmbed;
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.exception.UnknownChannelException;
import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.impl.SendableDiscordMessageImpl;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReceivedDiscordMessageImpl extends SendableDiscordMessageImpl implements ReceivedDiscordMessage {

    public static ReceivedDiscordMessage fromJDA(DiscordSRV discordSRV, Message message) {
        List<DiscordMessageEmbed> mappedEmbeds = new ArrayList<>();
        for (MessageEmbed embed : message.getEmbeds()) {
            mappedEmbeds.add(new DiscordMessageEmbed(embed));
        }

        boolean webhookMessage = message.isWebhookMessage();
        String webhookUsername = webhookMessage ? message.getAuthor().getName() : null;
        String webhookAvatarUrl = webhookMessage ? message.getAuthor().getEffectiveAvatarUrl() : null;

        DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(message.getChannel().getId())
                .orElse(null);
        return new ReceivedDiscordMessageImpl(
                discordSRV,
                textChannel,
                message.getChannel().getId(),
                message.getId(),
                message.getContentRaw(),
                mappedEmbeds,
                webhookUsername,
                webhookAvatarUrl
        );
    }

    public static ReceivedDiscordMessage fromWebhook(DiscordSRV discordSRV, ReadonlyMessage webhookMessage) {
        List<DiscordMessageEmbed> mappedEmbeds = new ArrayList<>();
        for (ReadonlyEmbed embed : webhookMessage.getEmbeds()) {
            List<DiscordMessageEmbed.Field> fields = new ArrayList<>();
            for (WebhookEmbed.EmbedField field : embed.getFields()) {
                fields.add(new DiscordMessageEmbed.Field(field.getName(), field.getValue(), field.isInline()));
            }

            Integer color = embed.getColor();
            WebhookEmbed.EmbedAuthor author = embed.getAuthor();
            WebhookEmbed.EmbedTitle title = embed.getTitle();
            ReadonlyEmbed.EmbedImage thumbnail = embed.getThumbnail();
            ReadonlyEmbed.EmbedImage image = embed.getImage();
            WebhookEmbed.EmbedFooter footer = embed.getFooter();

            mappedEmbeds.add(new DiscordMessageEmbed(
                    color != null ? color : Role.DEFAULT_COLOR_RAW,
                    author != null ? author.getName() : null,
                    author != null ? author.getUrl() : null,
                    author != null ? author.getIconUrl() : null,
                    title != null ? title.getText() : null,
                    title != null ? title.getUrl() : null,
                    embed.getDescription(),
                    fields,
                    thumbnail != null ? thumbnail.getUrl() : null,
                    image != null ? image.getUrl() : null,
                    embed.getTimestamp(),
                    footer != null ? footer.getText() : null,
                    footer != null ? footer.getIconUrl() : null
            ));
        }

        ReadonlyUser author = webhookMessage.getAuthor();
        String authorId = Long.toUnsignedString(author.getId());
        String avatarId = author.getAvatarId();
        String avatarUrl = avatarId != null
                ? String.format(User.AVATAR_URL, authorId, avatarId, avatarId.startsWith("a_") ? "gif" : "png")
                : String.format(User.DEFAULT_AVATAR_URL, Integer.parseInt(author.getDiscriminator()) % 5);

        DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(
                Long.toUnsignedString(webhookMessage.getChannelId())).orElse(null);
        return new ReceivedDiscordMessageImpl(
                discordSRV,
                textChannel,
                Long.toUnsignedString(webhookMessage.getChannelId()),
                Long.toUnsignedString(webhookMessage.getId()),
                webhookMessage.getContent(),
                mappedEmbeds,
                author.getName(),
                avatarUrl
        );
    }

    private final DiscordSRV discordSRV;
    private final DiscordTextChannel textChannel;
    private final String channelId;
    private final String id;

    private ReceivedDiscordMessageImpl(
            DiscordSRV discordSRV,
            DiscordTextChannel textChannel,
            String channelId,
            String id,
            String content,
            List<DiscordMessageEmbed> embeds,
            String webhookUsername,
            String webhookAvatarUrl
    ) {
        super(content, embeds, null, webhookUsername, webhookAvatarUrl);
        this.discordSRV = discordSRV;
        this.textChannel = textChannel;
        this.channelId = channelId;
        this.id = id;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull DiscordTextChannel getChannel() {
        return textChannel;
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> edit(SendableDiscordMessage message) {
        DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(channelId).orElse(null);
        if (textChannel == null) {
            CompletableFuture<ReceivedDiscordMessage> future = new CompletableFuture<>();
            future.completeExceptionally(new UnknownChannelException(null));
            return future;
        }

        return textChannel.editMessageById(textChannel.getId(), message);
    }
}
