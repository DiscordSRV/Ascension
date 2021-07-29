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

package com.discordsrv.common.discord.api.message;

import com.discordsrv.api.discord.api.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.message.SendableDiscordMessage;
import com.discordsrv.api.discord.api.message.impl.SendableDiscordMessageImpl;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReceivedDiscordMessageImpl extends SendableDiscordMessageImpl implements ReceivedDiscordMessage {

    private static List<DiscordMessageEmbed> mapEmbeds(List<MessageEmbed> embeds) {
        List<DiscordMessageEmbed> mappedEmbeds = new ArrayList<>();
        for (MessageEmbed embed : embeds) {
            mappedEmbeds.add(new DiscordMessageEmbed(embed));
        }
        return mappedEmbeds;
    }

    private static String getWebhookUsername(Message message) {
        if (!message.isWebhookMessage()) {
            return null;
        }

        return message.getAuthor().getName();
    }

    private static String getWebhookAvatarUrl(Message message) {
        if (!message.isWebhookMessage()) {
            return null;
        }

        return message.getAuthor().getEffectiveAvatarUrl();
    }

    private final DiscordSRV discordSRV;
    private final Message message;

    public ReceivedDiscordMessageImpl(DiscordSRV discordSRV, Message message) {
        super(
                message.getContentRaw(),
                mapEmbeds(message.getEmbeds()),
                getWebhookUsername(message),
                getWebhookAvatarUrl(message)
        );
        this.discordSRV = discordSRV;
        this.message = message;
    }

    @Override
    public String getId() {
        return message.getId();
    }

    @Override
    public String getDisplayedContent() {
        return message.getContentDisplay();
    }

    @Override
    public String getStrippedContent() {
        return message.getContentStripped();
    }

    @Override
    public CompletableFuture<ReceivedDiscordMessage> edit(SendableDiscordMessage message) {
        DiscordTextChannel textChannel = discordSRV.discordAPI().getTextChannelById(this.message.getChannel().getId());
        return textChannel.editMessageById(textChannel.getId(), message);
    }
}
