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

package com.discordsrv.common.discord.api.entity.message;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyAttachment;
import club.minnced.discord.webhook.receive.ReadonlyEmbed;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookEmbed;
import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ReceivedDiscordMessageImpl implements ReceivedDiscordMessage {

    public static ReceivedDiscordMessage fromJDA(DiscordSRV discordSRV, Message message) {
        List<DiscordMessageEmbed> mappedEmbeds = new ArrayList<>();
        for (MessageEmbed embed : message.getEmbeds()) {
            mappedEmbeds.add(new DiscordMessageEmbed(embed));
        }

        boolean webhookMessage = message.isWebhookMessage();
        DiscordMessageChannel channel = discordSRV.discordAPI().getMessageChannel(message.getChannel());
        DiscordUser user = discordSRV.discordAPI().getUser(message.getAuthor());

        Member member = message.getMember();
        DiscordGuildMember apiMember = member != null ? discordSRV.discordAPI().getGuildMember(member) : null;

        boolean self = false;
        if (webhookMessage) {
            CompletableFuture<WebhookClient> clientFuture = discordSRV.discordAPI()
                    .getCachedClients()
                    .getIfPresent(channel instanceof DiscordThreadChannel
                                  ? ((DiscordThreadChannel) channel).getParentChannel().getId()
                                  : channel.getId()
                    );

            if (clientFuture != null) {
                long clientId = clientFuture.join().getId();
                self = clientId == user.getId();
            }
        } else {
            self = user.isSelf();
        }

        List<Attachment> attachments = new ArrayList<>();
        for (Message.Attachment attachment : message.getAttachments()) {
            attachments.add(new Attachment(
                    attachment.getFileName(),
                    attachment.getUrl(),
                    attachment.getProxyUrl(),
                    attachment.getSize()
            ));
        }

        Message referencedMessage = message.getReferencedMessage();
        return new ReceivedDiscordMessageImpl(
                discordSRV,
                attachments,
                self,
                channel,
                referencedMessage != null ? fromJDA(discordSRV, referencedMessage) : null,
                apiMember,
                user,
                message.getChannel().getIdLong(),
                message.getIdLong(),
                message.getContentRaw(),
                mappedEmbeds,
                webhookMessage
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
                    color != null ? new Color(color) : null,
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

        DiscordMessageChannel channel = discordSRV.discordAPI().getMessageChannelById(
                webhookMessage.getChannelId()).orElse(null);
        DiscordUser user = discordSRV.discordAPI().getUserById(
                webhookMessage.getAuthor().getId()).orElse(null);
        DiscordGuildMember member = channel instanceof DiscordTextChannel && user != null
                ? ((DiscordTextChannel) channel).getGuild().getMemberById(user.getId()).orElse(null) : null;

        List<Attachment> attachments = new ArrayList<>();
        for (ReadonlyAttachment attachment : webhookMessage.getAttachments()) {
            attachments.add(new Attachment(
                    attachment.getFileName(),
                    attachment.getUrl(),
                    attachment.getProxyUrl(),
                    attachment.getSize()
            ));
        }

        return new ReceivedDiscordMessageImpl(
                discordSRV,
                attachments,
                true, // These are always from rest responses
                channel,
                null,
                member,
                user,
                webhookMessage.getChannelId(),
                webhookMessage.getId(),
                webhookMessage.getContent(),
                mappedEmbeds,
                true
        );
    }

    private final DiscordSRV discordSRV;
    private final List<Attachment> attachments;
    private final boolean fromSelf;
    private final DiscordMessageChannel channel;
    private final ReceivedDiscordMessage replyingTo;
    private final DiscordGuildMember member;
    private final DiscordUser author;
    private final String content;
    private final List<DiscordMessageEmbed> embeds;
    private final boolean webhookMessage;
    private final long channelId;
    private final long id;

    private ReceivedDiscordMessageImpl(
            DiscordSRV discordSRV,
            List<Attachment> attachments,
            boolean fromSelf,
            DiscordMessageChannel channel,
            ReceivedDiscordMessage replyingTo,
            DiscordGuildMember member,
            DiscordUser author,
            long channelId,
            long id,
            String content,
            List<DiscordMessageEmbed> embeds,
            boolean webhookMessage
    ) {
        this.discordSRV = discordSRV;
        this.attachments = attachments;
        this.fromSelf = fromSelf;
        this.channel = channel;
        this.replyingTo = replyingTo;
        this.member = member;
        this.author = author;
        this.content = content;
        this.embeds = embeds;
        this.webhookMessage = webhookMessage;
        this.channelId = channelId;
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public @NotNull String getContent() {
        return content;
    }

    @Override
    public @NotNull @Unmodifiable List<DiscordMessageEmbed> getEmbeds() {
        return embeds;
    }

    @Override
    public boolean isWebhookMessage() {
        return webhookMessage;
    }

    @Override
    public @NotNull String getJumpUrl() {
        return String.format(
                Message.JUMP_URL,
                getGuild().map(guild -> Long.toUnsignedString(guild.getId())).orElse("@me"),
                Long.toUnsignedString(getChannel().getId()),
                Long.toUnsignedString(id)
        );
    }

    @Override
    public @NotNull List<Attachment> getAttachments() {
        return attachments;
    }

    @Override
    public boolean isFromSelf() {
        return fromSelf;
    }

    @Override
    public @NotNull Optional<DiscordTextChannel> getTextChannel() {
        return channel instanceof DiscordTextChannel
                ? Optional.of((DiscordTextChannel) channel)
                : Optional.empty();
    }

    @Override
    public @NotNull Optional<DiscordDMChannel> getDMChannel() {
        return channel instanceof DiscordDMChannel
                ? Optional.of((DiscordDMChannel) channel)
                : Optional.empty();
    }

    @Override
    public @NotNull Optional<DiscordGuildMember> getMember() {
        return Optional.ofNullable(member);
    }

    @Override
    public @NotNull DiscordUser getAuthor() {
        return author;
    }

    @Override
    public @NotNull DiscordMessageChannel getChannel() {
        return channel;
    }

    @Override
    public @NotNull Optional<ReceivedDiscordMessage> getReplyingTo() {
        return Optional.ofNullable(replyingTo);
    }

    @Override
    public @NotNull CompletableFuture<Void> delete() {
        DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(channelId).orElse(null);
        if (textChannel == null) {
            return CompletableFutureUtil.failed(new RestErrorResponseException(ErrorResponse.UNKNOWN_CHANNEL));
        }

        return textChannel.deleteMessageById(getId(), fromSelf && webhookMessage);
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> edit(SendableDiscordMessage message) {
        if (!webhookMessage && message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot edit a non-webhook message into a webhook message");
        }

        DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(channelId).orElse(null);
        if (textChannel == null) {
            return CompletableFutureUtil.failed(new RestErrorResponseException(ErrorResponse.UNKNOWN_CHANNEL));
        }

        return textChannel.editMessageById(getId(), message);
    }

    //
    // Placeholders
    //

    @Placeholder("message_attachments")
    public Component _attachments(OrDefault<BaseChannelConfig> config, @PlaceholderRemainder String suffix) {
        if (suffix.startsWith("_")) {
            suffix = suffix.substring(1);
        } else if (!suffix.isEmpty()) {
            return null;
        }

        String attachmentFormat = config.map(cfg -> cfg.discordToMinecraft).get(cfg -> cfg.attachmentFormat);
        List<Component> components = new ArrayList<>();
        for (Attachment attachment : attachments) {
            components.add(ComponentUtil.fromAPI(
                    discordSRV.componentFactory().textBuilder(attachmentFormat)
                            .addReplacement("%file_name%", attachment.fileName())
                            .addReplacement("%file_url%", attachment.url())
                            .build()
            ));
        }

        return ComponentUtil.join(Component.text(suffix), components);
    }

    @Override
    public String toString() {
        return "ReceivedMessage:" + Long.toUnsignedString(getId()) + "/" + getChannel();
    }
}
