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

import com.discordsrv.api.discord.api.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.concurrent.CompletableFuture;

public class DiscordTextChannelImpl implements DiscordTextChannel {

    private final DiscordSRV discordSRV;
    private final TextChannel textChannel;

    public DiscordTextChannelImpl(DiscordSRV discordSRV, TextChannel textChannel) {
        this.discordSRV = discordSRV;
        this.textChannel = textChannel;
    }

    @Override
    public String getId() {
        return textChannel.getId();
    }

    @Override
    public long getIdLong() {
        return textChannel.getIdLong();
    }

    @Override
    public String getName() {
        return textChannel.getName();
    }

    @Override
    public String getTopic() {
        return textChannel.getTopic();
    }

    @Override
    public CompletableFuture<ReceivedDiscordMessage> sendMessage(SendableDiscordMessage message) {
        // TODO
        if (message.isWebhookMessage()) {

        } else {

        }
        return null;
    }

    @Override
    public CompletableFuture<ReceivedDiscordMessage> editMessageById(String id, SendableDiscordMessage message) {
        // TODO
        if (message.isWebhookMessage()) {

        } else {

        }
        return null;
    }
}
