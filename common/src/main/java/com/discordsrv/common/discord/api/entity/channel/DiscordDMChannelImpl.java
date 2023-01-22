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

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DiscordDMChannelImpl extends AbstractDiscordMessageChannel<PrivateChannel> implements DiscordDMChannel {

    private final DiscordUser user;

    public DiscordDMChannelImpl(DiscordSRV discordSRV, PrivateChannel privateChannel) {
        super(discordSRV, privateChannel);
        User user = privateChannel.getUser();
        this.user = user != null ? discordSRV.discordAPI().getUser(user) : null;
    }

    @Override
    public DiscordUser getUser() {
        return user;
    }

    @Override
    public CompletableFuture<ReceivedDiscordMessage> sendMessage(
            @NotNull SendableDiscordMessage message,
            @NotNull Map<String, InputStream> attachments
    ) {
        if (message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot send webhook messages to DMChannels");
        }

        MessageCreateAction action = channel.sendMessage(SendableDiscordMessageUtil.toJDASend(message));
        for (Map.Entry<String, InputStream> entry : attachments.entrySet()) {
            action = action.addFiles(FileUpload.fromData(entry.getValue(), entry.getKey()));
        }

        CompletableFuture<ReceivedDiscordMessage> future = action.submit()
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));

        return discordSRV.discordAPI().mapExceptions(future);
    }

    @Override
    public CompletableFuture<Void> deleteMessageById(long id, boolean webhookMessage) {
        if (webhookMessage) {
            throw new IllegalArgumentException("DMChannels do not contain webhook messages");
        }
        return discordSRV.discordAPI().mapExceptions(channel.deleteMessageById(id).submit());
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> editMessageById(long id, @NotNull SendableDiscordMessage message) {
        if (message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot send webhook messages to DMChannels");
        }

        CompletableFuture<ReceivedDiscordMessage> future = channel
                .editMessageById(id, SendableDiscordMessageUtil.toJDAEdit(message))
                .submit()
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));

        return discordSRV.discordAPI().mapExceptions(future);
    }

    @Override
    public String toString() {
        return "DMChannel:" + user + "(" + Long.toUnsignedString(getId()) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscordDMChannelImpl that = (DiscordDMChannelImpl) o;
        return Objects.equals(user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(user.getId());
    }

    @Override
    public PrivateChannel asJDA() {
        return channel;
    }
}
