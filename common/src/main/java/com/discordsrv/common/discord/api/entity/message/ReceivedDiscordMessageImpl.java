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

package com.discordsrv.common.discord.api.entity.message;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@PlaceholderPrefix("message_")
public class ReceivedDiscordMessageImpl implements ReceivedDiscordMessage {

    public static ReceivedDiscordMessage fromJDA(DiscordSRV discordSRV, Message message) {
        List<DiscordMessageEmbed> mappedEmbeds = new ArrayList<>();
        for (MessageEmbed embed : message.getEmbeds()) {
            mappedEmbeds.add(new DiscordMessageEmbed(embed));
        }

        boolean webhookMessage = message.isWebhookMessage();
        DiscordMessageChannel channel = discordSRV.discordAPI().getMessageChannel(message.getChannel());
        DiscordUser user = discordSRV.discordAPI().getUser(message.getAuthor());
        DiscordGuild guild = discordSRV.discordAPI().getGuild(message.getGuild());

        Member member = message.getMember();
        DiscordGuildMember apiMember = member != null ? discordSRV.discordAPI().getGuildMember(member) : null;

        boolean self = false;
        if (webhookMessage) {
            CompletableFuture<WebhookClient<Message>> clientFuture = discordSRV.discordAPI()
                    .getCachedClients()
                    .getIfPresent(channel instanceof DiscordThreadChannel
                                  ? ((DiscordThreadChannel) channel).getParentChannel().getId()
                                  : channel.getId()
                    );

            if (clientFuture != null) {
                long clientId = clientFuture.join().getIdLong();
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

        Set<DiscordUser> users = new HashSet<>();
        for (User jdaUser : message.getMentions().getUsers()) {
            users.add(discordSRV.discordAPI().getUser(jdaUser));
        }

        Set<DiscordGuildMember> members = new HashSet<>();
        for (Member jdaMember : message.getMentions().getMembers()) {
            members.add(discordSRV.discordAPI().getGuildMember(jdaMember));
        }

        Message referencedMessage = message.getReferencedMessage();
        return new ReceivedDiscordMessageImpl(
                discordSRV,
                attachments,
                self,
                channel,
                referencedMessage != null ? fromJDA(discordSRV, referencedMessage) : null,
                apiMember,
                guild,
                user,
                message.getChannel().getIdLong(),
                message.getIdLong(),
                message.getContentRaw(),
                mappedEmbeds,
                webhookMessage,
                users,
                members
        );
    }

    private final DiscordSRV discordSRV;
    private final List<Attachment> attachments;
    private final boolean fromSelf;
    private final DiscordMessageChannel channel;
    private final ReceivedDiscordMessage replyingTo;
    private final DiscordGuildMember member;
    private final DiscordGuild guild;
    private final DiscordUser author;
    private final String content;
    private final List<DiscordMessageEmbed> embeds;
    private final boolean webhookMessage;
    private final long channelId;
    private final long id;
    private final Set<DiscordUser> mentionedUsers;
    private final Set<DiscordGuildMember> mentionedMembers;

    private ReceivedDiscordMessageImpl(
            DiscordSRV discordSRV,
            List<Attachment> attachments,
            boolean fromSelf,
            DiscordMessageChannel channel,
            ReceivedDiscordMessage replyingTo,
            DiscordGuildMember member,
            DiscordGuild guild,
            DiscordUser author,
            long channelId,
            long id,
            String content,
            List<DiscordMessageEmbed> embeds,
            boolean webhookMessage,
            Set<DiscordUser> mentionedUsers,
            Set<DiscordGuildMember> mentionedMembers
    ) {
        this.discordSRV = discordSRV;
        this.attachments = Collections.unmodifiableList(attachments);
        this.fromSelf = fromSelf;
        this.channel = channel;
        this.replyingTo = replyingTo;
        this.member = member;
        this.guild = guild;
        this.author = author;
        this.content = content;
        this.embeds = Collections.unmodifiableList(embeds);
        this.webhookMessage = webhookMessage;
        this.channelId = channelId;
        this.id = id;
        this.mentionedUsers = Collections.unmodifiableSet(mentionedUsers);
        this.mentionedMembers = Collections.unmodifiableSet(mentionedMembers);
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
        DiscordGuild guild = getGuild();
        return String.format(
                Message.JUMP_URL,
                guild != null ? Long.toUnsignedString(guild.getId()) : "@me",
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
    public @Nullable DiscordGuildMember getMember() {
        return member;
    }

    @Override
    public @Nullable DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public Set<DiscordUser> getMentionedUsers() {
        return mentionedUsers;
    }

    @Override
    public Set<DiscordGuildMember> getMentionedMembers() {
        return mentionedMembers;
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
    public @Nullable ReceivedDiscordMessage getReplyingTo() {
        return replyingTo;
    }

    @Override
    public @NotNull Task<Void> delete() {
        DiscordMessageChannel messageChannel = discordSRV.discordAPI().getMessageChannelById(channelId);
        if (messageChannel == null) {
            return Task.failed(new RestErrorResponseException(ErrorResponse.UNKNOWN_CHANNEL));
        }

        return messageChannel.deleteMessageById(getId(), fromSelf && webhookMessage);
    }

    @Override
    public Task<ReceivedDiscordMessage> edit(
            @NotNull SendableDiscordMessage message
    ) {
        if (!webhookMessage && message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot edit a non-webhook message into a webhook message");
        }

        DiscordMessageChannel messageChannel = discordSRV.discordAPI().getMessageChannelById(channelId);
        if (messageChannel == null) {
            return Task.failed(new RestErrorResponseException(ErrorResponse.UNKNOWN_CHANNEL));
        }

        return messageChannel.editMessageById(getId(), message);
    }

    @Override
    public Task<ReceivedDiscordMessage> reply(@NotNull SendableDiscordMessage message) {
        if (message.isWebhookMessage()) {
            throw new IllegalStateException("Webhook messages cannot be used as replies");
        }

        DiscordMessageChannel messageChannel = discordSRV.discordAPI().getMessageChannelById(channelId);
        if (messageChannel == null) {
            return Task.failed(new RestErrorResponseException(ErrorResponse.UNKNOWN_CHANNEL));
        }

        return messageChannel.sendMessage(message.withReplyingToMessageId(id));
    }

    @Override
    public String toString() {
        return "ReceivedMessage:" + Long.toUnsignedString(getId()) + "/" + getChannel();
    }
}
