/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.common.config.configurate.annotation.Constants;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class DestinationConfig {

    @Setting("channel-ids")
    @Comment("The text and/or voice channel ids this in-game channel will forward to in Discord")
    public List<Long> channelIds = new ArrayList<>();

    @Setting("threads")
    @Comment("The threads that this in-game channel will forward to in Discord (this can be used instead of or with the %1 option)")
    @Constants.Comment("channel-ids")
    public List<ThreadConfig> threads = new ArrayList<>(Collections.singletonList(new ThreadConfig()));

    @ConfigSerializable
    public static class Single {
        @Setting("channel-id")
        public Long channelId = 0L;

        @Setting("thread-name")
        @Comment("If specified this destination will be a thread in the provided channel-id's channel, if left blank the destination will be the channel")
        public String threadName = "";
        public boolean privateThread = false;

        public DestinationConfig asDestination() {
            DestinationConfig config = new DestinationConfig();
            if (StringUtils.isEmpty(threadName)) {
                config.channelIds.add(channelId);
            } else {
                ThreadConfig threadConfig = new ThreadConfig();
                threadConfig.channelId = channelId;
                threadConfig.threadName = threadName;
                threadConfig.privateThread = privateThread;
                config.threads.add(threadConfig);
            }
            return config;
        }
    }
}
