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

package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.api.utils.messages.MessageEditRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractDiscordGuildMessageChannel<T extends GuildMessageChannel>
        extends AbstractDiscordMessageChannel<T>
        implements DiscordGuildMessageChannel {

    private final DiscordGuild guild;

    public AbstractDiscordGuildMessageChannel(DiscordSRV discordSRV, T channel) {
        super(discordSRV, channel);
        this.guild = discordSRV.discordAPI().getGuild(channel.getGuild());
    }

    public CompletableFuture<WebhookClient<Message>> queryWebhookClient() {
        return discordSRV.discordAPI().queryWebhookClient(getId());
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
    public @NotNull String getJumpUrl() {
        return channel.getJumpUrl();
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> sendMessage(@NotNull SendableDiscordMessage message) {
        return sendInternal(message);
    }

    @SuppressWarnings("unchecked") // Generics
    private <R extends MessageCreateRequest<? extends MessageCreateRequest<?>> & RestAction<Message>> CompletableFuture<ReceivedDiscordMessage> sendInternal(SendableDiscordMessage message) {
        MessageCreateData createData = SendableDiscordMessageUtil.toJDASend(message);

        CompletableFuture<R> createRequest;
        if (message.isWebhookMessage()) {
            createRequest = queryWebhookClient()
                    .thenApply(client -> (R) client.sendMessage(createData)
                            .setUsername(message.getWebhookUsername())
                            .setAvatarUrl(message.getWebhookAvatarUrl())
                    );
        } else {
            createRequest = CompletableFuture.completedFuture(((R) channel.sendMessage(createData)));
        }

        return createRequest
                .thenCompose(RestAction::submit)
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> editMessageById(
            long id,
            @NotNull SendableDiscordMessage message
    ) {
        return editInternal(id, message);
    }

    @SuppressWarnings("unchecked") // Generics
    private <R extends MessageEditRequest<? extends MessageEditRequest<?>> & RestAction<Message>> CompletableFuture<ReceivedDiscordMessage> editInternal(
            long id,
            SendableDiscordMessage message
    ) {
        MessageEditData editData = SendableDiscordMessageUtil.toJDAEdit(message);

        CompletableFuture<R> editRequest;
        if (message.isWebhookMessage()) {
            editRequest = queryWebhookClient().thenApply(client -> (R) client.editMessageById(id, editData));
        } else {
            editRequest = CompletableFuture.completedFuture(((R) channel.editMessageById(id, editData)));
        }

        return editRequest
                .thenCompose(RestAction::submit)
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }

    @Override
    public CompletableFuture<Void> deleteMessageById(long id, boolean webhookMessage) {
        CompletableFuture<Void> future;
        if (webhookMessage) {
            future = queryWebhookClient().thenCompose(client -> client.deleteMessageById(id).submit());
        } else {
            future = channel.deleteMessageById(id).submit();
        }
        return discordSRV.discordAPI().mapExceptions(future);
    }

}
