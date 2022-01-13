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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessage;
import com.discordsrv.api.discord.api.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildImpl;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DiscordGuildMessageChannelImpl<T extends GuildMessageChannel & IThreadContainer>
        extends DiscordMessageChannelImpl<T>
        implements DiscordGuildMessageChannel {

    private final DiscordGuild guild;

    public DiscordGuildMessageChannelImpl(DiscordSRV discordSRV, T channel) {
        super(discordSRV, channel);
        this.guild = new DiscordGuildImpl(discordSRV, channel.getGuild());
    }

    @Override
    public @NotNull String getName() {
        return channel.getName();
    }

    @Override
    public String getAsMention() {
        return channel.getAsMention();
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
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

    private CompletableFuture<List<DiscordThreadChannel>> threads(Function<IThreadContainer, ThreadChannelPaginationAction> action) {
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

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> sendMessage(@NotNull SendableDiscordMessage message) {
        return message(message, WebhookClient::send, MessageChannel::sendMessage);
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> editMessageById(long id, @NotNull SendableDiscordMessage message) {
        return message(
                message,
                (client, msg) -> client.edit(id, msg),
                (textChannel, msg) -> textChannel.editMessageById(id, msg)
        );
    }

    private CompletableFuture<ReceivedDiscordMessage> message(
            SendableDiscordMessage message,
            BiFunction<WebhookClient, WebhookMessage, CompletableFuture<ReadonlyMessage>> webhookFunction,
            BiFunction<T, Message, MessageAction> jdaFunction) {
        return discordSRV.discordAPI().mapExceptions(() -> {
            CompletableFuture<ReceivedDiscordMessage> future;
            if (message.isWebhookMessage()) {
                future = discordSRV.discordAPI().queryWebhookClient(getId())
                        .thenCompose(client -> webhookFunction.apply(
                                client, SendableDiscordMessageUtil.toWebhook(message)))
                        .thenApply(msg -> ReceivedDiscordMessageImpl.fromWebhook(discordSRV, msg));
            } else {
                future = jdaFunction
                        .apply(channel, SendableDiscordMessageUtil.toJDA(message))
                        .submit()
                        .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
            }
            return future;
        });
    }

    @Override
    public CompletableFuture<Void> deleteMessageById(long id, boolean webhookMessage) {
        CompletableFuture<Void> future;
        if (webhookMessage) {
            future = channel.deleteMessageById(id).submit();
        } else {
            future = discordSRV.discordAPI()
                    .queryWebhookClient(channel.getIdLong())
                    .thenCompose(client -> client.delete(id));
        }
        return discordSRV.discordAPI().mapExceptions(future);
    }

}
