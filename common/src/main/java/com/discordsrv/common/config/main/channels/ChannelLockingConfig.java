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

package com.discordsrv.common.config.main.channels;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class ChannelLockingConfig {

    public Channels channels = new Channels();
    public Threads threads = new Threads();

    @ConfigSerializable
    public static class Channels {

        @Comment("If the permissions should be taken from @everyone while the server is offline")
        public boolean everyone = false;

        @Comment("Role ids for roles that should have the permissions taken while the server is offline")
        public List<Long> roleIds = new ArrayList<>();

        public boolean read = false;
        public boolean write = true;
        public boolean addReactions = true;

    }

    @ConfigSerializable
    public static class Threads {

        @Comment("If threads should be archived while the server is shutdown")
        public boolean archive = true;

        @Comment("If the bot will attempt to unarchive threads rather than make new threads")
        public boolean unarchive = true;

    }

}
