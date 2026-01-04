/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
    public @NotNull Task<ReceivedDiscordMessage> sendMessage(@NotNull SendableDiscordMessage message) {
        if (message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot send webhook messages to DMChannels");
        }

        MessageCreateAction action = channel.sendMessage(SendableDiscordMessageUtil.toJDASend(message));

        Long referencedMessageId = message.getMessageIdToReplyTo();
        if (referencedMessageId != null) {
            action = action.setMessageReference(referencedMessageId);
        }

        return discordSRV.discordAPI().toTask(action)
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }

    @Override
    public Task<Void> deleteMessageById(long id, boolean webhookMessage) {
        if (webhookMessage) {
            throw new IllegalArgumentException("DMChannels do not contain webhook messages");
        }

        return discordSRV.discordAPI().toTask(() -> channel.deleteMessageById(id));
    }

    @Override
    public @NotNull Task<ReceivedDiscordMessage> editMessageById(
            long id,
            @NotNull SendableDiscordMessage message
    ) {
        if (message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot send webhook messages to DMChannels");
        }

        return discordSRV.discordAPI()
                .toTask(channel.editMessageById(id, SendableDiscordMessageUtil.toJDAEdit(message)))
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
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
