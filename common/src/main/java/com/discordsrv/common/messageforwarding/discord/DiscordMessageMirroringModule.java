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

package com.discordsrv.common.messageforwarding.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.api.entity.DiscordUser;
import com.discordsrv.api.discord.api.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.events.DiscordMessageDeleteEvent;
import com.discordsrv.api.discord.events.DiscordMessageUpdateEvent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.receive.discord.DiscordChatMessageProcessingEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.DiscordIgnores;
import com.discordsrv.common.config.main.channels.MirroringConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.module.type.AbstractModule;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DiscordMessageMirroringModule extends AbstractModule {

    private final Cache<MessageReference, Set<MessageReference>> mapping;

    public DiscordMessageMirroringModule(DiscordSRV discordSRV) {
        super(discordSRV);
        this.mapping = discordSRV.caffeineBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
    }

    @Subscribe
    public void onDiscordChatMessageProcessing(DiscordChatMessageProcessingEvent event) {
        if (checkCancellation(event)) {
            return;
        }

        Map<GameChannel, OrDefault<BaseChannelConfig>> channels = discordSRV.channelConfig().orDefault(event.getChannel());
        if (channels == null || channels.isEmpty()) {
            return;
        }

        ReceivedDiscordMessage message = event.getDiscordMessage();
        DiscordMessageChannel channel = event.getChannel();

        List<DiscordMessageChannel> mirrorChannels = new ArrayList<>();
        List<CompletableFuture<DiscordThreadChannel>> futures = new ArrayList<>();

        for (Map.Entry<GameChannel, OrDefault<BaseChannelConfig>> entry : channels.entrySet()) {
            OrDefault<BaseChannelConfig> channelConfig = entry.getValue();
            OrDefault<MirroringConfig> config = channelConfig.map(cfg -> cfg.mirroring);
            if (!config.get(cfg -> cfg.enabled, true)) {
                continue;
            }

            DiscordIgnores ignores = config.get(cfg -> cfg.ignores);
            if (ignores != null && ignores.shouldBeIgnored(message.isWebhookMessage(), message.getAuthor(), message.getMember().orElse(null))) {
                continue;
            }

            IChannelConfig iChannelConfig = channelConfig.get(cfg -> cfg instanceof IChannelConfig ? (IChannelConfig) cfg : null);
            if (iChannelConfig == null) {
                continue;
            }

            List<Long> channelIds = iChannelConfig.channelIds();
            if (channelIds != null) {
                for (Long channelId : channelIds) {
                    discordSRV.discordAPI().getTextChannelById(channelId).ifPresent(textChannel -> {
                        if (textChannel.getId() != channel.getId()) {
                            mirrorChannels.add(textChannel);
                        }
                    });
                }
            }

            discordSRV.discordAPI().findOrCreateThreads(iChannelConfig, threadChannel -> {
                if (threadChannel.getId() != channel.getId()) {
                    mirrorChannels.add(threadChannel);
                }
            }, futures);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, t) -> {
            List<DiscordTextChannel> text = new ArrayList<>();
            List<DiscordThreadChannel> thread = new ArrayList<>();
            for (DiscordMessageChannel mirrorChannel : mirrorChannels) {
                if (mirrorChannel instanceof DiscordTextChannel) {
                    text.add((DiscordTextChannel) mirrorChannel);
                } else if (mirrorChannel instanceof DiscordThreadChannel) {
                    thread.add((DiscordThreadChannel) mirrorChannel);
                }
            }

            SendableDiscordMessage.Builder builder = convert(event.getDiscordMessage());
            List<CompletableFuture<ReceivedDiscordMessage>> messageFutures = new ArrayList<>();
            if (!text.isEmpty()) {
                SendableDiscordMessage finalMessage = builder.build();
                for (DiscordTextChannel textChannel : text) {
                    messageFutures.add(textChannel.sendMessage(finalMessage));
                }
            }
            if (!thread.isEmpty()) {
                SendableDiscordMessage finalMessage = builder.convertToNonWebhook().build();
                for (DiscordThreadChannel threadChannel : thread) {
                    messageFutures.add(threadChannel.sendMessage(finalMessage));
                }
            }

            CompletableFuture.allOf(messageFutures.toArray(new CompletableFuture[0])).whenComplete((v2, t2) -> {
                Set<MessageReference> messages = new HashSet<>();
                for (CompletableFuture<ReceivedDiscordMessage> messageFuture : messageFutures) {
                    if (messageFuture.isCompletedExceptionally()) {
                        continue;
                    }

                    messages.add(getReference(messageFuture.join()));
                }

                mapping.put(getReference(message), messages);
            });
        });
    }

    @Subscribe
    public void onDiscordMessageUpdate(DiscordMessageUpdateEvent event) {
        ReceivedDiscordMessage message = event.getMessage();
        Set<MessageReference> references = mapping.get(getReference(message), k -> null);
        if (references == null) {
            return;
        }

        Map<DiscordTextChannel, MessageReference> text = new LinkedHashMap<>();
        Map<DiscordThreadChannel, MessageReference> thread = new LinkedHashMap<>();
        for (MessageReference reference : references) {
            DiscordMessageChannel channel = reference.getMessageChannel(discordSRV);
            if (channel instanceof DiscordTextChannel) {
                text.put((DiscordTextChannel) channel, reference);
            } else if (channel instanceof DiscordThreadChannel) {
                thread.put((DiscordThreadChannel) channel, reference);
            }
        }
        SendableDiscordMessage.Builder builder = convert(message);
        if (!text.isEmpty()) {
            SendableDiscordMessage finalMessage = builder.build();
            for (Map.Entry<DiscordTextChannel, MessageReference> entry : text.entrySet()) {
                entry.getKey().editMessageById(entry.getValue().messageId, finalMessage).whenComplete((v, t) -> {
                    if (t != null) {
                        discordSRV.logger().error("Failed to update mirrored message in " + entry.getKey());
                    }
                });
            }
        }
        if (!thread.isEmpty()) {
            SendableDiscordMessage finalMessage = builder.convertToNonWebhook().build();
            for (Map.Entry<DiscordThreadChannel, MessageReference> entry : thread.entrySet()) {
                entry.getKey().editMessageById(entry.getValue().messageId, finalMessage).whenComplete((v, t) -> {
                    if (t != null) {
                        discordSRV.logger().error("Failed to update mirrored message in " + entry.getKey());
                    }
                });
            }
        }
    }

    @Subscribe
    public void onDiscordMessageDelete(DiscordMessageDeleteEvent event) {
        Set<MessageReference> references = mapping.get(getReference(event.getChannel(), event.getMessageId(), false), k -> null);
        if (references == null) {
            return;
        }

        for (MessageReference reference : references) {
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

    private SendableDiscordMessage.Builder convert(ReceivedDiscordMessage message) {
        DiscordGuildMember member = message.getMember().orElse(null);
        DiscordUser user = message.getAuthor();

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder()
                .setContent(message.getContent().orElse(null))
                .setWebhookUsername(member != null ? member.getEffectiveName() : user.getUsername())
                .setWebhookAvatarUrl(member != null
                                     ? member.getEffectiveServerAvatarUrl()
                                     : user.getEffectiveAvatarUrl());
        for (DiscordMessageEmbed embed : message.getEmbeds()) {
            builder.addEmbed(embed);
        }
        return builder;
    }

    private MessageReference getReference(ReceivedDiscordMessage message) {
        return getReference(message.getChannel(), message.getId(), message.isWebhookMessage());
    }

    private MessageReference getReference(DiscordMessageChannel channel, long messageId, boolean webhookMessage) {
        if (channel instanceof DiscordTextChannel) {
            DiscordTextChannel textChannel = (DiscordTextChannel) channel;
            return new MessageReference(textChannel, messageId, webhookMessage);
        } else if (channel instanceof DiscordThreadChannel) {
            DiscordThreadChannel threadChannel = (DiscordThreadChannel) channel;
            return new MessageReference(threadChannel, messageId, webhookMessage);
        }
        throw new IllegalStateException("Unexpected channel type: " + channel.getClass().getName());
    }

    public static class MessageReference {

        private final long channelId;
        private final long threadId;
        private final long messageId;
        private final boolean webhookMessage;

        public MessageReference(DiscordTextChannel textChannel, long messageId, boolean webhookMessage) {
            this(textChannel.getId(), -1L, messageId, webhookMessage);
        }

        public MessageReference(DiscordThreadChannel threadChannel, long messageId, boolean webhookMessage) {
            this(threadChannel.getParentChannel().getId(), threadChannel.getId(), messageId, webhookMessage);
        }

        public MessageReference(long channelId, long threadId, long messageId, boolean webhookMessage) {
            this.channelId = channelId;
            this.threadId = threadId;
            this.messageId = messageId;
            this.webhookMessage = webhookMessage;
        }

        public DiscordMessageChannel getMessageChannel(DiscordSRV discordSRV) {
            DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(channelId).orElse(null);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageReference that = (MessageReference) o;
            // Intentionally ignores webhookMessage
            return channelId == that.channelId && threadId == that.threadId && messageId == that.messageId;
        }

        @Override
        public int hashCode() {
            // Intentionally ignores webhookMessage
            return Objects.hash(channelId, threadId, messageId);
        }
    }
}
