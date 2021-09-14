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
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.discordsrv.api.discord.api.DiscordAPI;
import com.discordsrv.api.discord.api.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.api.entity.user.DiscordUser;
import com.discordsrv.api.discord.api.exception.NotReadyException;
import com.discordsrv.api.discord.api.exception.UnknownChannelException;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.ChannelConfig;
import com.discordsrv.common.discord.api.channel.DiscordDMChannelImpl;
import com.discordsrv.common.discord.api.channel.DiscordTextChannelImpl;
import com.discordsrv.common.discord.api.guild.DiscordGuildImpl;
import com.discordsrv.common.discord.api.user.DiscordUserImpl;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class DiscordAPIImpl implements DiscordAPI {

    private final DiscordSRV discordSRV;

    private final AsyncLoadingCache<Long, WebhookClient> cachedClients;

    public DiscordAPIImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.cachedClients = discordSRV.caffeineBuilder()
                .removalListener((RemovalListener<Long, WebhookClient>) (id, client, cause) -> {
                    if (client != null) {
                        client.close();
                    }
                })
                .expireAfter(new WebhookCacheExpiry())
                .buildAsync(new WebhookCacheLoader());
    }

    public CompletableFuture<WebhookClient> queryWebhookClient(long channelId) {
        return cachedClients.get(channelId);
    }

    public AsyncLoadingCache<Long, WebhookClient> getCachedClients() {
        return cachedClients;
    }

    @Override
    public @NotNull Optional<? extends DiscordMessageChannel> getMessageChannelById(long id) {
        Optional<DiscordTextChannel> textChannel = getTextChannelById(id);
        if (textChannel.isPresent()) {
            return textChannel;
        }

        return getDirectMessageChannelById(id);
    }

    @Override
    public @NotNull Optional<DiscordDMChannel> getDirectMessageChannelById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getPrivateChannelById(id))
                .map(privateChannel -> new DiscordDMChannelImpl(discordSRV, privateChannel));
    }

    @Override
    public @NotNull Optional<DiscordTextChannel> getTextChannelById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getTextChannelById(id))
                .map(textChannel -> new DiscordTextChannelImpl(discordSRV, textChannel));
    }

    @Override
    public @NotNull Optional<DiscordGuild> getGuildById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getGuildById(id))
                .map(guild -> new DiscordGuildImpl(discordSRV, guild));
    }

    @Override
    public @NotNull Optional<DiscordUser> getUserById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getUserById(id))
                .map(DiscordUserImpl::new);
    }

    private class WebhookCacheLoader implements AsyncCacheLoader<Long, WebhookClient> {

        @Override
        public @NonNull CompletableFuture<WebhookClient> asyncLoad(@NonNull Long channelId, @NonNull Executor executor) {
            CompletableFuture<WebhookClient> future = new CompletableFuture<>();

            JDA jda = discordSRV.jda().orElse(null);
            if (jda == null) {
                future.completeExceptionally(new NotReadyException());
                return future;
            }

            TextChannel textChannel = jda.getTextChannelById(channelId);
            if (textChannel == null) {
                future.completeExceptionally(new UnknownChannelException());
                return future;
            }

            return textChannel.retrieveWebhooks().submit().thenApply(webhooks -> {
                Webhook hook = null;
                for (Webhook webhook : webhooks) {
                    User user = webhook.getOwnerAsUser();
                    if (user == null
                            || !user.getId().equals(jda.getSelfUser().getId())
                            || !webhook.getName().equals("DiscordSRV")) {
                        continue;
                    }

                    hook = webhook;
                    break;
                }

                return hook;
            }).thenCompose(webhook -> {
                if (webhook != null) {
                    CompletableFuture<Webhook> completableFuture = new CompletableFuture<>();
                    completableFuture.complete(webhook);
                    return completableFuture;
                }

                return textChannel.createWebhook("DiscordSRV").submit();
            }).thenApply(webhook ->
                    WebhookClientBuilder.fromJDA(webhook)
                            .setHttpClient(jda.getHttpClient())
                            .setExecutorService(discordSRV.scheduler().executor())
                            .build()
            );
        }
    }

    private class WebhookCacheExpiry implements Expiry<Long, WebhookClient> {

        private boolean isConfiguredChannel(Long channelId) {
            for (BaseChannelConfig config : discordSRV.config().channels.values()) {
                if (config instanceof ChannelConfig
                        && ((ChannelConfig) config).channelIds.contains(channelId)) {
                    return true;
                }
            }
            return false;
        }

        private long expireAfterWrite(Long channelId) {
            return isConfiguredChannel(channelId) ? Long.MAX_VALUE : TimeUnit.MINUTES.toNanos(15);
        }

        @Override
        public long expireAfterCreate(@NonNull Long channelId, @NonNull WebhookClient webhookClient, long currentTime) {
            return expireAfterWrite(channelId);
        }

        @Override
        public long expireAfterUpdate(@NonNull Long channelId, @NonNull WebhookClient webhookClient, long currentTime, @NonNegative long currentDuration) {
            return expireAfterWrite(channelId);
        }

        @Override
        public long expireAfterRead(@NonNull Long channelId, @NonNull WebhookClient webhookClient, long currentTime, @NonNegative long currentDuration) {
            return isConfiguredChannel(channelId) ? Long.MAX_VALUE : TimeUnit.MINUTES.toNanos(10);
        }
    }
}
