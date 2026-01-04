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
import com.discordsrv.api.events.message.post.game.DeathMessagePostEvent;
import com.discordsrv.api.events.message.postprocess.game.DeathMessagePostProcessEvent;
import com.discordsrv.api.events.message.preprocess.game.DeathMessagePreProcessEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.channels.server.DeathMessageConfig;

import java.util.List;

public class DeathMessageModule extends AbstractGameMessageModule<DeathMessageConfig, DeathMessagePreProcessEvent, DeathMessagePostProcessEvent> {

    public DeathMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "DEATH_MESSAGES");
    }

    @Subscribe(priority = EventPriorities.LAST, ignoreCancelled = false, ignoreProcessed = false)
    public void onDeathMessageReceive(DeathMessagePreProcessEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        discordSRV.scheduler().run(() -> process(event, event.getPlayer(), event.getGameChannel()));
        event.markAsProcessed();
    }

    @Override
    public DeathMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return ((ServerBaseChannelConfig) channelConfig).deathMessages;
    }

    @Override
    protected DeathMessagePostProcessEvent createPostProcessEvent(
            DeathMessagePreProcessEvent preEvent,
            IPlayer player,
            List<DiscordGuildMessageChannel> channels,
            SendableDiscordMessage discordMessage
    ) {
        return new DeathMessagePostProcessEvent(preEvent, player, channels, discordMessage);
    }

    @Override
    protected AbstractGameMessagePostEvent<DeathMessagePostProcessEvent> createPostEvent(
            DeathMessagePostProcessEvent preEvent,
            ReceivedDiscordMessageCluster cluster
    ) {
        return new DeathMessagePostEvent(preEvent, cluster);
    }

    @Override
    public void setPlaceholders(
            DeathMessageConfig config,
            DeathMessagePreProcessEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        formatter.addPlaceholder("message", event.getMessage());
    }

}
