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

package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class MemberCachingConfig {

    @Comment("If members should be cached at all\n"
            + "Requires the \"Server Members Intent\"")
    public boolean enabled = true;

    @Comment("If members should be cached at startup\n"
            + "Requires the \"Server Members Intent\"")
    public boolean chunk = true;

    @Comment("Amount of users to keep in cache based on least-recently-used basis per Discord server\n"
            + "Set to 0 to disable (always keep all in cache)")
    public int lru = 5_000;

    @Comment("Filter for which servers should be cached at startup")
    public GuildFilter chunkingServerFilter = new GuildFilter();

    @ConfigSerializable
    public static class GuildFilter {

        @Comment("If the ids option acts as a blacklist, otherwise it is a whitelist")
        public boolean blacklist = true;
        public List<Long> ids = new ArrayList<>();
    }
}
