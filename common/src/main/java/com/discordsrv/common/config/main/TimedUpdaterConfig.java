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

package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ConfigSerializable
public class TimedUpdaterConfig {

    public List<VoiceChannelConfig> voiceChannels = new ArrayList<>(Collections.singletonList(new VoiceChannelConfig()));
    public List<TextChannelConfig> textChannels = new ArrayList<>(Collections.singletonList(new TextChannelConfig()));

    public List<UpdaterConfig> getConfigs() {
        List<UpdaterConfig> configs = new ArrayList<>();
        configs.addAll(voiceChannels);
        configs.addAll(textChannels);
        return configs;
    }

    public interface UpdaterConfig {

        boolean any();
        long timeSeconds();
        long minimumSeconds();

    }

    public static class VoiceChannelConfig implements UpdaterConfig {

        @Comment("The channel IDs.\n"
                + "The bot will need the \"View Channel\", \"Manage Channels\" and \"Connection\" permissions for the provided channels")
        public List<Long> channelIds = new ArrayList<>();

        @Comment("The format for the channel name(s), placeholders are supported.")
        public String nameFormat = "";

        @Comment("The time between updates in minutes. The minimum time is 10 minutes.")
        public int timeMinutes = 10;

        @Override
        public boolean any() {
            return !channelIds.isEmpty();
        }

        @Override
        public long timeSeconds() {
            return TimeUnit.MINUTES.toSeconds(timeMinutes);
        }

        @Override
        public long minimumSeconds() {
            return TimeUnit.MINUTES.toSeconds(10);
        }
    }

    public static class TextChannelConfig implements UpdaterConfig {

        @Comment("The channel IDs.\n"
                + "The bot will need the \"View Channel\" and \"Manage Channels\" permissions for the provided channels")
        public List<Long> channelIds = new ArrayList<>();

        @Comment("The format for the channel name(s), placeholders are supported.\n"
                + "If this is blank, the name will not be updated")
        public String nameFormat = "";

        @Comment("The format for the channel topic(s), placeholders are supported.\n"
                + "If this is blank, the topic will not be updated")
        public String topicFormat = "";

        @Comment("The time between updates in minutes. The minimum time is 10 minutes.")
        public int timeMinutes = 10;

        @Override
        public boolean any() {
            return !channelIds.isEmpty();
        }

        @Override
        public long timeSeconds() {
            return TimeUnit.MINUTES.toSeconds(timeMinutes);
        }

        @Override
        public long minimumSeconds() {
            return TimeUnit.MINUTES.toSeconds(10);
        }
    }

}
