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

package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.api.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordThreadContainer;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.IThreadContainer;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AbstractDiscordThreadedGuildMessageChannel<T extends GuildMessageChannel & IThreadContainer>
        extends AbstractDiscordGuildMessageChannel<T>
        implements DiscordGuildMessageChannel, DiscordThreadContainer {

    public AbstractDiscordThreadedGuildMessageChannel(DiscordSRV discordSRV, T channel) {
        super(discordSRV, channel);
    }

    @Override
    public @NotNull List<DiscordThreadChannel> getActiveThreads() {
        List<ThreadChannel> threads = channel.getThreadChannels();
        List<DiscordThreadChannel> threadChannels = new ArrayList<>(threads.size());
        for (ThreadChannel thread : threads) {
            threadChannels.add(new DiscordThreadChannelImpl(discordSRV, thread));
        }
        return threadChannels;
    }

    @Override
    public CompletableFuture<List<DiscordThreadChannel>> retrieveArchivedPrivateThreads() {
        return threads(IThreadContainer::retrieveArchivedPrivateThreadChannels);
    }

    @Override
    public CompletableFuture<List<DiscordThreadChannel>> retrieveArchivedJoinedPrivateThreads() {
        return threads(IThreadContainer::retrieveArchivedPrivateJoinedThreadChannels);
    }

    @Override
    public CompletableFuture<List<DiscordThreadChannel>> retrieveArchivedPublicThreads() {
        return threads(IThreadContainer::retrieveArchivedPublicThreadChannels);
    }

    private CompletableFuture<List<DiscordThreadChannel>> threads(
            Function<IThreadContainer, ThreadChannelPaginationAction> action) {
        return discordSRV.discordAPI().mapExceptions(() ->
                                                             action.apply(channel)
                                                                     .submit()
                                                                     .thenApply(channels -> channels.stream()
                                                                             .map(channel -> new DiscordThreadChannelImpl(discordSRV, channel))
                                                                             .collect(Collectors.toList())
                                                                     )
        );
    }

    @Override
    public CompletableFuture<DiscordThreadChannel> createThread(String name, boolean privateThread) {
        return thread(channel -> channel.createThreadChannel(name, privateThread));
    }

    @Override
    public CompletableFuture<DiscordThreadChannel> createThread(String name, long messageId) {
        return thread(channel -> channel.createThreadChannel(name, messageId));
    }

    private CompletableFuture<DiscordThreadChannel> thread(Function<T, ThreadChannelAction> action) {
        return discordSRV.discordAPI().mapExceptions(() ->
                                                             action.apply(channel)
                                                                     .submit()
                                                                     .thenApply(channel -> new DiscordThreadChannelImpl(discordSRV, channel))
        );
    }

}
