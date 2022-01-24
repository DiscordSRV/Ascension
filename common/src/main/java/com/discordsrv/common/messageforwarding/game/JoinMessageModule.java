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

package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.JoinMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.JoinMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.JoinMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.logging.NamedLogger;

public class JoinMessageModule extends AbstractGameMessageModule<JoinMessageConfig> {

    public JoinMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "JOIN_MESSAGES"));
    }

    @Override
    public OrDefault<JoinMessageConfig> mapConfig(OrDefault<BaseChannelConfig> channelConfig) {
        return channelConfig.map(cfg -> cfg.joinMessages);
    }

    @Override
    public boolean isEnabled(OrDefault<JoinMessageConfig> config) {
        return config.get(cfg -> cfg.enabled, true);
    }

    @Override
    public SendableDiscordMessage.Builder getFormat(OrDefault<JoinMessageConfig> config) {
        return config.get(cfg -> cfg.format);
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new JoinMessageForwardedEvent(cluster));
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onStatusMessageReceive(JoinMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), event.getGameChannel());
    }
}
