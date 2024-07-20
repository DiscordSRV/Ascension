/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.discord.message.DiscordMessageDeleteEvent;
import com.discordsrv.api.event.events.discord.message.DiscordMessageUpdateEvent;
import com.discordsrv.api.event.events.message.forward.game.AbstractGameMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.discord.DiscordChatMessageReceiveEvent;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.MirroringConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.generic.DiscordIgnoresConfig;
import com.discordsrv.common.discord.util.DiscordPermissionUtil;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.github.benmanes.caffeine.cache.Cache;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class DiscordMessageMirroringModule extends AbstractModule<DiscordSRV> {

    private final Cache<Long, Cache<Long, Sync>> mapping;

    public DiscordMessageMirroringModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "DISCORD_MIRRORING"));
        this.mapping = discordSRV.caffeineBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .expireAfterAccess(30, TimeUnit.MINUTES)
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
        return EnumSet.of(DiscordGatewayIntent.GUILD_MESSAGES, DiscordGatewayIntent.MESSAGE_CONTENT);
    }

    @SuppressWarnings("unchecked") // Wacky generics
    @Subscribe
    public <CC extends BaseChannelConfig & IChannelConfig> void onDiscordChatMessageProcessing(DiscordChatMessageReceiveEvent event) {
        if (checkCancellation(event)) {
            return;
        }

        Map<GameChannel, BaseChannelConfig> channels = discordSRV.channelConfig().resolve(event.getChannel());
        if (channels == null || channels.isEmpty()) {
            return;
        }

        ReceivedDiscordMessage message = event.getMessage();

        List<CompletableFuture<MirrorOperation>> futures = new ArrayList<>();
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
            int maxSize = attachmentConfig.maximumSizeKb * 1000;
            boolean embedAttachments = attachmentConfig.embedAttachments;
            if (maxSize >= 0 || embedAttachments) {
                for (ReceivedDiscordMessage.Attachment attachment : message.getAttachments()) {
                    if (attachments.containsKey(attachment)) {
                        continue;
                    }

                    if (maxSize == 0 || attachment.sizeBytes() <= maxSize) {
                        Request request = new Request.Builder()
                                .url(attachment.url())
                                .get()
                                .addHeader("Accept", "*/*")
                                .build();

                        byte[] bytes = null;
                        try (Response response = discordSRV.httpClient().newCall(request).execute()) {
                            ResponseBody body = response.body();
                            if (body != null) {
                                bytes = body.bytes();
                            }
                        } catch (IOException e) {
                            logger().error("Failed to download attachment for mirroring", e);
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
                    discordSRV.destinations().lookupDestination(channelConfig.destination(), true, true)
                            .thenApply(messageChannels -> {
                                List<MirrorTarget> targets = new ArrayList<>();
                                for (DiscordGuildMessageChannel messageChannel : messageChannels) {
                                    targets.add(new MirrorTarget(messageChannel, config));
                                }
                                return new MirrorOperation(message, config, targets);
                            })
            );
        }

        CompletableFutureUtil.combine(futures).whenComplete((lists, v) -> {
            Set<Long> channelIdsHandled = new HashSet<>();
            for (MirrorOperation operation : lists) {
                List<CompletableFuture<MirroredMessage>> mirrorFutures = new ArrayList<>();

                for (MirrorTarget target : operation.targets) {
                    DiscordGuildMessageChannel mirrorChannel = target.targetChannel;
                    long channelId = mirrorChannel.getId();
                    if (channelId == event.getChannel().getId() || channelIdsHandled.contains(channelId)) {
                        continue;
                    }
                    channelIdsHandled.add(channelId);

                    MirroringConfig config = target.config;
                    MirroringConfig.AttachmentConfig attachmentConfig = config.attachments;

                    SendableDiscordMessage.Builder messageBuilder = convert(event.getMessage(), mirrorChannel, config);
                    if (!attachmentEmbed.getFields().isEmpty() && attachmentConfig.embedAttachments) {
                        messageBuilder.addEmbed(attachmentEmbed.build());
                    }

                    int maxSize = attachmentConfig.maximumSizeKb * 1000;
                    List<InputStream> streams = new ArrayList<>();
                    if (!attachments.isEmpty() && maxSize >= 0) {
                        attachments.forEach((attachment, bytes) -> {
                            if (bytes != null && (maxSize == 0 || attachment.sizeBytes() <= maxSize)) {
                                InputStream stream = new ByteArrayInputStream(bytes);
                                streams.add(stream);
                                messageBuilder.addAttachment(stream, attachment.fileName());
                            }
                        });
                    }

                    if (messageBuilder.isEmpty()) {
                        logger().debug("Nothing to mirror to " + mirrorChannel + ", skipping");
                        for (InputStream stream : streams) {
                            try {
                                stream.close();
                            } catch (IOException ignored) {}
                        }
                        return;
                    }

                    GuildMessageChannel channel = (GuildMessageChannel) mirrorChannel.getAsJDAMessageChannel();
                    String missingPermissions = DiscordPermissionUtil.missingPermissionsString(channel, Permission.VIEW_CHANNEL, Permission.MANAGE_WEBHOOKS);
                    if (missingPermissions != null) {
                        logger().error("Failed to mirror message to " + describeChannel(mirrorChannel) + ": " + missingPermissions);
                        continue;
                    }

                    CompletableFuture<MirroredMessage> future =
                            mirrorChannel.sendMessage(messageBuilder.build())
                                    .thenApply(msg -> new MirroredMessage(msg, config));

                    mirrorFutures.add(future);
                    future.exceptionally(t -> {
                        if (t instanceof CompletionException) {
                            t = t.getCause();
                        }
                        logger().error("Failed to mirror message to " + describeChannel(mirrorChannel), t);
                        for (InputStream stream : streams) {
                            try {
                                stream.close();
                            } catch (IOException ignored) {}
                        }
                        return null;
                    });
                }

                CompletableFutureUtil.combine(mirrorFutures).whenComplete((messages, t2) -> {
                    MessageReference reference = getReference(operation.originalMessage, operation.configForOriginalMessage);

                    Map<ReceivedDiscordMessage, MessageReference> references = new LinkedHashMap<>();
                    references.put(message, reference);
                    for (MirroredMessage mirroredMessage : messages) {
                        references.put(mirroredMessage.message, getReference(mirroredMessage));
                    }

                    putIntoCache(reference, references);
                });
            }
        }).exceptionally(t -> {
            if (t instanceof CompletionException) {
                t = t.getCause();
            }
            logger().error("Failed to mirror message", t);
            return null;
        });
    }

    private String describeChannel(DiscordGuildMessageChannel channel) {
        if (channel instanceof DiscordThreadChannel) {
            return "\"" + channel.getName() + "\" in #" + ((DiscordThreadChannel) channel).getParentChannel().getName();
        }

        return "#" + channel.getName();
    }

    @Subscribe
    public void onDiscordMessageUpdate(DiscordMessageUpdateEvent event) {
        Cache<Long, Sync> syncs = mapping.getIfPresent(event.getChannel().getId());
        if (syncs == null) {
            return;
        }

        ReceivedDiscordMessage message = event.getMessage();
        Sync sync = syncs.getIfPresent(message.getId());
        if (sync == null || sync.original == null || !sync.original.isMatching(message)) {
            return;
        }

        for (MessageReference reference : sync.mirrors) {
            DiscordGuildMessageChannel channel = reference.getMessageChannel(discordSRV);
            if (channel == null) {
                continue;
            }

            SendableDiscordMessage sendableMessage = convert(message, channel, reference.config).build();
            channel.editMessageById(reference.messageId, sendableMessage).exceptionally(t -> {
                logger().error("Failed to update mirrored message in " + channel);
                return null;
            });
        }
    }

    @Subscribe
    public void onDiscordMessageDelete(DiscordMessageDeleteEvent event) {
        Cache<Long, Sync> syncs = mapping.getIfPresent(event.getChannel().getId());
        if (syncs == null) {
            return;
        }

        Sync sync = syncs.getIfPresent(event.getMessageId());
        if (sync == null || sync.original == null || !sync.original.isMatching(event.getChannel())
                || sync.original.messageId != event.getMessageId()) {
            return;
        }

        for (MessageReference reference : sync.mirrors) {
            DiscordGuildMessageChannel channel = reference.getMessageChannel(discordSRV);
            if (channel == null) {
                continue;
            }

            channel.deleteMessageById(reference.messageId, reference.webhookMessage).exceptionally(t -> {
                logger().error("Failed to delete mirrored message in " + describeChannel(channel));
                return null;
            });
        }
    }

    @Subscribe
    public void onGameMessageForwarded(AbstractGameMessageForwardedEvent event) {
        Set<? extends ReceivedDiscordMessage> messages = event.getDiscordMessage().getMessages();

        Map<ReceivedDiscordMessage, MessageReference> references = new LinkedHashMap<>();
        for (ReceivedDiscordMessage message : messages) {
            DiscordMessageChannel channel = message.getChannel();

            MirroringConfig config = discordSRV.channelConfig().resolve(channel).values().iterator().next().mirroring; // TODO: add channel to event
            MessageReference reference = getReference(message, config);
            references.put(message, reference);
        }

        putIntoCache(null, references);
    }

    @SuppressWarnings("DataFlowIssue") // Supplier always returns a non-null value
    @NotNull
    private Cache<Long, Sync> getCache(long channelId) {
        return mapping.get(
                channelId,
                k -> discordSRV.caffeineBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .build()
        );
    }

    private void putIntoCache(@Nullable MessageReference original, Map<ReceivedDiscordMessage, MessageReference> references) {
        if (original == null && references.size() <= 1) {
            return;
        }

        for (Map.Entry<ReceivedDiscordMessage, MessageReference> entry : references.entrySet()) {
            ReceivedDiscordMessage message = entry.getKey();
            MessageReference reference = entry.getValue();

            List<MessageReference> refs = new ArrayList<>();
            for (MessageReference ref : references.values()) {
                if (ref == reference) {
                    continue;
                }
                refs.add(ref);
            }

            getCache(message.getChannel().getId()).put(message.getId(), new Sync(original, refs));
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
        String username = discordSRV.placeholderService().replacePlaceholders(config.usernameFormat, member, user);
        if (username.length() > 32) {
            username = username.substring(0, 32);
        }

        ReceivedDiscordMessage replyMessage = message.getReplyingTo();
        String content = Objects.requireNonNull(message.getContent())
                .replace("[", "\\["); // Block markdown urls

        String finalContent;
        if (replyMessage != null) {
            MessageReference matchingReference = null;

            Cache<Long, Sync> syncs = mapping.getIfPresent(replyMessage.getChannel().getId());
            if (syncs != null) {
                Sync sync = syncs.getIfPresent(replyMessage.getId());
                if (sync != null) {
                    matchingReference = sync.getForChannel(destinationChannel);
                }
            }

            String jumpUrl = matchingReference != null ? String.format(
                    Message.JUMP_URL,
                    Long.toUnsignedString(destinationChannel.getGuild().getId()),
                    Long.toUnsignedString(destinationChannel.getId()),
                    Long.toUnsignedString(matchingReference.messageId)
            ) : replyMessage.getJumpUrl();

            finalContent = PlainPlaceholderFormat.supplyWith(
                    PlainPlaceholderFormat.Formatting.DISCORD,
                    () -> discordSRV.placeholderService()
                            .replacePlaceholders(
                                    config.replyFormat,
                                    replyMessage.getMember(),
                                    replyMessage.getAuthor(),
                                    new SinglePlaceholder("message_jump_url", jumpUrl),
                                    new SinglePlaceholder("message", content)
                            )
            );
        } else {
            finalContent = content;
        }

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder()
                .setAllowedMentions(Collections.emptyList())
                .setContent(finalContent.substring(0, Math.min(finalContent.length(), Message.MAX_CONTENT_LENGTH)))
                .setWebhookUsername(username)
                .setWebhookAvatarUrl(
                        member != null
                            ? member.getEffectiveServerAvatarUrl()
                            : user.getEffectiveAvatarUrl()
                );
        for (DiscordMessageEmbed embed : message.getEmbeds()) {
            builder.addEmbed(embed);
        }
        return builder;
    }

    private MessageReference getReference(MirroredMessage message) {
        return getReference(message.message, message.config);
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

    private static class MirrorOperation {

        private final ReceivedDiscordMessage originalMessage;
        private final MirroringConfig configForOriginalMessage;
        private final List<MirrorTarget> targets;

        public MirrorOperation(ReceivedDiscordMessage originalMessage, MirroringConfig configForOriginalMessage, List<MirrorTarget> targets) {
            this.originalMessage = originalMessage;
            this.configForOriginalMessage = configForOriginalMessage;
            this.targets = targets;
        }

    }

    private static class MirrorTarget {

        private final DiscordGuildMessageChannel targetChannel;
        private final MirroringConfig config;

        public MirrorTarget(DiscordGuildMessageChannel targetChannel, MirroringConfig config) {
            this.targetChannel = targetChannel;
            this.config = config;
        }
    }

    private static class MirroredMessage {

        private final ReceivedDiscordMessage message;
        private final MirroringConfig config;

        public MirroredMessage(ReceivedDiscordMessage message, MirroringConfig config) {
            this.message = message;
            this.config = config;
        }
    }

    private static class Sync {

        private final MessageReference original;
        private final List<MessageReference> mirrors;

        public Sync(MessageReference original, List<MessageReference> mirrors) {
            this.original = original;
            this.mirrors = mirrors;
        }

        public MessageReference getForChannel(DiscordGuildMessageChannel channel) {
            for (MessageReference mirror : mirrors) {
                if (mirror.isMatching(channel)) {
                    return mirror;
                }
            }
            return null;
        }

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

        public boolean isMatching(DiscordMessageChannel channel) {
            return channel instanceof DiscordThreadChannel
                    ? channel.getId() == threadId
                            && ((DiscordThreadChannel) channel).getParentChannel().getId() == channelId
                    : channel.getId() == channelId;
        }

        public boolean isMatching(ReceivedDiscordMessage message) {
            return isMatching(message.getChannel()) && messageId == message.getId();
        }
    }
}
