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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.FluentRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public abstract class AbstractDiscordGuildMessageChannel<T extends GuildMessageChannel>
        extends AbstractDiscordMessageChannel<T>
        implements DiscordGuildMessageChannel {

    private final DiscordGuild guild;

    public AbstractDiscordGuildMessageChannel(DiscordSRV discordSRV, T channel) {
        super(discordSRV, channel);
        this.guild = discordSRV.discordAPI().getGuild(channel.getGuild());
    }

    public CompletableFuture<WebhookClient> queryWebhookClient() {
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
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public CompletableFuture<ReceivedDiscordMessage> sendMessage(
            @NotNull SendableDiscordMessage message, @NotNull Map<String, InputStream> attachments
    ) {
        return message(message, (webhookClient, webhookMessage) -> {
            for (Map.Entry<String, InputStream> entry : attachments.entrySet()) {
                webhookMessage.addFile(entry.getKey(), entry.getValue());
            }
            return webhookClient.send(webhookMessage.build());
        }, (channel, msg) -> {
            MessageCreateAction action = channel.sendMessage(SendableDiscordMessageUtil.toJDASend(msg));
            for (Map.Entry<String, InputStream> entry : attachments.entrySet()) {
                action = action.addFiles(FileUpload.fromData(entry.getValue(), entry.getKey()));
            }
            return action;
        });
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> editMessageById(long id, @NotNull SendableDiscordMessage message) {
        return message(
                message,
                (client, msg) -> client.edit(id, msg.build()),
                (textChannel, msg) -> textChannel.editMessageById(id, SendableDiscordMessageUtil.toJDAEdit(msg))
        );
    }

    private CompletableFuture<ReceivedDiscordMessage> message(
            SendableDiscordMessage message,
            BiFunction<WebhookClient, WebhookMessageBuilder, CompletableFuture<ReadonlyMessage>> webhookFunction,
            BiFunction<T, SendableDiscordMessage, FluentRestAction<? extends Message, ?>> jdaFunction) {
        return discordSRV.discordAPI().mapExceptions(() -> {
            CompletableFuture<ReceivedDiscordMessage> future;
            if (message.isWebhookMessage()) {
                future = queryWebhookClient()
                        .thenCompose(client -> webhookFunction.apply(
                                client, SendableDiscordMessageUtil.toWebhook(message)))
                        .thenApply(msg -> ReceivedDiscordMessageImpl.fromWebhook(discordSRV, msg));
            } else {
                future = jdaFunction
                        .apply(channel, message)
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
