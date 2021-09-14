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

package com.discordsrv.common.listener;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.receive.game.ChatMessageProcessingEvent;
import com.discordsrv.api.event.events.message.forward.game.ChatMessageForwardedEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.ChannelConfig;
import com.discordsrv.common.config.main.channels.minecraftodiscord.MinecraftToDiscordChatConfig;
import com.discordsrv.common.discord.api.message.ReceivedDiscordMessageClusterImpl;
import com.discordsrv.common.function.OrDefault;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GameChatListener extends AbstractListener {

    public GameChatListener(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onChatReceive(ChatMessageProcessingEvent event) {
        if (checkProcessor(event) || checkCancellation(event) || !discordSRV.isReady()) {
            return;
        }

        GameChannel gameChannel = event.getGameChannel();

        OrDefault<BaseChannelConfig> channelConfig = discordSRV.channelConfig().orDefault(gameChannel);
        OrDefault<MinecraftToDiscordChatConfig> chatConfig = channelConfig.map(cfg -> cfg.minecraftToDiscord);

        SendableDiscordMessage.Builder builder = chatConfig.get(cfg -> cfg.messageFormat);
        if (builder == null) {
            return;
        }

        Component message = ComponentUtil.fromAPI(event.message());
        String serializedMessage = DiscordSerializer.INSTANCE.serialize(message);

        SendableDiscordMessage discordMessage = builder.toFormatter()
                .addContext(event.getPlayer())
                .addReplacement("%message%", serializedMessage)
                .build();

        List<Long> channelIds = channelConfig.get(cfg -> cfg instanceof ChannelConfig ? ((ChannelConfig) cfg).channelIds : null);
        if (channelIds == null || channelIds.isEmpty()) {
            return;
        }

        List<CompletableFuture<ReceivedDiscordMessage>> futures = new ArrayList<>();
        for (Long channelId : channelIds) {
            discordSRV.discordAPI().getTextChannelById(channelId).ifPresent(textChannel ->
                    futures.add(textChannel.sendMessage(discordMessage)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        discordSRV.logger().error("Failed to deliver message to Discord", t);
                        return;
                    }

                    List<ReceivedDiscordMessage> messages = new ArrayList<>();
                    for (CompletableFuture<ReceivedDiscordMessage> future : futures) {
                        messages.add(future.join());
                    }

                    discordSRV.eventBus().publish(
                            new ChatMessageForwardedEvent(
                                    new ReceivedDiscordMessageClusterImpl(messages)));
                });
    }

}
