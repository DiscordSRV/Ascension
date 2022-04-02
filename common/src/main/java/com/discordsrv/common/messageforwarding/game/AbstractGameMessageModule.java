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

package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.api.util.DiscordFormattingUtil;
import com.discordsrv.api.event.events.message.receive.game.AbstractGameMessageReceiveEvent;
import com.discordsrv.api.placeholder.FormattedText;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.IMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageClusterImpl;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractGameMessageModule<T extends IMessageConfig> extends AbstractModule<DiscordSRV> {

    public AbstractGameMessageModule(DiscordSRV discordSRV, String loggerName) {
        super(discordSRV, new NamedLogger(discordSRV, loggerName));
    }

    public abstract OrDefault<T> mapConfig(OrDefault<BaseChannelConfig> channelConfig);
    public abstract void postClusterToEventBus(ReceivedDiscordMessageCluster cluster);

    public final CompletableFuture<?> process(
            @Nullable AbstractGameMessageReceiveEvent event,
            @Nullable DiscordSRVPlayer player,
            @Nullable GameChannel channel
    ) {
        if (channel == null) {
            // Send to all channels due to lack of specified channel
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (OrDefault<BaseChannelConfig> channelConfig : discordSRV.channelConfig().getAllChannels()) {
                futures.add(forwardToChannel(event, player, channelConfig));
            }
            return CompletableFutureUtil.combine(futures);
        }

        OrDefault<BaseChannelConfig> channelConfig = discordSRV.channelConfig().orDefault(channel);
        return forwardToChannel(event, player, channelConfig);
    }

    private CompletableFuture<Void> forwardToChannel(
            @Nullable AbstractGameMessageReceiveEvent event,
            @Nullable DiscordSRVPlayer player,
            @NotNull OrDefault<BaseChannelConfig> config
    ) {
        OrDefault<T> moduleConfig = mapConfig(config);
        if (!moduleConfig.get(IMessageConfig::enabled, true)) {
            return null;
        }

        IChannelConfig channelConfig = config.get(c -> c instanceof IChannelConfig ? (IChannelConfig) c : null);
        if (channelConfig == null) {
            return null;
        }

        List<DiscordMessageChannel> messageChannels = new CopyOnWriteArrayList<>();
        List<CompletableFuture<DiscordThreadChannel>> futures = new ArrayList<>();

        List<Long> channelIds = channelConfig.channelIds();
        if (channelIds != null) {
            for (Long channelId : channelConfig.channelIds()) {
                DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(channelId).orElse(null);
                if (textChannel != null) {
                    messageChannels.add(textChannel);
                } else if (channelId > 0) {
                    discordSRV.logger().error("Unable to find channel with ID "
                                                      + Long.toUnsignedString(channelId)
                                                      + ", unable to forward message to Discord");
                }
            }
        }

        discordSRV.discordAPI().findOrCreateThreads(config, channelConfig, messageChannels::add, futures, true);

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenCompose((v) -> {
            SendableDiscordMessage.Builder format = moduleConfig.get(IMessageConfig::format);
            if (format == null) {
                return CompletableFuture.completedFuture(null);
            }

            Component component = event != null ? ComponentUtil.fromAPI(event.getMessage()) : null;
            String message = component != null ? convertMessage(moduleConfig, component) : null;
            Map<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> messageFutures;
            messageFutures = sendMessageToChannels(
                    moduleConfig, format, messageChannels, message,
                    // Context
                    config, player
            );

            return CompletableFuture.allOf(messageFutures.keySet().toArray(new CompletableFuture[0]))
                    .whenComplete((vo, t2) -> {
                        Set<ReceivedDiscordMessage> messages = new LinkedHashSet<>();
                        for (Map.Entry<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> entry : messageFutures.entrySet()) {
                            CompletableFuture<ReceivedDiscordMessage> future = entry.getKey();
                            if (future.isCompletedExceptionally()) {
                                future.exceptionally(t -> {
                                    if (t instanceof CompletionException) {
                                        t = t.getCause();
                                    }
                                    discordSRV.discordConnectionManager().handleRequestFailure(
                                            "Failed to deliver a message to " + entry.getValue(), t);
                                    return null;
                                });
                                // Ignore ones that failed
                                continue;
                            }

                            // They are all done, so joining will return the result instantly
                            messages.add(future.join());
                        }

                        if (messages.isEmpty()) {
                            // Nothing was delivered
                            return;
                        }

                        postClusterToEventBus(new ReceivedDiscordMessageClusterImpl(messages));
                    })
                    .exceptionally(t -> {
                        if (t instanceof CompletionException) {
                            return null;
                        }
                        discordSRV.logger().error("Failed to publish to event bus", t);
                        return null;
                    });
        });
    }

    public String convertMessage(OrDefault<T> config, Component component) {
        return DiscordFormattingUtil.escapeContent(
                discordSRV.componentFactory().discordSerializer().serialize(component)
        );
    }

    public Map<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> sendMessageToChannels(
            OrDefault<T> config,
            SendableDiscordMessage.Builder format,
            List<DiscordMessageChannel> channels,
            String message,
            Object... context
    ) {
        SendableDiscordMessage discordMessage = format.toFormatter()
                .addContext(context)
                .addReplacement("%message%", new FormattedText(message))
                .applyPlaceholderService()
                .build();

        Map<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> futures = new LinkedHashMap<>();
        for (DiscordMessageChannel channel : channels) {
            futures.put(channel.sendMessage(discordMessage), channel);
        }

        return futures;
    }
}
