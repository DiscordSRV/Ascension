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

package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.connection.jda.errorresponse.ErrorCallbackContext;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.events.message.receive.game.AbstractGameMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageClusterImpl;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract class AbstractGameMessageModule<T extends IMessageConfig, E extends AbstractGameMessageReceiveEvent> extends AbstractModule<DiscordSRV> {

    public AbstractGameMessageModule(DiscordSRV discordSRV, String loggerName) {
        super(discordSRV, new NamedLogger(discordSRV, loggerName));
    }

    @Override
    public boolean isEnabled() {
        for (BaseChannelConfig channelConfig : discordSRV.channelConfig().getAllChannels()) {
            if (mapConfig(channelConfig).enabled()) {
                return true;
            }
        }
        return false;
    }

    public T mapConfig(E event, BaseChannelConfig channelConfig) {
        return mapConfig(channelConfig);
    }

    public abstract T mapConfig(BaseChannelConfig channelConfig);
    public abstract void postClusterToEventBus(ReceivedDiscordMessageCluster cluster);

    public final CompletableFuture<?> process(
            @Nullable E event,
            @Nullable DiscordSRVPlayer player,
            @Nullable GameChannel channel
    ) {
        if (player != null && !(player instanceof IPlayer)) {
            throw new IllegalArgumentException("Provided player was not created by DiscordSRV, instead was " + player.getClass().getName());
        }
        IPlayer srvPlayer = (IPlayer) player;

        if (channel == null) {
            // Send to all channels due to lack of specified channel
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (BaseChannelConfig channelConfig : discordSRV.channelConfig().getAllChannels()) {
                futures.add(forwardToChannel(event, srvPlayer, channelConfig));
            }
            return CompletableFutureUtil.combine(futures);
        }

        BaseChannelConfig channelConfig = discordSRV.channelConfig().get(channel);
        return forwardToChannel(event, srvPlayer, channelConfig);
    }

    @SuppressWarnings("unchecked") // Wacky generis
    private <CC extends BaseChannelConfig & IChannelConfig> CompletableFuture<Void> forwardToChannel(
            @Nullable E event,
            @Nullable IPlayer player,
            @NotNull BaseChannelConfig config
    ) {
        T moduleConfig = mapConfig(event, config);
        if (!moduleConfig.enabled()) {
            return null;
        }

        CC channelConfig = config instanceof IChannelConfig ? (CC) config : null;
        if (channelConfig == null) {
            return null;
        }

        return discordSRV.discordAPI().findOrCreateDestinations(channelConfig, true, true).thenCompose(messageChannels -> {
            SendableDiscordMessage.Builder format = moduleConfig.format();
            if (format == null) {
                return CompletableFuture.completedFuture(null);
            }

            Map<CompletableFuture<ReceivedDiscordMessage>, DiscordGuildMessageChannel> messageFutures;
            messageFutures = sendMessageToChannels(
                    moduleConfig, player, format, messageChannels, event,
                    // Context
                    config, player
            );

            return CompletableFuture.allOf(messageFutures.keySet().toArray(new CompletableFuture[0]))
                    .whenComplete((vo, t2) -> {
                        Set<ReceivedDiscordMessage> messages = new LinkedHashSet<>();
                        for (Map.Entry<CompletableFuture<ReceivedDiscordMessage>, DiscordGuildMessageChannel> entry : messageFutures.entrySet()) {
                            CompletableFuture<ReceivedDiscordMessage> future = entry.getKey();
                            if (future.isCompletedExceptionally()) {
                                future.exceptionally(t -> {
                                    if (t instanceof CompletionException) {
                                        t = t.getCause();
                                    }
                                    ErrorCallbackContext.context("Failed to deliver a message to " + entry.getValue()).accept(t);
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
        }).exceptionally(t -> {
            discordSRV.logger().error("Error in sending message", t);
            return null;
        });
    }

    public String convertComponent(T config, Component component) {
        return discordSRV.componentFactory().discordSerializer().serialize(component);
    }

    public Map<CompletableFuture<ReceivedDiscordMessage>, DiscordGuildMessageChannel> sendMessageToChannels(
            T config,
            IPlayer player,
            SendableDiscordMessage.Builder format,
            List<DiscordGuildMessageChannel> channels,
            E event,
            Object... context
    ) {
        SendableDiscordMessage.Formatter formatter = format.toFormatter()
                .addContext(context)
                .applyPlaceholderService();

        setPlaceholders(config, event, formatter);

        SendableDiscordMessage discordMessage = formatter
                .build();

        Map<CompletableFuture<ReceivedDiscordMessage>, DiscordGuildMessageChannel> futures = new LinkedHashMap<>();
        for (DiscordGuildMessageChannel channel : channels) {
            futures.put(channel.sendMessage(discordMessage), channel);
        }

        return futures;
    }

    public abstract void setPlaceholders(T config, E event, SendableDiscordMessage.Formatter formatter);
}
