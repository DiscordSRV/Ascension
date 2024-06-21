package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import com.discordsrv.api.discord.entity.channel.DiscordForumChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.requests.restaction.AbstractThreadCreateAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscordForumChannelImpl implements DiscordForumChannel {

    private final DiscordSRV discordSRV;
    private final ForumChannel channel;
    private final DiscordGuild guild;

    public DiscordForumChannelImpl(DiscordSRV discordSRV, ForumChannel channel) {
        this.discordSRV = discordSRV;
        this.channel = channel;
        this.guild = discordSRV.discordAPI().getGuild(channel.getGuild());
    }

    @Override
    public ForumChannel asJDA() {
        return channel;
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

    @Override
    public CompletableFuture<DiscordThreadChannel> createPost(String name, SendableDiscordMessage message) {
        return thread(
                channel -> channel.createForumPost(name, SendableDiscordMessageUtil.toJDASend(message)),
                ForumPost::getThreadChannel
        );
    }

    @SuppressWarnings("CodeBlock2Expr")
    private <R> CompletableFuture<DiscordThreadChannel> thread(
            Function<ForumChannel, AbstractThreadCreateAction<R, ?>> action,
            Function<R, ThreadChannel> resultMapper
    ) {
        return discordSRV.discordAPI().mapExceptions(() -> {
            return action.apply(channel)
                    .submit()
                    .thenApply(result -> discordSRV.discordAPI().getThreadChannel(resultMapper.apply(result)));
        });
    }

    @Override
    public DiscordChannelType getType() {
        return DiscordChannelType.FORUM;
    }

    @Override
    public String toString() {
        return "Forum:" + getName() + "(" + Long.toUnsignedString(getId()) + ")";
    }
}
