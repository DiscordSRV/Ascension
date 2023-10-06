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

package com.discordsrv.common.discord.api;

import com.discordsrv.api.discord.DiscordAPI;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.jda.errorresponse.ErrorCallbackContext;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.*;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.entity.interaction.command.CommandType;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.exception.NotReadyException;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.generic.ThreadConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.discord.api.entity.DiscordUserImpl;
import com.discordsrv.common.discord.api.entity.channel.*;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildImpl;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildMemberImpl;
import com.discordsrv.common.discord.api.entity.guild.DiscordRoleImpl;
import com.discordsrv.common.function.CheckedSupplier;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Expiry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class DiscordAPIImpl implements DiscordAPI {

    private final DiscordSRV discordSRV;
    private final DiscordCommandRegistry commandRegistry;
    private final AsyncLoadingCache<Long, WebhookClient<Message>> cachedClients;
    private final List<ThreadChannelLookup> threadLookups = new CopyOnWriteArrayList<>();

    public DiscordAPIImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.commandRegistry = new DiscordCommandRegistry(discordSRV);
        this.cachedClients = discordSRV.caffeineBuilder()
                .expireAfter(new WebhookCacheExpiry())
                .buildAsync(new WebhookCacheLoader());
    }

    public CompletableFuture<WebhookClient<Message>> queryWebhookClient(long channelId) {
        return cachedClients.get(channelId);
    }

    public AsyncLoadingCache<Long, WebhookClient<Message>> getCachedClients() {
        return cachedClients;
    }

    public <T extends BaseChannelConfig & IChannelConfig> List<DiscordGuildMessageChannel> findDestinations(
            T config,
            boolean log
    ) {
        return findOrCreateDestinations(config, false, log).join();
    }

    public <T extends BaseChannelConfig & IChannelConfig> CompletableFuture<List<DiscordGuildMessageChannel>> findOrCreateDestinations(
            T config,
            boolean create,
            boolean log
    ) {
        return findOrCreateDestinations(config.destination(), config.channelLocking.threads.unarchive, create, log);
    }

    public CompletableFuture<List<DiscordGuildMessageChannel>> findOrCreateDestinations(
            DestinationConfig destination,
            boolean unarchive,
            boolean create,
            boolean log
    ) {

        List<DiscordGuildMessageChannel> channels = new CopyOnWriteArrayList<>();
        for (Long channelId : destination.channelIds) {
            DiscordMessageChannel channel = getMessageChannelById(channelId);
            if (!(channel instanceof DiscordGuildMessageChannel)) {
                continue;
            }
            channels.add((DiscordGuildMessageChannel) channel);
        }

        List<CompletableFuture<Void>> threadFutures = new ArrayList<>();
        List<ThreadConfig> threadConfigs = destination.threads;
        if (threadConfigs != null && !threadConfigs.isEmpty()) {
            for (ThreadConfig threadConfig : threadConfigs) {
                long channelId = threadConfig.channelId;
                DiscordThreadContainer channel = getTextChannelById(channelId);
                if (channel == null) {
                    channel = getForumChannelById(channelId);
                }
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
                        channels.add(getThreadChannel(jdaChannel));
                        continue;
                    }
                }

                if (!create) {
                    continue;
                }

                CompletableFuture<DiscordThreadChannel> future;
                if (thread != null) {
                    // Unarchive the thread
                    future = new CompletableFuture<>();
                    unarchiveOrCreateThread(threadConfig, channel, thread, future);
                } else {
                    // Find or create the thread
                    future = findOrCreateThread(unarchive, threadConfig, channel);
                }

                DiscordThreadContainer container = channel;
                threadFutures.add(future.handle((threadChannel, t) -> {
                    if (t != null) {
                        ErrorCallbackContext.context(
                                "Failed to deliver message to thread \""
                                        + threadConfig.threadName + "\" in channel " + container
                        ).accept(t);
                        throw new RuntimeException(); // Just here to fail the future
                    }

                    if (threadChannel != null) {
                        channels.add(threadChannel);
                    }
                    return null;
                }));
            }
        }

        return CompletableFutureUtil.combine(threadFutures).thenApply(v -> channels);
    }

    private DiscordThreadChannel findThread(ThreadConfig config, List<DiscordThreadChannel> threads) {
        for (DiscordThreadChannel thread : threads) {
            if (thread.getName().equals(config.threadName)) {
                return thread;
            }
        }
        return null;
    }

    private CompletableFuture<DiscordThreadChannel> findOrCreateThread(boolean unarchive, ThreadConfig threadConfig, DiscordThreadContainer container) {
        if (!unarchive) {
            return container.createThread(threadConfig.threadName, threadConfig.privateThread);
        }

        CompletableFuture<DiscordThreadChannel> completableFuture = new CompletableFuture<>();
        lookupThreads(
                container,
                threadConfig.privateThread,
                lookup -> findOrCreateThread(threadConfig, container, lookup, completableFuture),
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
            DiscordThreadContainer container,
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
            unarchiveOrCreateThread(config, container, thread, completableFuture);
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
            DiscordThreadContainer container,
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

        container.createThread(config.threadName, config.privateThread).whenComplete(((threadChannel, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
            } else {
                future.complete(threadChannel);
            }
        }));
    }

    public void lookupThreads(
            DiscordThreadContainer container,
            boolean privateThreads,
            Consumer<ThreadChannelLookup> lookupConsumer,
            BiConsumer<DiscordThreadChannel, Throwable> channelConsumer
    ) {
        ThreadChannelLookup lookup;
        synchronized (threadLookups) {
            for (ThreadChannelLookup threadLookup : threadLookups) {
                if (threadLookup.isPrivateThreads() != privateThreads
                        || threadLookup.getChannelId() != container.getId()) {
                    continue;
                }

                threadLookup.getChannelFuture().whenComplete(channelConsumer);
                return;
            }

            lookup = new ThreadChannelLookup(
                    container.getId(), privateThreads,
                    privateThreads
                        ? container.retrieveArchivedPrivateThreads()
                        : container.retrieveArchivedPublicThreads()
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
            return CompletableFutureUtil.failed(t);
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
    public @Nullable DiscordMessageChannel getMessageChannelById(long id) {
        DiscordTextChannel textChannel = getTextChannelById(id);
        if (textChannel != null) {
            return textChannel;
        }

        DiscordThreadChannel threadChannel = getCachedThreadChannelById(id);
        if (threadChannel != null) {
            return threadChannel;
        }

        return getDirectMessageChannelById(id);
    }

    public DiscordChannel getChannel(Channel jda) {
        if (jda instanceof ForumChannel) {
            return getForumChannel((ForumChannel) jda);
        } else if (jda instanceof MessageChannel) {
            return getMessageChannel((MessageChannel) jda);
        } else {
            throw new IllegalArgumentException("Unmappable Channel type: " + jda.getClass().getName());
        }
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

    private <T, J> T mapJDAEntity(Function<JDA, J> get, Function<J, T> map) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return null;
        }

        J entity = get.apply(jda);
        if (entity == null) {
            return null;
        }

        return map.apply(entity);
    }

    @Override
    public @Nullable DiscordDMChannel getDirectMessageChannelById(long id) {
        return mapJDAEntity(jda -> jda.getPrivateChannelById(id), this::getDirectMessageChannel);
    }

    public DiscordDMChannelImpl getDirectMessageChannel(PrivateChannel jda) {
        return new DiscordDMChannelImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordNewsChannel getNewsChannelById(long id) {
        return null;
    }

    public DiscordNewsChannelImpl getNewsChannel(NewsChannel jda) {
        return new DiscordNewsChannelImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordTextChannel getTextChannelById(long id) {
        return mapJDAEntity(jda -> jda.getTextChannelById(id), this::getTextChannel);
    }

    public DiscordTextChannelImpl getTextChannel(TextChannel jda) {
        return new DiscordTextChannelImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordForumChannel getForumChannelById(long id) {
        return mapJDAEntity(jda -> jda.getForumChannelById(id), this::getForumChannel);
    }

    public DiscordForumChannelImpl getForumChannel(ForumChannel jda) {
        return new DiscordForumChannelImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordVoiceChannel getVoiceChannelById(long id) {
        return mapJDAEntity(jda -> jda.getVoiceChannelById(id), this::getVoiceChannel);
    }

    public DiscordVoiceChannelImpl getVoiceChannel(VoiceChannel jda) {
        return new DiscordVoiceChannelImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordStageChannel getStageChannelById(long id) {
        return mapJDAEntity(jda -> jda.getStageChannelById(id), this::getStageChannel);
    }

    public DiscordStageChannelImpl getStageChannel(StageChannel jda) {
        return new DiscordStageChannelImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordThreadChannel getCachedThreadChannelById(long id) {
        return mapJDAEntity(jda -> jda.getThreadChannelById(id), this::getThreadChannel);
    }

    public DiscordThreadChannelImpl getThreadChannel(ThreadChannel jda) {
        return new DiscordThreadChannelImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordGuild getGuildById(long id) {
        return mapJDAEntity(jda -> jda.getGuildById(id), this::getGuild);
    }

    public DiscordGuildImpl getGuild(Guild jda) {
        return new DiscordGuildImpl(discordSRV, jda);
    }

    public DiscordGuildMemberImpl getGuildMember(Member jda) {
        return new DiscordGuildMemberImpl(discordSRV, jda);
    }

    @Override
    public @Nullable DiscordUser getUserById(long id) {
        return mapJDAEntity(jda -> jda.getUserById(id), this::getUser);
    }

    public DiscordUserImpl getUser(User jda) {
        return new DiscordUserImpl(discordSRV, jda);
    }

    @Override
    public @NotNull CompletableFuture<DiscordUser> retrieveUserById(long id) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return notReady();
        }

        return mapExceptions(
                jda.retrieveUserById(id)
                        .submit()
                        .thenApply(this::getUser)
        );
    }

    @Override
    public boolean isUserCachingEnabled() {
        return discordSRV.discordConnectionManager().getIntents()
                .contains(DiscordGatewayIntent.GUILD_MEMBERS);
    }

    @Override
    public @Nullable DiscordRole getRoleById(long id) {
        return mapJDAEntity(jda -> jda.getRoleById(id), this::getRole);
    }

    public DiscordRoleImpl getRole(Role jda) {
        return new DiscordRoleImpl(discordSRV, jda);
    }

    @Override
    public DiscordCommand.RegistrationResult registerCommand(DiscordCommand command) {
        return commandRegistry.register(command, false);
    }

    @Override
    public void unregisterCommand(DiscordCommand command) {
        commandRegistry.unregister(command);
    }

    public Optional<DiscordCommand> getActiveCommand(@Nullable Guild guild, CommandType type, String name) {
        return Optional.ofNullable(commandRegistry.getActive(guild != null ? guild.getIdLong() : null, type, name));
    }

    public DiscordCommandRegistry commandRegistry() {
        return commandRegistry;
    }

    private class WebhookCacheLoader implements AsyncCacheLoader<Long, WebhookClient<Message>> {

        @Override
        public @NonNull CompletableFuture<WebhookClient<Message>> asyncLoad(@NonNull Long channelId, @NonNull Executor executor) {
            JDA jda = discordSRV.jda();
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
                            || !webhook.getName().equals("DSRV")) {
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

                return textChannel.createWebhook("DSRV").submit();
            }).thenApply(webhook ->
                    WebhookClient.createClient(
                            webhook.getJDA(),
                            webhook.getId(),
                            Objects.requireNonNull(webhook.getToken())
                    )
            );
        }
    }

    private class WebhookCacheExpiry implements Expiry<Long, WebhookClient<Message>> {

        private boolean isConfiguredChannel(Long channelId) {
            for (BaseChannelConfig config : discordSRV.config().channels.values()) {
                DestinationConfig destination = config instanceof IChannelConfig ? ((IChannelConfig) config).destination() : null;
                if (destination == null) {
                    continue;
                }

                if (destination.channelIds.contains(channelId)) {
                    return true;
                }
                for (ThreadConfig thread : destination.threads) {
                    if (Objects.equals(thread.channelId, channelId)) {
                        return true;
                    }
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
