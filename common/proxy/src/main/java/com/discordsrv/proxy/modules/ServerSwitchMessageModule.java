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

package com.discordsrv.proxy.modules;

import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.ServerSwitchMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.ServerSwitchMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.messageforwarding.game.AbstractGameMessageModule;
import com.discordsrv.proxy.config.channels.ServerSwitchMessageConfig;
import com.discordsrv.proxy.config.channels.base.ProxyBaseChannelConfig;

public class ServerSwitchMessageModule extends AbstractGameMessageModule<ServerSwitchMessageConfig, ServerSwitchMessageReceiveEvent> {

    public ServerSwitchMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "SERVER_SWITCH_MESSAGES");
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onServerSwitchReceive(ServerSwitchMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), null);
        event.markAsProcessed();
    }

    @Override
    public OrDefault<ServerSwitchMessageConfig> mapConfig(OrDefault<BaseChannelConfig> channelConfig) {
        return channelConfig.map(cfg -> ((ProxyBaseChannelConfig) cfg).serverSwitchMessages);
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new ServerSwitchMessageForwardedEvent(cluster));
    }
}
