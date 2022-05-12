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

package com.discordsrv.common.discord.api;

import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ThreadChannelLookup {

    private final long channelId;
    private final boolean privateThreads;
    private final CompletableFuture<List<DiscordThreadChannel>> future;
    private final CompletableFuture<DiscordThreadChannel> channelFuture = new CompletableFuture<>();

    public ThreadChannelLookup(long channelId, boolean privateThreads, CompletableFuture<List<DiscordThreadChannel>> future) {
        this.channelId = channelId;
        this.privateThreads = privateThreads;
        this.future = future;
    }

    public long getChannelId() {
        return channelId;
    }

    public boolean isPrivateThreads() {
        return privateThreads;
    }

    public CompletableFuture<List<DiscordThreadChannel>> getFuture() {
        return future;
    }

    public CompletableFuture<DiscordThreadChannel> getChannelFuture() {
        return channelFuture;
    }
}
