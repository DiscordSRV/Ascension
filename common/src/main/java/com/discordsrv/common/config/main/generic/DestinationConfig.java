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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.common.config.configurate.annotation.Constants;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ConfigSerializable
public class DestinationConfig {

    @Setting("channel-ids")
    @Comment("The text, announcement, voice and/or stage channel ids this in-game channel will forward to in Discord")
    public List<Long> channelIds = new ArrayList<>();

    @Setting("threads")
    @Comment("The threads that this in-game channel will forward to in Discord (this can be used with or instead of the %1 option)")
    @Constants.Comment("channel-ids")
    public List<ThreadConfig> threads = new ArrayList<>(Collections.singletonList(new ThreadConfig()));

    @ConfigSerializable
    public static class Single {
        @Setting("channel-id")
        public Long channelId = 0L;

        @Setting(nodeFromParent = true)
        public ThreadConfig thread = new ThreadConfig("");

        @SuppressWarnings("unused") // Configurate
        public Single() {}

        public Single(String threadName, boolean privateThread) {
            this.thread.threadName = threadName;
            this.thread.privateThread = privateThread;
        }

        public DestinationConfig asDestination() {
            DestinationConfig config = new DestinationConfig();
            if (thread == null || StringUtils.isEmpty(thread.threadName)) {
                config.channelIds.add(channelId);
            } else {
                config.threads.add(thread);
            }
            return config;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Single single = (Single) o;
            return Objects.equals(channelId, single.channelId) && Objects.equals(thread, single.thread);
        }

        @Override
        public int hashCode() {
            return Objects.hash(channelId, thread);
        }
    }

    public boolean contains(DiscordChannel channel) {
        if (channel instanceof DiscordThreadChannel) {
            long parentId = ((DiscordThreadChannel) channel).getParentChannel().getId();
            String threadName = ((DiscordThreadChannel) channel).getName();
            for (ThreadConfig thread : threads) {
                if (thread.channelId == parentId && thread.threadName.equals(threadName)) {
                    return true;
                }
            }
            return false;
        }

        return channelIds.contains(channel.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DestinationConfig that = (DestinationConfig) o;
        return Objects.equals(channelIds, that.channelIds) && Objects.equals(threads, that.threads);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelIds, threads);
    }
}
