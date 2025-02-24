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

package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.Mentionable;
import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadContainer;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.channel.attribute.IPostContainer;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.AbstractThreadCreateAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractDiscordForumChannel<PC extends IPostContainer>
        implements DiscordChannel, DiscordThreadContainer, Mentionable {

    protected final DiscordSRV discordSRV;
    protected final PC channel;
    protected final DiscordGuild guild;

    public AbstractDiscordForumChannel(DiscordSRV discordSRV, PC channel) {
        this.discordSRV = discordSRV;
        this.channel = channel;
        this.guild = discordSRV.discordAPI().getGuild(channel.getGuild());
    }

    @Override
    public long getId() {
        return channel.getIdLong();
    }

    @Override
    public String getAsMention() {
        return channel.getAsMention();
    }

    @Override
    public @NotNull String getName() {
        return channel.getName();
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public @NotNull String getJumpUrl() {
        return channel.getJumpUrl();
    }

    @Override
    public Task<Void> delete() {
        return discordSRV.discordAPI().toTask(channel::delete);
    }

    @Override
    public @NotNull List<DiscordThreadChannel> getActiveThreads() {
        List<ThreadChannel> threads = channel.getThreadChannels();
        List<DiscordThreadChannel> threadChannels = new ArrayList<>(threads.size());
        for (ThreadChannel thread : threads) {
            threadChannels.add(discordSRV.discordAPI().getThreadChannel(thread));
        }
        return threadChannels;
    }

    @Override
    public Task<List<DiscordThreadChannel>> retrieveArchivedPrivateThreads() {
        return threads(IThreadContainer::retrieveArchivedPrivateThreadChannels);
    }

    @Override
    public Task<List<DiscordThreadChannel>> retrieveArchivedJoinedPrivateThreads() {
        return threads(IThreadContainer::retrieveArchivedPrivateJoinedThreadChannels);
    }

    @Override
    public Task<List<DiscordThreadChannel>> retrieveArchivedPublicThreads() {
        return threads(IThreadContainer::retrieveArchivedPublicThreadChannels);
    }

    private Task<List<DiscordThreadChannel>> threads(Function<IThreadContainer, ThreadChannelPaginationAction> action) {
        return discordSRV.discordAPI().toTask(() -> action.apply(channel))
                .thenApply(channels -> channels.stream()
                        .map(channel -> discordSRV.discordAPI().getThreadChannel(channel))
                        .collect(Collectors.toList())
                );
    }

    @Override
    public Task<DiscordThreadChannel> createThread(String name, boolean privateThread) {
        throw new IllegalStateException("Cannot create Threads in Forums without a message");
    }

    @Override
    public Task<DiscordThreadChannel> createThread(String name, long messageId) {
        return thread(channel -> channel.createThreadChannel(name, messageId), result -> result);
    }

    @Override
    public IThreadContainer getAsJDAThreadContainer() {
        return channel;
    }

    protected <R> Task<DiscordThreadChannel> thread(
            Function<PC, AbstractThreadCreateAction<R, ?>> action,
            Function<R, ThreadChannel> resultMapper
    ) {
        return discordSRV.discordAPI().toTask(() -> action.apply(channel))
                    .thenApply(result -> discordSRV.discordAPI().getThreadChannel(resultMapper.apply(result)));
    }

}
