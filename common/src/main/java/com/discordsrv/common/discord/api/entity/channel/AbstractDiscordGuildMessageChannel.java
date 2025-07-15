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

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageDeleteAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.api.utils.messages.MessageEditRequest;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDiscordGuildMessageChannel<T extends GuildMessageChannel>
        extends AbstractDiscordMessageChannel<T>
        implements DiscordGuildMessageChannel {

    private final DiscordGuild guild;

    public AbstractDiscordGuildMessageChannel(DiscordSRV discordSRV, T channel) {
        super(discordSRV, channel);
        this.guild = discordSRV.discordAPI().getGuild(channel.getGuild());
    }

    public Task<WebhookClient<Message>> queryWebhookClient() {
        return discordSRV.discordAPI().queryWebhookClient(getId());
    }

    @Override
    public @NotNull String getName() {
        return channel.getName();
    }

    @Override
    public FormattedText getAsMention() {
        return FormattedText.of(channel.getAsMention());
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
    public @NotNull Task<ReceivedDiscordMessage> sendMessage(@NotNull SendableDiscordMessage message) {
        return sendInternal(message);
    }

    protected <R> WebhookMessageCreateAction<R> mapAction(WebhookMessageCreateAction<R> action) {
        return action;
    }

    @SuppressWarnings("unchecked") // Generics
    private <R extends MessageCreateRequest<? extends MessageCreateRequest<?>> & RestAction<Message>> Task<ReceivedDiscordMessage> sendInternal(SendableDiscordMessage message) {
        MessageCreateData createData = SendableDiscordMessageUtil.toJDASend(message);

        Task<R> createRequest;
        if (message.isWebhookMessage()) {
            createRequest = queryWebhookClient()
                    .thenApply(client -> (R) mapAction(client.sendMessage(createData))
                            .setUsername(message.getWebhookUsername())
                            .setAvatarUrl(message.getWebhookAvatarUrl())
                    );
        } else {
            MessageCreateAction action = channel.sendMessage(createData);

            Long referencedMessageId = message.getMessageIdToReplyTo();
            if (referencedMessageId != null) {
                action = action.setMessageReference(referencedMessageId);
            }
            createRequest = Task.completed((R) action);
        }

        return createRequest
                .then(restAction -> discordSRV.discordAPI().toTask(restAction))
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }

    @Override
    public @NotNull Task<ReceivedDiscordMessage> editMessageById(
            long id,
            @NotNull SendableDiscordMessage message
    ) {
        return editInternal(id, message);
    }

    protected <R> WebhookMessageEditAction<R> mapAction(WebhookMessageEditAction<R> action) {
        return action;
    }

    @SuppressWarnings("unchecked") // Generics
    private <R extends MessageEditRequest<? extends MessageEditRequest<?>> & RestAction<Message>> Task<ReceivedDiscordMessage> editInternal(
            long id,
            SendableDiscordMessage message
    ) {
        MessageEditData editData = SendableDiscordMessageUtil.toJDAEdit(message);

        Task<R> editRequest;
        if (message.isWebhookMessage()) {
            editRequest = queryWebhookClient().thenApply(client -> (R) mapAction(client.editMessageById(id, editData)));
        } else {
            editRequest = Task.completed(((R) channel.editMessageById(id, editData)));
        }

        return editRequest
                .then(restAction -> discordSRV.discordAPI().toTask(restAction))
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }

    protected WebhookMessageDeleteAction mapAction(WebhookMessageDeleteAction action) {
        return action;
    }

    @Override
    public Task<Void> deleteMessageById(long id, boolean webhookMessage) {
        Task<Void> future;
        if (webhookMessage) {
            future = queryWebhookClient()
                    .then(client -> discordSRV.discordAPI().toTask(() -> mapAction(client.deleteMessageById(id))));
        } else {
            future = discordSRV.discordAPI().toTask(() -> channel.deleteMessageById(id));
        }
        return future;
    }

    @Override
    public Task<Void> delete() {
        return discordSRV.discordAPI().toTask(channel::delete);
    }
}
