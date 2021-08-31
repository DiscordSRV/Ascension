/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.channel;

import com.discordsrv.api.discord.api.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.api.entity.user.DiscordUser;
import com.discordsrv.api.discord.api.exception.NotReadyException;
import com.discordsrv.api.discord.api.exception.UnknownChannelException;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.message.util.SendableDiscordMessageUtil;
import com.discordsrv.common.discord.api.user.DiscordUserImpl;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.PrivateChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DiscordDMChannelImpl extends DiscordMessageChannelImpl implements DiscordDMChannel {

    private final DiscordSRV discordSRV;
    private final String id;
    private final DiscordUser user;

    public DiscordDMChannelImpl(DiscordSRV discordSRV, PrivateChannel privateChannel) {
        this.discordSRV = discordSRV;
        this.id = privateChannel.getId();
        this.user = new DiscordUserImpl(privateChannel.getUser());
    }

    private PrivateChannel privateChannel() {
        JDA jda = discordSRV.jda().orElse(null);
        if (jda == null) {
            throw new NotReadyException();
        }

        PrivateChannel privateChannel = jda.getPrivateChannelById(id);
        if (privateChannel == null) {
            throw new UnknownChannelException();
        }

        return privateChannel;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public DiscordUser getUser() {
        return user;
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> sendMessage(SendableDiscordMessage message) {
        if (message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot send webhook messages to DMChannels");
        }

        CompletableFuture<ReceivedDiscordMessage> future = privateChannel()
                .sendMessage(SendableDiscordMessageUtil.toJDA(message))
                .submit()
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
        return mapExceptions(future);
    }

    @Override
    public CompletableFuture<Void> deleteMessageById(String id) {
        CompletableFuture<Void> future = privateChannel()
                .deleteMessageById(id)
                .submit();
        return mapExceptions(future);
    }

    @Override
    public @NotNull CompletableFuture<ReceivedDiscordMessage> editMessageById(String id, SendableDiscordMessage message) {
        if (message.isWebhookMessage()) {
            throw new IllegalArgumentException("Cannot send webhook messages to DMChannels");
        }

        CompletableFuture<ReceivedDiscordMessage> future = privateChannel()
                .editMessageById(id, SendableDiscordMessageUtil.toJDA(message))
                .submit()
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
        return mapExceptions(future);
    }
}
