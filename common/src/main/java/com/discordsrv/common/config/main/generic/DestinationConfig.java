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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.common.config.configurate.annotation.Constants;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;

@ConfigSerializable
public class DestinationConfig {

    @Setting("channel-ids")
    @Comment("The text, announcement, voice and/or stage channel ids this in-game channel will forward to in Discord\n"
            + "The bot needs to have the following permissions in these channels:\n"
            + "- \"View Channel\"\n"
            + "- \"Send Messages\" to non-webhook messages\n"
            + "- \"Manage Webhooks\" to send webhook messages"
    )
    public List<Long> channelIds = new ArrayList<>();

    @Setting("threads")
    @Comment("The threads that this in-game channel will forward to in Discord (this can be used with or instead of the %1 option)\n"
            + "The bot needs to have the following permissions in these channels:\n"
            + "- \"View Channel\"\n"
            + "- \"Create Public Threads\" to automatically create the thread (when \"%2\" is %3)\n"
            + "- \"Create Private Threads\" to automatically create the thread (when \"%2\" is %4)\n"
            + "- \"Manage Threads\" to send messages into a manually created private thread that wasn't created by the bot\n"
            + "- \"Send Messages in Threads\" to send non-webhook messages\n"
            + "- \"Manage Webhooks\" to send webhook messages"
    )
    @Constants.Comment({"channel-ids", "private-thread", "false", "true"})
    public List<ThreadConfig> threads = new ArrayList<>(Collections.singletonList(new ThreadConfig()));

    @ConfigSerializable
    public static class Single {
        @Setting("channel-id")
        @Comment("The text, announcement, voice or stage channel id (or text, announcement, forum or stage channel id if using a thread)\n"
                + "The bot needs to have the following permissions in these channels:\n"
                + "- \"View Channel\"\n"
                + "- \"Manage Webhooks\" to send webhook messages\n"
                + "When \"%1\" is %3:\n"
                + "- \"Send Messages\" to send non-webhook messages\n"
                + "When \"%1\" is %4:\n"
                + "- \"Create Public Threads\" to automatically create the thread (when \"%2\" is %3)\n"
                + "- \"Create Private Threads\" to automatically create the thread (when \"%2\" is %4)\n"
                + "- \"Manage Threads\" to send messages into a manually created private thread that wasn't created by the bot\n"
                + "- \"Send Messages in Threads\" to non-webhook messages in the thread"
        )
        @Constants.Comment({"use-thread", "private-thread", "false", "true"})
        public Long channelId = 0L;

        public boolean useThread = true;

        @Setting(nodeFromParent = true)
        public ThreadConfig thread = new ThreadConfig("");

        @SuppressWarnings("unused") // Configurate
        public Single() {}

        public Single(String threadName, boolean privateThread) {
            this.useThread = threadName != null && !threadName.isEmpty();
            this.thread.threadName = threadName;
            this.thread.privateThread = privateThread;
        }

        public DestinationConfig asDestination() {
            DestinationConfig config = new DestinationConfig();
            if (thread == null || StringUtils.isEmpty(thread.threadName) || !useThread) {
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
            Single other = (Single) o;
            return Objects.equals(channelId, other.channelId)
                    && Objects.equals(useThread, other.useThread)
                    && Objects.equals(thread, other.thread);
        }

        @Override
        public int hashCode() {
            return Objects.hash(channelId, useThread, thread);
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

    public boolean contains(DiscordGuild guild) {
        Set<Long> allChannelIds = new HashSet<>(channelIds.size() + threads.size());
        allChannelIds.addAll(channelIds);
        threads.forEach(thread -> allChannelIds.add(thread.channelId));

        for (DiscordGuildChannel channel : guild.getChannels()) {
            if (allChannelIds.contains(channel.getId())) {
                return true;
            }
        }
        return false;
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
