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

package com.discordsrv.common.discord.api;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.discordsrv.api.discord.DiscordAPI;
import com.discordsrv.api.discord.connection.jda.errorresponse.ErrorCallbackContext;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.*;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.NotReadyException;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.channels.base.ThreadConfig;
import com.discordsrv.common.discord.api.entity.DiscordUserImpl;
import com.discordsrv.common.discord.api.entity.channel.*;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildImpl;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildMemberImpl;
import com.discordsrv.common.discord.api.entity.guild.DiscordRoleImpl;
import com.discordsrv.common.function.CheckedSupplier;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DiscordAPIImpl implements DiscordAPI {

    private final DiscordSRV discordSRV;
    private final AsyncLoadingCache<Long, WebhookClient> cachedClients;
    private final List<ThreadChannelLookup> threadLookups = new CopyOnWriteArrayList<>();

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

    /**
     * Finds active threads based for the provided {@link IChannelConfig}.
     * @param config the config that specified the threads
     * @return the list of active threads
     */
    public List<DiscordThreadChannel> findThreads(OrDefault<BaseChannelConfig> config, IChannelConfig channelConfig) {
        List<DiscordThreadChannel> channels = new ArrayList<>();
        findOrCreateThreads(config, channelConfig, channels::add, null, false);
        return channels;
    }

    /**
     * Finds or potentially unarchives or creates threads based on the provided {@link IChannelConfig}.
     * @param config the config
     * @param channelConsumer the consumer that will take the channels as they are gathered
     * @param futures a possibly null list of {@link CompletableFuture} for tasks that need to be completed to get all threads
     */
    public void findOrCreateThreads(
            OrDefault<BaseChannelConfig> config,
            IChannelConfig channelConfig,
            Consumer<DiscordThreadChannel> channelConsumer,
            @Nullable List<CompletableFuture<DiscordThreadChannel>> futures,
            boolean log
    ) {
        List<ThreadConfig> threads = channelConfig.threads();
        if (threads == null) {
            return;
        }

        for (ThreadConfig threadConfig : threads) {
            long channelId = threadConfig.channelId;
            DiscordTextChannel channel = getTextChannelById(channelId).orElse(null);
            if (channel == null) {
                if (channelId > 0 && log) {
                    discordSRV.logger().error("Unable to find channel with ID " + Long.toUnsignedString(channelId));
                }
                continue;
            }

            // Check if a thread by the same name is still active
            DiscordThreadChannel thread = findThread(threadConfig, channel.getActiveThreads());
            if (thread != null) {
                ThreadChannel jdaChannel = thread.asJDA();
                if (!jdaChannel.isArchived()) {
                    channelConsumer.accept(thread);
                    continue;
                }
            }

            if (futures == null) {
                // Futures not specified, don't try to unarchive or create threads
                continue;
            }

            CompletableFuture<DiscordThreadChannel> future;
            if (thread != null) {
                // Unarchive the thread
                future = new CompletableFuture<>();
                unarchiveOrCreateThread(threadConfig, channel, thread, future);
            } else {
                // Find or create the thread
                future = findOrCreateThread(config, threadConfig, channel);
            }

            futures.add(future.handle((threadChannel, t) -> {
                if (t != null) {
                    ErrorCallbackContext.context(
                            "Failed to deliver message to thread \""
                                    + threadConfig.threadName + "\" in channel " + channel
                    ).accept(t);
                    throw new RuntimeException(); // Just here to fail the future
                }

                if (threadChannel != null) {
                    channelConsumer.accept(threadChannel);
                }
                return threadChannel;
            }));
        }
    }

    private DiscordThreadChannel findThread(ThreadConfig config, List<DiscordThreadChannel> threads) {
        for (DiscordThreadChannel thread : threads) {
            if (thread.getName().equals(config.threadName)) {
                return thread;
            }
        }
        return null;
    }

    private CompletableFuture<DiscordThreadChannel> findOrCreateThread(
            OrDefault<BaseChannelConfig> config,
            ThreadConfig threadConfig,
            DiscordTextChannel textChannel
    ) {
        if (!config.map(cfg -> cfg.channelLocking).map(cfg -> cfg.threads).get(cfg -> cfg.unarchive, true)) {
            return textChannel.createThread(threadConfig.threadName, threadConfig.privateThread);
        }

        CompletableFuture<DiscordThreadChannel> completableFuture = new CompletableFuture<>();
        lookupThreads(
                textChannel,
                threadConfig.privateThread,
                lookup -> findOrCreateThread(threadConfig, textChannel, lookup, completableFuture),
                (thread, throwable) -> {
                    if (throwable != null) {
                        completableFuture.completeExceptionally(throwable);
                    } else {
                        completableFuture.complete(thread);
                    }
                });
        return completableFuture;
    }

    private void findOrCreateThread(
            ThreadConfig config,
            DiscordTextChannel textChannel,
            ThreadChannelLookup lookup,
            CompletableFuture<DiscordThreadChannel> completableFuture
    ) {
        completableFuture.whenComplete((threadChannel, throwable) -> {
            CompletableFuture<DiscordThreadChannel> future = lookup.getChannelFuture();
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                future.complete(threadChannel);
            }
        });
        lookup.getFuture().whenComplete((channels, throwable) -> {
            if (throwable != null) {
                completableFuture.completeExceptionally(throwable);
                return;
            }

            DiscordThreadChannel thread = findThread(config, channels);
            unarchiveOrCreateThread(config, textChannel, thread, completableFuture);
        }).exceptionally(t -> {
            if (t instanceof CompletionException) {
                completableFuture.completeExceptionally(t.getCause());
                return null;
            }
            completableFuture.completeExceptionally(t);
            return null;
        });
    }

    private void unarchiveOrCreateThread(
            ThreadConfig config,
            DiscordTextChannel textChannel,
            DiscordThreadChannel thread,
            CompletableFuture<DiscordThreadChannel> future
    ) {
        if (thread != null) {
            ThreadChannel channel = thread.asJDA();
            if (channel.isLocked() || channel.isArchived()) {
                try {
                    channel.getManager()
                            .setArchived(false)
                            .reason("DiscordSRV Auto Unarchive")
                            .queue(v -> future.complete(thread), future::completeExceptionally);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            } else {
                future.complete(thread);
            }
            return;
        }

        textChannel.createThread(config.threadName, config.privateThread).whenComplete(((threadChannel, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
            } else {
                future.complete(threadChannel);
            }
        }));
    }

    public void lookupThreads(
            DiscordTextChannel textChannel,
            boolean privateThreads,
            Consumer<ThreadChannelLookup> lookupConsumer,
            BiConsumer<DiscordThreadChannel, Throwable> channelConsumer
    ) {
        ThreadChannelLookup lookup;
        synchronized (threadLookups) {
            for (ThreadChannelLookup threadLookup : threadLookups) {
                if (threadLookup.isPrivateThreads() != privateThreads
                        || threadLookup.getChannelId() != textChannel.getId()) {
                    continue;
                }

                threadLookup.getChannelFuture().whenComplete(channelConsumer);
                return;
            }

            lookup = new ThreadChannelLookup(
                    textChannel.getId(), privateThreads,
                    privateThreads
                        ? textChannel.retrieveArchivedPrivateThreads()
                        : textChannel.retrieveArchivedPublicThreads()
            );
            threadLookups.add(lookup);
        }

        lookup.getChannelFuture().whenComplete(channelConsumer);
        lookupConsumer.accept(lookup);
        lookup.getFuture().whenComplete((channel, t) -> threadLookups.remove(lookup));
    }

    public <T> CompletableFuture<T> mapExceptions(CheckedSupplier<CompletableFuture<T>> futureSupplier) {
        try {
            return mapExceptions(futureSupplier.get());
        } catch (Throwable t) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return future;
        }
    }

    public <T> CompletableFuture<T> mapExceptions(CompletableFuture<T> future) {
        return future.handle((response, t) -> {
            if (t instanceof ErrorResponseException) {
                ErrorResponseException exception = (ErrorResponseException) t;
                int code = exception.getErrorCode();
                ErrorResponse errorResponse = exception.getErrorResponse();
                throw new RestErrorResponseException(code, errorResponse != null ? errorResponse.getMeaning() : "Unknown", t);
            } else if (t != null) {
                throw (RuntimeException) t;
            }
            return response;
        });
    }

    public <T> CompletableFuture<T> notReady() {
        return CompletableFutureUtil.failed(new NotReadyException());
    }

    @Override
    public @NotNull Optional<? extends DiscordMessageChannel> getMessageChannelById(long id) {
        Optional<DiscordTextChannel> textChannel = getTextChannelById(id);
        if (textChannel.isPresent()) {
            return textChannel;
        }

        Optional<DiscordThreadChannel> threadChannel = getCachedThreadChannelById(id);
        if (threadChannel.isPresent()) {
            return threadChannel;
        }

        return getDirectMessageChannelById(id);
    }

    public AbstractDiscordMessageChannel<?> getMessageChannel(MessageChannel jda) {
        if (jda instanceof TextChannel) {
            return getTextChannel((TextChannel) jda);
        } else if (jda instanceof ThreadChannel) {
            return getThreadChannel((ThreadChannel) jda);
        } else if (jda instanceof PrivateChannel) {
            return getDirectMessageChannel((PrivateChannel) jda);
        } else if (jda instanceof NewsChannel) {
            return getNewsChannel((NewsChannel) jda);
        } else {
            throw new IllegalArgumentException("Unmappable MessageChannel type: " + jda.getClass().getName());
        }
    }

    @Override
    public @NotNull Optional<DiscordDMChannel> getDirectMessageChannelById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getPrivateChannelById(id))
                .map(this::getDirectMessageChannel);
    }

    public DiscordDMChannelImpl getDirectMessageChannel(PrivateChannel jda) {
        return new DiscordDMChannelImpl(discordSRV, jda);
    }

    @Override
    public @NotNull Optional<DiscordNewsChannel> getNewsChannelById(long id) {
        return Optional.empty();
    }

    public DiscordNewsChannelImpl getNewsChannel(NewsChannel jda) {
        return new DiscordNewsChannelImpl(discordSRV, jda);
    }

    @Override
    public @NotNull Optional<DiscordTextChannel> getTextChannelById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getTextChannelById(id))
                .map(this::getTextChannel);
    }

    public DiscordTextChannelImpl getTextChannel(TextChannel jda) {
        return new DiscordTextChannelImpl(discordSRV, jda);
    }

    @Override
    public @NotNull Optional<DiscordThreadChannel> getCachedThreadChannelById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getThreadChannelById(id))
                .map(this::getThreadChannel);
    }

    public DiscordThreadChannelImpl getThreadChannel(ThreadChannel jda) {
        return new DiscordThreadChannelImpl(discordSRV, jda);
    }

    @Override
    public @NotNull Optional<DiscordGuild> getGuildById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getGuildById(id))
                .map(this::getGuild);
    }

    public DiscordGuildImpl getGuild(Guild jda) {
        return new DiscordGuildImpl(discordSRV, jda);
    }

    public DiscordGuildMemberImpl getGuildMember(Member jda) {
        return new DiscordGuildMemberImpl(discordSRV, jda);
    }

    @Override
    public @NotNull Optional<DiscordUser> getUserById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getUserById(id))
                .map(this::getUser);
    }

    public DiscordUserImpl getUser(User jda) {
        return new DiscordUserImpl(discordSRV, jda);
    }

    @Override
    public @NotNull CompletableFuture<DiscordUser> retrieveUserById(long id) {
        JDA jda = discordSRV.jda().orElse(null);
        if (jda == null) {
            return notReady();
        }

        return jda.retrieveUserById(id)
                .submit()
                .thenApply(this::getUser);
    }

    @Override
    public boolean isUserCachingEnabled() {
        return discordSRV.discordConnectionDetails()
                .getGatewayIntents()
                .contains(GatewayIntent.GUILD_MEMBERS);
    }

    @Override
    public @NotNull Optional<DiscordRole> getRoleById(long id) {
        return discordSRV.jda()
                .map(jda -> jda.getRoleById(id))
                .map(this::getRole);
    }

    public DiscordRoleImpl getRole(Role jda) {
        return new DiscordRoleImpl(discordSRV, jda);
    }

    private class WebhookCacheLoader implements AsyncCacheLoader<Long, WebhookClient> {

        @Override
        public @NonNull CompletableFuture<WebhookClient> asyncLoad(@NonNull Long channelId, @NonNull Executor executor) {
            JDA jda = discordSRV.jda().orElse(null);
            if (jda == null) {
                return notReady();
            }

            TextChannel textChannel = jda.getTextChannelById(channelId);
            if (textChannel == null) {
                return CompletableFutureUtil.failed(new IllegalArgumentException("Channel could not be found"));
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
                    return CompletableFuture.completedFuture(webhook);
                }

                return textChannel.createWebhook("DiscordSRV").submit();
            }).thenApply(webhook ->
                    WebhookClientBuilder.fromJDA(webhook)
                            .setHttpClient(jda.getHttpClient())
                            .setExecutorService(discordSRV.scheduler().scheduledExecutorService())
                            .build()
            );
        }
    }

    private class WebhookCacheExpiry implements Expiry<Long, WebhookClient> {

        private boolean isConfiguredChannel(Long channelId) {
            for (BaseChannelConfig config : discordSRV.config().channels.values()) {
                if (config instanceof IChannelConfig
                        && ((IChannelConfig) config).channelIds().contains(channelId)) {
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
