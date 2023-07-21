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

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.ServerSwitchMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.ServerSwitchMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.proxy.ProxyBaseChannelConfig;
import com.discordsrv.common.config.main.channels.proxy.ServerSwitchMessageConfig;
import net.kyori.adventure.text.Component;

public class ServerSwitchMessageModule extends AbstractGameMessageModule<ServerSwitchMessageConfig, ServerSwitchMessageReceiveEvent> {

    public ServerSwitchMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "SERVER_SWITCH_MESSAGES");
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onServerSwitchMessageReceive(ServerSwitchMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), null);
        event.markAsProcessed();
    }

    @Override
    public ServerSwitchMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return ((ProxyBaseChannelConfig) channelConfig).serverSwitchMessages;
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new ServerSwitchMessageForwardedEvent(cluster));
    }

    @Override
    public void setPlaceholders(
            ServerSwitchMessageConfig config,
            ServerSwitchMessageReceiveEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        MinecraftComponent messageComponent = event.getMessage();
        Component message = messageComponent != null ? ComponentUtil.fromAPI(messageComponent) : null;

        formatter.addPlaceholder("message", message);
    }
}
