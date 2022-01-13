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

package com.discordsrv.proxy.config.channels.base;

import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.channels.base.ThreadConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public class ProxyChannelConfig extends ProxyBaseChannelConfig implements IChannelConfig {

    public ProxyChannelConfig() {
        initialize();
    }

    @Setting(CHANNEL_IDS_OPTION_NAME)
    @Comment(CHANNEL_IDS_COMMENT)
    public List<Long> channelIds = CHANNEL_IDS_VALUE;

    @Override
    public List<Long> channelIds() {
        return channelIds;
    }

    @Setting(THREADS_OPTION_NAME)
    @Comment(THREADS_COMMENT)
    public List<ThreadConfig> threads = THREADS_VALUE;

    @Override
    public List<ThreadConfig> threads() {
        return threads;
    }
}
