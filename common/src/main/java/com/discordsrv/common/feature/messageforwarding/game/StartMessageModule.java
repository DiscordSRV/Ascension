/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.events.message.post.game.AbstractGameMessagePostEvent;
import com.discordsrv.api.events.message.postprocess.game.AbstractGameMessagePostProcessEvent;
import com.discordsrv.api.events.message.preprocess.game.AbstractGameMessagePreProcessEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.StartMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;

import java.util.List;

public class StartMessageModule extends AbstractGameMessageModule<StartMessageConfig, AbstractGameMessagePreProcessEvent, AbstractGameMessagePostProcessEvent<AbstractGameMessagePreProcessEvent>> {

    public StartMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "START_MESSAGE");
    }

    @Override
    public StartMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.startMessage;
    }

    @Override
    protected AbstractGameMessagePostProcessEvent<AbstractGameMessagePreProcessEvent> createPostProcessEvent(
            AbstractGameMessagePreProcessEvent preEvent,
            IPlayer player,
            List<DiscordGuildMessageChannel> channels,
            SendableDiscordMessage discordMessage
    ) {
        return null;
    }

    @Override
    protected AbstractGameMessagePostEvent<AbstractGameMessagePostProcessEvent<AbstractGameMessagePreProcessEvent>> createPostEvent(
            AbstractGameMessagePostProcessEvent<AbstractGameMessagePreProcessEvent> preEvent,
            ReceivedDiscordMessageCluster cluster
    ) {
        return null;
    }

    @Override
    public void setPlaceholders(StartMessageConfig config, AbstractGameMessagePreProcessEvent event, SendableDiscordMessage.Formatter formatter) {}

    @Override
    public void serverStarted() {
        process(null, null, null);
    }
}
