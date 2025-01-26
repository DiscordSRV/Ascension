package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadContainer;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.channel.attribute.IPostContainer;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.requests.restaction.AbstractThreadCreateAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractDiscordForumChannel<PC extends IPostContainer> implements DiscordChannel, DiscordThreadContainer {

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
    public CompletableFuture<Void> delete() {
        return discordSRV.discordAPI().mapExceptions(() -> channel.delete().submit());
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

    @SuppressWarnings("CodeBlock2Expr")
    private CompletableFuture<List<DiscordThreadChannel>> threads(
            Function<IThreadContainer, ThreadChannelPaginationAction> action) {
        return discordSRV.discordAPI().mapExceptions(() -> {
            return action.apply(channel)
                    .submit()
                    .thenApply(channels -> channels.stream()
                            .map(channel -> discordSRV.discordAPI().getThreadChannel(channel))
                            .collect(Collectors.toList())
                    );
        });
    }

    @Override
    public CompletableFuture<DiscordThreadChannel> createThread(String name, boolean privateThread) {
        throw new IllegalStateException("Cannot create Threads in Forums without a message");
    }

    @Override
    public CompletableFuture<DiscordThreadChannel> createThread(String name, long messageId) {
        return thread(channel -> channel.createThreadChannel(name, messageId), result -> result);
    }

    @Override
    public IThreadContainer getAsJDAThreadContainer() {
        return channel;
    }

    @SuppressWarnings("CodeBlock2Expr")
    protected <R> CompletableFuture<DiscordThreadChannel> thread(
            Function<PC, AbstractThreadCreateAction<R, ?>> action,
            Function<R, ThreadChannel> resultMapper
    ) {
        return discordSRV.discordAPI().mapExceptions(() -> {
            return action.apply(channel)
                    .submit()
                    .thenApply(result -> discordSRV.discordAPI().getThreadChannel(resultMapper.apply(result)));
        });
    }

}
