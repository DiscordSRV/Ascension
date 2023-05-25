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
import com.discordsrv.api.event.events.message.forward.game.JoinMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.JoinMessageReceiveEvent;
import com.discordsrv.api.placeholder.FormattedText;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.IMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;

public class JoinMessageModule extends AbstractGameMessageModule<IMessageConfig, JoinMessageReceiveEvent> {

    public JoinMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "JOIN_MESSAGES");
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onJoinMessageReceive(JoinMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), event.getGameChannel());
        event.markAsProcessed();
    }

    @Override
    public IMessageConfig mapConfig(JoinMessageReceiveEvent event, BaseChannelConfig channelConfig) {
        return channelConfig.joinMessages().getForEvent(event);
    }

    @Override
    public IMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.joinMessages();
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new JoinMessageForwardedEvent(cluster));
    }

    @Override
    public void setPlaceholders(
            IMessageConfig config,
            JoinMessageReceiveEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        MinecraftComponent messageComponent = event.getMessage();
        String message = messageComponent != null ? convertComponent(config, ComponentUtil.fromAPI(messageComponent)) : null;

        formatter.addPlaceholder("message", new FormattedText(message));
    }
}
