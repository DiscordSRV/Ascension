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

package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.events.message.receive.game.AbstractGameMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.StopMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.player.IPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StopMessageModule extends AbstractGameMessageModule<StopMessageConfig, AbstractGameMessageReceiveEvent> {

    public StopMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "START_MESSAGE");
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public StopMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.stopMessage;
    }

    @Override
    public void postClusterToEventBus(GameChannel channel, @NotNull ReceivedDiscordMessageCluster cluster) {}

    @Override
    public List<CompletableFuture<ReceivedDiscordMessage>> sendMessageToChannels(
            StopMessageConfig config,
            IPlayer player,
            SendableDiscordMessage.Builder format,
            Collection<DiscordGuildMessageChannel> channels,
            AbstractGameMessageReceiveEvent event,
            Object... context
    ) {
        if (!config.enabled) {
            return Collections.emptyList();
        }
        return super.sendMessageToChannels(config, player, format, channels, event, context);
    }

    @Override
    public void setPlaceholders(StopMessageConfig config, AbstractGameMessageReceiveEvent event, SendableDiscordMessage.Formatter formatter) {}

    @Override
    public void disable() {
        try {
            process(null, null, null).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger().error("Failed to queue stop message to be sent within 5 seconds.");
        } catch (InterruptedException | ExecutionException ignored) {}
    }
}
