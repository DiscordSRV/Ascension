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
import com.discordsrv.api.discord.entity.channel.DiscordGuildChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
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
import com.discordsrv.common.discord.util.DiscordPermissionUtil;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.testing.TestHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * An abstracted flow to send in-game messages to a given destination and publish the results to the event bus.
 * <p>
 * Order of operations:
 * - Event (E generic) is received, implementation calls {@link #process(AbstractGameMessageReceiveEvent, DiscordSRVPlayer, GameChannel)}
 * - {@link IPlayer} and {@link BaseChannelConfig} (uses {@link #mapConfig(AbstractGameMessageReceiveEvent, BaseChannelConfig)} are resolved, then {@link #forwardToChannel(AbstractGameMessageReceiveEvent, IPlayer, BaseChannelConfig)} is called
 * - Destinations are looked up and {@link #sendMessageToChannels} gets called
 * - {@link #setPlaceholders(IMessageConfig, AbstractGameMessageReceiveEvent, SendableDiscordMessage.Formatter)} is called to set any additional placeholders
 * - {@link #sendMessageToChannel(DiscordGuildMessageChannel, SendableDiscordMessage)} is called (once per channel) to send messages to individual channels
 * - {@link #postClusterToEventBus(ReceivedDiscordMessageCluster)} is called with all messages that were sent (if any messages were sent)
 *
 * @param <T> config model
 * @param <E> the event indicating a message was received from in-game, of type {@link AbstractGameMessageReceiveEvent}
 */
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
        if (channelConfig == null) {
            return CompletableFuture.completedFuture(null);
        }

        return forwardToChannel(event, srvPlayer, channelConfig);
    }

    @SuppressWarnings("unchecked") // Wacky generis
    protected <CC extends BaseChannelConfig & IChannelConfig> CompletableFuture<Void> forwardToChannel(
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

        return discordSRV.destinations().lookupDestination(channelConfig.destination(), true, true).thenCompose(messageChannels -> {
            SendableDiscordMessage.Builder format = moduleConfig.format();
            if (format == null || format.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            List<CompletableFuture<ReceivedDiscordMessage>> messageFutures = sendMessageToChannels(
                    moduleConfig, player, format, messageChannels, event,
                    // Context
                    config, player
            );

            return CompletableFutureUtil.combine(messageFutures).whenComplete((vo, t2) -> {
                Set<ReceivedDiscordMessage> messages = new LinkedHashSet<>();
                for (CompletableFuture<ReceivedDiscordMessage> future : messageFutures) {
                    ReceivedDiscordMessage message = future.join();
                    if (message != null) {
                        messages.add(message);
                    }
                }

                if (messages.isEmpty()) {
                    // Nothing was delivered
                    return;
                }

                postClusterToEventBus(new ReceivedDiscordMessageClusterImpl(messages));
            }).exceptionally(t -> {
                discordSRV.logger().error("Failed to publish to event bus", t);
                TestHelper.fail(t);
                return null;
            }).thenApply(v -> (Void) null);
        }).exceptionally(t -> {
            discordSRV.logger().error("Error in forwarding message", t);
            TestHelper.fail(t);
            return null;
        });
    }

    public List<CompletableFuture<ReceivedDiscordMessage>> sendMessageToChannels(
            T config,
            IPlayer player,
            SendableDiscordMessage.Builder format,
            Collection<DiscordGuildMessageChannel> channels,
            E event,
            Object... context
    ) {
        SendableDiscordMessage.Formatter formatter = format.toFormatter()
                .addContext(context)
                .applyPlaceholderService();

        setPlaceholders(config, event, formatter);

        SendableDiscordMessage discordMessage = formatter
                .build();
        if (discordMessage.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<ReceivedDiscordMessage>> futures = new ArrayList<>();
        for (DiscordGuildMessageChannel channel : channels) {
            futures.add(sendMessageToChannel(channel, discordMessage));
        }

        return futures;
    }

    @Nullable
    protected final CompletableFuture<ReceivedDiscordMessage> sendMessageToChannel(DiscordGuildMessageChannel channel, SendableDiscordMessage message) {
        GuildChannel permissionChannel = (GuildMessageChannel) channel.getAsJDAMessageChannel();

        Permission sendPermission;
        if (message.isWebhookMessage()) {
            if (permissionChannel instanceof ThreadChannel) {
                permissionChannel = ((ThreadChannel) permissionChannel).getParentChannel();
            }
            sendPermission = Permission.MANAGE_WEBHOOKS;
        } else {
            sendPermission = permissionChannel instanceof ThreadChannel
                             ? Permission.MESSAGE_SEND_IN_THREADS
                             : Permission.MESSAGE_SEND;
        }

        String missingPermissions = DiscordPermissionUtil.missingPermissionsString(permissionChannel, Permission.VIEW_CHANNEL, sendPermission);
        if (missingPermissions != null) {
            logger().error("Failed to send message to " + describeDestination(channel) + ": " + missingPermissions);
            return CompletableFuture.completedFuture(null);
        }

        return channel.sendMessage(message).exceptionally(t -> {
            ErrorCallbackContext.context("Failed to deliver a message to " + describeDestination(channel)).accept(t);
            TestHelper.fail(t);
            return null;
        });
    }

    private String describeDestination(DiscordGuildChannel channel) {
        if (channel instanceof DiscordThreadChannel) {
            return "\"" + channel.getName() + "\" in #" + ((DiscordThreadChannel) channel).getParentChannel().getName();
        }
        return "#" + channel.getName();
    }

    public abstract void setPlaceholders(T config, E event, SendableDiscordMessage.Formatter formatter);
}
