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

import com.discordsrv.api.discord.api.entity.DiscordUser;
import com.discordsrv.api.discord.api.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.DiscordUserImpl;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.PrivateChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DiscordDMChannelImpl extends DiscordMessageChannelImpl<PrivateChannel> implements DiscordDMChannel {

    private final DiscordUser user;

    public DiscordDMChannelImpl(DiscordSRV discordSRV, PrivateChannel privateChannel) {
        super(discordSRV, privateChannel);
        this.user = new DiscordUserImpl(discordSRV, privateChannel.getUser());
    }

    @Override
    public DiscordUser getUser() {
        return user;
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> sendMessage(@NotNull SendableDiscordMessage message) {
        if (message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot send webhook messages to DMChannels");
        }

        CompletableFuture<ReceivedDiscordMessage> future = channel
                .sendMessage(SendableDiscordMessageUtil.toJDA(message))
                .submit()
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
                .editMessageById(id, SendableDiscordMessageUtil.toJDA(message))
                .submit()
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));

        return discordSRV.discordAPI().mapExceptions(future);
    }

    @Override
    public PrivateChannel getAsJDAPrivateChannel() {
        return channel;
    }
}
