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
import java.util.List;

@ConfigSerializable
public class ChannelUpdaterConfig {

    @Comment("The channel IDs.\n"
            + "The bot will need the \"View Channel\" and \"Manage Channels\" permissions, "
            + "additionally \"Connect\" is required for voice channels")
    public List<Long> channelIds = new ArrayList<>();

    @Comment("If this is blank, the name will not be updated")
    public String nameFormat = "";

    @Comment("If this is blank, the topic will not be updated. Unavailable for voice channels")
    public String topicFormat = "";

    @Comment("The time between updates in minutes. The minimum time is 10 minutes.")
    public int timeMinutes = 10;
}
