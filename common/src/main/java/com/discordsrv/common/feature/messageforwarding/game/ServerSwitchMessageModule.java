/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.feature.messageforwarding.game;

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.post.game.AbstractGameMessagePostEvent;
import com.discordsrv.api.events.message.post.game.ServerSwitchMessagePostEvent;
import com.discordsrv.api.events.message.postprocess.game.ServerSwitchMessagePostProcessEvent;
import com.discordsrv.api.events.message.preprocess.game.ServerSwitchMessagePreProcessEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.proxy.ProxyBaseChannelConfig;
import com.discordsrv.common.config.main.channels.proxy.ServerSwitchMessageConfig;

import java.util.List;

public class ServerSwitchMessageModule extends AbstractGameMessageModule<ServerSwitchMessageConfig, ServerSwitchMessagePreProcessEvent, ServerSwitchMessagePostProcessEvent> {

    public ServerSwitchMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "SERVER_SWITCH_MESSAGES");
    }

    @Subscribe(priority = EventPriorities.LAST, ignoreCancelled = false, ignoreProcessed = false)
    public void onServerSwitchMessageReceive(ServerSwitchMessagePreProcessEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        discordSRV.scheduler().run(() -> process(event, event.getPlayer(), null));
        event.markAsProcessed();
    }

    @Override
    public ServerSwitchMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return ((ProxyBaseChannelConfig) channelConfig).serverSwitchMessages;
    }

    @Override
    protected ServerSwitchMessagePostProcessEvent createPostProcessEvent(
            ServerSwitchMessagePreProcessEvent preEvent,
            IPlayer player,
            List<DiscordGuildMessageChannel> channels,
            SendableDiscordMessage discordMessage
    ) {
        return new ServerSwitchMessagePostProcessEvent(preEvent, player, channels, discordMessage);
    }

    @Override
    protected AbstractGameMessagePostEvent<ServerSwitchMessagePostProcessEvent> createPostEvent(
            ServerSwitchMessagePostProcessEvent preEvent, ReceivedDiscordMessageCluster cluster) {
        return new ServerSwitchMessagePostEvent(preEvent, cluster);
    }

    @Override
    public void setPlaceholders(
            ServerSwitchMessageConfig config,
            ServerSwitchMessagePreProcessEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        formatter.addPlaceholder("message", event.getMessage());
    }
}
