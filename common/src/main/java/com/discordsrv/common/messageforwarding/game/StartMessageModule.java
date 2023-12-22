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

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.events.message.receive.game.AbstractGameMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.StartMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.player.IPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StartMessageModule extends AbstractGameMessageModule<StartMessageConfig, AbstractGameMessageReceiveEvent> {

    public StartMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "START_MESSAGE");
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public StartMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.startMessage;
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {}

    @Override
    public Map<CompletableFuture<ReceivedDiscordMessage>, DiscordGuildMessageChannel> sendMessageToChannels(
            StartMessageConfig config,
            IPlayer player,
            SendableDiscordMessage.Builder format,
            Collection<DiscordGuildMessageChannel> channels,
            AbstractGameMessageReceiveEvent event,
            Object... context
    ) {
        if (!config.enabled) {
            return Collections.emptyMap();
        }
        return super.sendMessageToChannels(config, player, format, channels, event, context);
    }

    @Override
    public void setPlaceholders(StartMessageConfig config, AbstractGameMessageReceiveEvent event, SendableDiscordMessage.Formatter formatter) {}

    @Override
    public void enable() {
        process(null, null, null);
    }
}
