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

package com.discordsrv.common.feature;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.preprocess.game.*;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.channel.global.GlobalChannel;

public class WorldChannelModule extends AbstractModule<DiscordSRV> {

    public WorldChannelModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "WORLDCHANNEL"));
    }

    @Subscribe(priority = EventPriorities.EARLIEST)
    public void onGameMessagePreProcess(AbstractGameMessagePreProcessEvent event) {
        if (event.getGameChannel() != null && !event.getGameChannel().getChannelName().equals(GlobalChannel.DEFAULT_NAME)) {
            return;
        }

        String ownerName = event.getPlayer().world().namespace();
        String worldName = event.getPlayer().world().value();

        GameChannel worldChannel = discordSRV.channelConfig().resolveChannel(ownerName, worldName);
        if (worldChannel == null) return;

        ServerBaseChannelConfig channelConfig = (ServerBaseChannelConfig) discordSRV.channelConfig().get(worldChannel);
        if (channelConfig == null) return;

        IMessageConfig messageConfig = messageConfigForEvent(channelConfig, event);
        if (messageConfig != null && !messageConfig.enabled()) {
            logger().debug("Not routing " + event.getClass().getSimpleName() + " to world channel " + worldChannel + " because message config is missing or message type is disabled for world \"" + worldName + "\"");
            return;
        }

        logger().debug("Routing " + event.getClass().getSimpleName() + " destined for channel "
                + event.getGameChannel() + " to world-specific channel " + worldChannel
                + " from player " + event.getPlayer() + " in world \"" + worldName + "\"");
        event.setGameChannel(worldChannel);
    }

    private IMessageConfig messageConfigForEvent(ServerBaseChannelConfig channelConfig, AbstractGameMessagePreProcessEvent event) {
        if (event instanceof GameChatMessagePreProcessEvent) return channelConfig.minecraftToDiscord;
        if (event instanceof JoinMessagePreProcessEvent) return channelConfig.joinMessages().getForEvent((JoinMessagePreProcessEvent) event);
        if (event instanceof LeaveMessagePreProcessEvent) return channelConfig.leaveMessages;
        if (event instanceof DeathMessagePreProcessEvent) return channelConfig.deathMessages;
        if (event instanceof AwardMessagePreProcessEvent) return channelConfig.awardMessages;

        return null;
    }
}
