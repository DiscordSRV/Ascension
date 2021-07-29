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

package com.discordsrv.common.discord.api;

import club.minnced.discord.webhook.WebhookClient;
import com.discordsrv.api.discord.api.DiscordAPI;
import com.discordsrv.api.discord.api.channel.DiscordTextChannel;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.channel.DiscordTextChannelImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DiscordAPIImpl implements DiscordAPI {

    private final DiscordSRV discordSRV;
    private final Map<String, WebhookClient> configuredClients = new HashMap<>();
    private final Cache<String, WebhookClient> cachedClients = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .removalListener((RemovalListener<String, WebhookClient>) (id, client, cause) -> {
                if (client != null) {
                    client.close();
                }
            })
            .build();

    public DiscordAPIImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public WebhookClient getWebhookClient(String channelId) {
        WebhookClient client = configuredClients.get(channelId);
        if (client != null) {
            return client;
        }

        return cachedClients.getIfPresent(channelId);
    }

    @Override
    public DiscordTextChannel getTextChannelById(String id) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return null;
        }

        TextChannel textChannel = jda.getTextChannelById(id);
        return textChannel != null ? new DiscordTextChannelImpl(discordSRV, textChannel) : null;
    }

}
