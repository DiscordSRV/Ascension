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

package com.discordsrv.common.messageforwarding.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.events.message.DiscordMessageDeleteEvent;
import com.discordsrv.api.discord.events.message.DiscordMessageUpdateEvent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.receive.discord.DiscordChatMessageProcessingEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.generic.DiscordIgnoresConfig;
import com.discordsrv.common.config.main.channels.MirroringConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.github.benmanes.caffeine.cache.Cache;
import net.dv8tion.jda.api.entities.Message;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DiscordMessageMirroringModule extends AbstractModule<DiscordSRV> {

    private final Cache<String, Mirror> mapping;

    public DiscordMessageMirroringModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "DISCORD_MIRRORING"));
        this.mapping = discordSRV.caffeineBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public boolean isEnabled() {
        for (BaseChannelConfig config : discordSRV.channelConfig().getAllChannels()) {
            if (config.mirroring.enabled) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Arrays.asList(DiscordGatewayIntent.GUILD_MESSAGES, DiscordGatewayIntent.MESSAGE_CONTENT);
    }

    @SuppressWarnings("unchecked") // Wacky generics
    @Subscribe
    public <CC extends BaseChannelConfig & IChannelConfig> void onDiscordChatMessageProcessing(DiscordChatMessageProcessingEvent event) {
        if (checkCancellation(event)) {
            return;
        }

        Map<GameChannel, BaseChannelConfig> channels = discordSRV.channelConfig().resolve(event.getChannel());
        if (channels == null || channels.isEmpty()) {
            return;
        }

        ReceivedDiscordMessage message = event.getDiscordMessage();

        List<CompletableFuture<List<Pair<DiscordGuildMessageChannel, MirroringConfig>>>> futures = new ArrayList<>();
        Map<ReceivedDiscordMessage.Attachment, byte[]> attachments = new LinkedHashMap<>();
        DiscordMessageEmbed.Builder attachmentEmbed = DiscordMessageEmbed.builder().setDescription("Attachments");

        for (Map.Entry<GameChannel, BaseChannelConfig> entry : channels.entrySet()) {
            BaseChannelConfig baseChannelConfig = entry.getValue();
            MirroringConfig config = baseChannelConfig.mirroring;
            if (!config.enabled) {
                continue;
            }

            DiscordIgnoresConfig ignores = config.ignores;
            if (ignores != null && ignores.shouldBeIgnored(message.isWebhookMessage(), message.getAuthor(), message.getMember())) {
                continue;
            }

            CC channelConfig = baseChannelConfig instanceof IChannelConfig ? (CC) baseChannelConfig : null;
            if (channelConfig == null) {
                continue;
            }

            MirroringConfig.AttachmentConfig attachmentConfig = config.attachments;
            int maxSize = attachmentConfig.maximumSizeKb;
            boolean embedAttachments = attachmentConfig.embedAttachments;
            if (maxSize >= 0 || embedAttachments) {
                for (ReceivedDiscordMessage.Attachment attachment : message.getAttachments()) {
                    if (attachments.containsKey(attachment)) {
                        continue;
                    }

                    if (maxSize == 0 || attachment.sizeBytes() <= (maxSize * 1000)) {
                        Request request = new Request.Builder()
                                .url(attachment.proxyUrl())
                                .get()
                                .build();

                        byte[] bytes = null;
                        try (Response response = discordSRV.httpClient().newCall(request).execute()) {
                            ResponseBody body = response.body();
                            if (body != null) {
                                bytes = body.bytes();
                            }
                        } catch (IOException e) {
                            discordSRV.logger().error("Failed to download attachment for mirroring", e);
                        }
                        attachments.put(attachment, bytes);
                        continue;
                    }

                    if (!embedAttachments) {
                        continue;
                    }

                    attachments.put(attachment, null);
                    attachmentEmbed.addField(attachment.fileName(), "[link](" + attachment.url() + ")", true);
                }
            }

            futures.add(
                    discordSRV.discordAPI().findOrCreateDestinations(channelConfig, true, true)
                            .thenApply(messageChannels -> {
                                List<Pair<DiscordGuildMessageChannel, MirroringConfig>> pairs = new ArrayList<>();
                                for (DiscordGuildMessageChannel messageChannel : messageChannels) {
                                    pairs.add(Pair.of(messageChannel, config));
                                }
                                return pairs;
                            })
            );
        }

        CompletableFutureUtil.combine(futures).whenComplete((lists, t) -> {
            List<CompletableFuture<Pair<ReceivedDiscordMessage, MirroringConfig>>> messageFutures = new ArrayList<>();
            for (List<Pair<DiscordGuildMessageChannel, MirroringConfig>> pairs : lists) {
                for (Pair<DiscordGuildMessageChannel, MirroringConfig> pair : pairs) {
                    DiscordGuildMessageChannel mirrorChannel = pair.getKey();
                    MirroringConfig config = pair.getValue();
                    MirroringConfig.AttachmentConfig attachmentConfig = config.attachments;

                    SendableDiscordMessage.Builder messageBuilder = convert(event.getDiscordMessage(), mirrorChannel, config);
                    if (!attachmentEmbed.getFields().isEmpty() && attachmentConfig.embedAttachments) {
                        messageBuilder.addEmbed(attachmentEmbed.build());
                    }

                    int maxSize = attachmentConfig.maximumSizeKb;
                    Map<String, InputStream> currentAttachments;
                    if (!attachments.isEmpty() && maxSize > 0) {
                        currentAttachments = new LinkedHashMap<>();
                        attachments.forEach((attachment, bytes) -> {
                            if (bytes != null && attachment.sizeBytes() <= maxSize) {
                                currentAttachments.put(attachment.fileName(), new ByteArrayInputStream(bytes));
                            }
                        });
                    } else {
                        currentAttachments = Collections.emptyMap();
                    }

                    CompletableFuture<Pair<ReceivedDiscordMessage, MirroringConfig>> future =
                            mirrorChannel.sendMessage(messageBuilder.build(), currentAttachments)
                                    .thenApply(msg -> Pair.of(msg, config));

                    messageFutures.add(future);
                    future.exceptionally(t2 -> {
                        discordSRV.logger().error("Failed to mirror message to " + mirrorChannel, t2);
                        return null;
                    });
                }
            }

            CompletableFutureUtil.combine(messageFutures).whenComplete((messages, t2) -> {
                Map<Long, MessageReference> references = new LinkedHashMap<>();
                for (Pair<ReceivedDiscordMessage, MirroringConfig> pair : messages) {
                    ReceivedDiscordMessage msg = pair.getKey();
                    references.put(msg.getChannel().getId(), getReference(msg, pair.getValue()));
                }
                mapping.put(getCacheKey(message), new Mirror(getReference(message, null), references));
            });
        });
    }

    @Subscribe
    public void onDiscordMessageUpdate(DiscordMessageUpdateEvent event) {
        ReceivedDiscordMessage message = event.getMessage();
        Mirror mirror = mapping.get(getCacheKey(message), k -> null);
        if (mirror == null) {
            return;
        }

        for (MessageReference reference : mirror.mirrors.values()) {
            DiscordGuildMessageChannel channel = reference.getMessageChannel(discordSRV);
            if (channel == null) {
                continue;
            }

            SendableDiscordMessage sendableMessage = convert(message, channel, reference.config).build();
            channel.editMessageById(reference.messageId, sendableMessage).whenComplete((v, t) -> {
                if (t != null) {
                    discordSRV.logger().error("Failed to update mirrored message in " + channel);
                }
            });
        }
    }

    @Subscribe
    public void onDiscordMessageDelete(DiscordMessageDeleteEvent event) {
        Mirror mirror = mapping.get(getCacheKey(event.getChannel(), event.getMessageId()), k -> null);
        if (mirror == null) {
            return;
        }

        for (MessageReference reference : mirror.mirrors.values()) {
            DiscordMessageChannel channel = reference.getMessageChannel(discordSRV);
            if (channel == null) {
                continue;
            }

            channel.deleteMessageById(reference.messageId, reference.webhookMessage).whenComplete((v, t) -> {
                if (t != null) {
                    discordSRV.logger().error("Failed to delete mirrored message in " + channel);
                }
            });
        }
    }

    /**
     * Converts a given received message to a sendable message.
     */
    private SendableDiscordMessage.Builder convert(
            ReceivedDiscordMessage message,
            DiscordGuildMessageChannel destinationChannel,
            MirroringConfig config
    ) {
        DiscordGuildMember member = message.getMember();
        DiscordUser user = message.getAuthor();
        String username = discordSRV.placeholderService().replacePlaceholders(
                config.usernameFormat, "%user_effective_name% [M]",
                member, user
        );
        if (username.length() > 32) {
            username = username.substring(0, 32);
        }

        ReceivedDiscordMessage replyMessage = message.getReplyingTo();
        String content = Objects.requireNonNull(message.getContent())
                .replace("[", "\\["); // Block markdown urls

        if (replyMessage != null) {
            MessageReference matchingReference = null;
            for (Mirror mirror : mapping.asMap().values()) {
                if (!mirror.hasMessage(replyMessage)) {
                    continue;
                }

                MessageReference ref = mirror.getForChannel(destinationChannel);
                if (ref != null) {
                    matchingReference = ref;
                    break;
                }
            }

            String jumpUrl = matchingReference != null ? String.format(
                    Message.JUMP_URL,
                    Long.toUnsignedString(destinationChannel.getGuild().getId()),
                    Long.toUnsignedString(destinationChannel.getId()),
                    Long.toUnsignedString(matchingReference.messageId)
            ) : replyMessage.getJumpUrl();

            content = discordSRV.placeholderService()
                    .replacePlaceholders(config.replyFormat, replyMessage.getMember(), replyMessage.getAuthor())
                    .replace("%message_jump_url%", jumpUrl)
                    .replace("%message%", content);
        }

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder()
                .setAllowedMentions(Collections.emptyList())
                .setContent(content.substring(0, Math.min(content.length(), Message.MAX_CONTENT_LENGTH)))
                .setWebhookUsername(username)
                .setWebhookAvatarUrl(
                        member != null
                            ? member.getEffectiveServerAvatarUrl()
                            : user.getEffectiveAvatarUrl()
                );
        builder.getAllowedMentions().clear();
        for (DiscordMessageEmbed embed : message.getEmbeds()) {
            builder.addEmbed(embed);
        }
        return builder;
    }

    private MessageReference getReference(ReceivedDiscordMessage message, MirroringConfig config) {
        return getReference(message.getChannel(), message.getId(), message.isWebhookMessage(), config);
    }

    private MessageReference getReference(
            DiscordMessageChannel channel,
            long messageId,
            boolean webhookMessage,
            MirroringConfig config
    ) {
        if (channel instanceof DiscordTextChannel) {
            DiscordTextChannel textChannel = (DiscordTextChannel) channel;
            return new MessageReference(textChannel, messageId, webhookMessage, config);
        } else if (channel instanceof DiscordThreadChannel) {
            DiscordThreadChannel threadChannel = (DiscordThreadChannel) channel;
            return new MessageReference(threadChannel, messageId, webhookMessage, config);
        }
        throw new IllegalStateException("Unexpected channel type: " + channel.getClass().getName());
    }

    private static String getCacheKey(ReceivedDiscordMessage message) {
        return getCacheKey(message.getChannel(), message.getId());
    }

    private static String getCacheKey(DiscordMessageChannel channel, long messageId) {
        if (channel instanceof DiscordTextChannel) {
            return getCacheKey(channel.getId(), 0L, messageId);
        } else if (channel instanceof DiscordThreadChannel) {
            long parentId = ((DiscordThreadChannel) channel).getParentChannel().getId();
            return getCacheKey(parentId, channel.getId(), messageId);
        }
        throw new IllegalStateException("Unexpected channel type: " + channel.getClass().getName());
    }

    private static String getCacheKey(long channelId, long threadId, long messageId) {
        return Long.toUnsignedString(channelId)
                + (threadId > 0 ? ":" + Long.toUnsignedString(threadId) : "")
                + ":" + Long.toUnsignedString(messageId);
    }

    private static class MessageReference {

        private final long channelId;
        private final long threadId;
        private final long messageId;
        private final boolean webhookMessage;
        private final MirroringConfig config;

        public MessageReference(
                DiscordTextChannel textChannel,
                long messageId,
                boolean webhookMessage,
                MirroringConfig config
        ) {
            this(textChannel.getId(), -1L, messageId, webhookMessage, config);
        }

        public MessageReference(
                DiscordThreadChannel threadChannel,
                long messageId,
                boolean webhookMessage,
                MirroringConfig config
        ) {
            this(threadChannel.getParentChannel().getId(), threadChannel.getId(), messageId, webhookMessage, config);
        }

        public MessageReference(
                long channelId,
                long threadId,
                long messageId,
                boolean webhookMessage,
                MirroringConfig config
        ) {
            this.channelId = channelId;
            this.threadId = threadId;
            this.messageId = messageId;
            this.webhookMessage = webhookMessage;
            this.config = config;
        }

        public DiscordGuildMessageChannel getMessageChannel(DiscordSRV discordSRV) {
            DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(channelId);
            if (textChannel == null) {
                return null;
            } else if (threadId == -1) {
                return textChannel;
            }

            for (DiscordThreadChannel activeThread : textChannel.getActiveThreads()) {
                if (activeThread.getId() == threadId) {
                    return activeThread;
                }
            }
            return null;
        }

        public boolean isMatching(ReceivedDiscordMessage message) {
            return isMatching((DiscordGuildMessageChannel) message.getChannel())
                    && message.getId() == messageId;
        }

        public boolean isMatching(DiscordGuildMessageChannel channel) {
            return channel instanceof DiscordThreadChannel
                    ? channel.getId() == threadId
                            && ((DiscordThreadChannel) channel).getParentChannel().getId() == channelId
                    : channel.getId() == channelId;
        }
    }

    private static class Mirror {

        private final MessageReference original;
        private final Map<Long, MessageReference> mirrors; // thread/channel id -> reference

        public Mirror(MessageReference original, Map<Long, MessageReference> mirrors) {
            this.original = original;
            this.mirrors = mirrors;
        }

        public boolean hasMessage(ReceivedDiscordMessage message) {
            if (original.isMatching(message)) {
                return true;
            }
            MessageReference reference = mirrors.get(message.getChannel().getId());
            return reference != null && reference.isMatching(message);
        }

        public MessageReference getForChannel(DiscordGuildMessageChannel channel) {
            long id = channel.getId();
            if (original.isMatching(channel)) {
                return original;
            } else {
                return mirrors.get(id);
            }
        }
    }
}
