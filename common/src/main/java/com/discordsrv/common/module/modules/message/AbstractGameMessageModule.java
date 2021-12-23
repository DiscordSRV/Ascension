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

package com.discordsrv.common.module.modules.message;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.api.util.DiscordFormattingUtil;
import com.discordsrv.api.event.events.message.receive.game.AbstractGameMessageReceiveEvent;
import com.discordsrv.api.placeholder.FormattedText;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.discord.api.message.ReceivedDiscordMessageClusterImpl;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.module.type.AbstractModule;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractGameMessageModule<T> extends AbstractModule {

    public AbstractGameMessageModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    public abstract OrDefault<T> mapConfig(OrDefault<BaseChannelConfig> channelConfig);
    public abstract boolean isEnabled(OrDefault<T> config);
    public abstract SendableDiscordMessage.Builder getFormat(OrDefault<T> config);
    public abstract void postClusterToEventBus(ReceivedDiscordMessageCluster cluster);

    public final void process(
            @NotNull AbstractGameMessageReceiveEvent event,
            @NotNull DiscordSRVPlayer player,
            @Nullable GameChannel channel
    ) {
        if (channel == null) {
            // Send to all channels due to lack of specified channel
            for (OrDefault<BaseChannelConfig> channelConfig : discordSRV.channelConfig().getAllChannels()) {
                forwardToChannel(event, player, channelConfig);
            }
            return;
        }

        OrDefault<BaseChannelConfig> channelConfig = discordSRV.channelConfig().orDefault(channel);
        forwardToChannel(event, player, channelConfig);
    }

    private void forwardToChannel(
            @NotNull AbstractGameMessageReceiveEvent event,
            @NotNull DiscordSRVPlayer player,
            @NotNull OrDefault<BaseChannelConfig> channelConfig
    ) {
        OrDefault<T> config = mapConfig(channelConfig);
        if (!isEnabled(config)) {
            return;
        }

        List<Long> channelIds = channelConfig.get(cfg -> cfg instanceof IChannelConfig ? ((IChannelConfig) cfg).ids() : null);
        if (channelIds == null || channelIds.isEmpty()) {
            return;
        }

        SendableDiscordMessage.Builder format = getFormat(config);
        if (format == null) {
            return;
        }

        String message = convertMessage(config, ComponentUtil.fromAPI(event.getMessage()));
        List<CompletableFuture<ReceivedDiscordMessage>> futures = sendMessageToChannels(
                config, format, channelIds, message,
                // Context
                channelConfig, player
        );

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        discordSRV.logger().error("Failed to deliver message to Discord", t);
                        return;
                    }

                    List<ReceivedDiscordMessage> messages = new ArrayList<>();
                    for (CompletableFuture<ReceivedDiscordMessage> future : futures) {
                        // They are all done
                        messages.add(future.join());
                    }

                    postClusterToEventBus(new ReceivedDiscordMessageClusterImpl(messages));
                });
    }

    public String convertMessage(OrDefault<T> config, Component component) {
        return DiscordFormattingUtil.escapeContent(
                discordSRV.componentFactory().discordSerializer().serialize(component)
        );
    }

    public List<CompletableFuture<ReceivedDiscordMessage>> sendMessageToChannels(
            OrDefault<T> config,
            SendableDiscordMessage.Builder format,
            List<Long> channelIds,
            String message,
            Object... context
    ) {
        SendableDiscordMessage discordMessage = format.toFormatter()
                .addContext(context)
                .addReplacement("%message%", new FormattedText(message))
                .applyPlaceholderService()
                .build();

        List<CompletableFuture<ReceivedDiscordMessage>> futures = new ArrayList<>();
        for (Long channelId : channelIds) {
            discordSRV.discordAPI().getTextChannelById(channelId)
                    .ifPresent(channel -> futures.add(channel.sendMessage(discordMessage)));
        }

        return futures;
    }
}
