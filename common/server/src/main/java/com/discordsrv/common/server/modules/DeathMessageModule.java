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

package com.discordsrv.common.server.modules;

import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.DeathMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.DeathMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.server.config.channels.DeathMessageConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.messageforwarding.game.AbstractGameMessageModule;
import com.discordsrv.common.server.config.channels.base.ServerBaseChannelConfig;

public class DeathMessageModule extends AbstractGameMessageModule<DeathMessageConfig> {

    public DeathMessageModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onDeathReceive(DeathMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), event.getGameChannel());
        event.markAsProcessed();
    }

    @Override
    public OrDefault<DeathMessageConfig> mapConfig(OrDefault<BaseChannelConfig> channelConfig) {
        return channelConfig.map(cfg -> ((ServerBaseChannelConfig) cfg).deathMessages);
    }

    @Override
    public boolean isEnabled(OrDefault<DeathMessageConfig> config) {
        return config.get(cfg -> cfg.enabled, true);
    }

    @Override
    public SendableDiscordMessage.Builder getFormat(OrDefault<DeathMessageConfig> config) {
        return config.get(cfg -> cfg.format);
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new DeathMessageForwardedEvent(cluster));
    }
}
