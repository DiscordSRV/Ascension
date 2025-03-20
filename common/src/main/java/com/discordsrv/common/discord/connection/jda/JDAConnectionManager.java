/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.connection.jda;

import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.jda.errorresponse.ErrorCallbackContext;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.events.placeholder.PlaceholderContextMappingEvent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.BotConfig;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.connection.HttpProxyConfig;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.main.MemberCachingConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.scheduler.Scheduler;
import com.discordsrv.common.core.scheduler.threadfactory.CountingThreadFactory;
import com.discordsrv.common.discord.api.DiscordAPIImpl;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.connection.DiscordConnectionManager;
import com.discordsrv.common.discord.connection.details.DiscordConnectionDetailsImpl;
import com.discordsrv.common.feature.debug.DebugGenerateEvent;
import com.discordsrv.common.feature.debug.file.TextDebugFile;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.helper.Timeout;
import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.dv8tion.jda.internal.entities.ReceivedMessage;
import net.dv8tion.jda.internal.hooks.EventManagerProxy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JDAConnectionManager implements DiscordConnectionManager {

    private final DiscordSRV discordSRV;
    private final FailureCallback failureCallback;
    private Future<?> failureCallbackFuture;
    private ScheduledExecutorService gatewayPool;
    private ScheduledExecutorService rateLimitSchedulerPool;
    private ExecutorService rateLimitElasticPool;

    private JDA instance;

    // Currently used intents & cache flags
    private final Set<DiscordGatewayIntent> intents = new HashSet<>();
    private final Set<DiscordCacheFlag> cacheFlags = new HashSet<>();

    // Bot owner details
    private final Timeout botOwnerTimeout = new Timeout(Duration.ofMinutes(5));
    private final AtomicReference<Task<DiscordUser>> botOwnerRequest = new AtomicReference<>();

    // Logging timeouts
    private final Timeout mfaTimeout = new Timeout(Duration.ofSeconds(30));
    private final Timeout serverErrorTimeout = new Timeout(Duration.ofSeconds(20));

    public JDAConnectionManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.failureCallback = new FailureCallback(new NamedLogger(discordSRV, "DISCORD_REQUESTS"));

        // Default failure callback
        RestAction.setDefaultFailure(failureCallback);

        // Disable all mentions by default for safety
        MessageRequest.setDefaultMentions(Collections.emptyList());

        // Disable this warning (that doesn't even have a stacktrace)
        Message.suppressContentIntentWarning();

        discordSRV.eventBus().subscribe(this);
    }

    public Set<DiscordGatewayIntent> getIntents() {
        return intents;
    }

    public Set<DiscordCacheFlag> getCacheFlags() {
        return cacheFlags;
    }

    @Override
    public JDA instance() {
        return instance;
    }

    private void checkDefaultFailureCallback() {
        Consumer<? super Throwable> defaultFailure = RestAction.getDefaultFailure();
        if (defaultFailure != failureCallback) {
            discordSRV.logger().error("RestAction DefaultFailure was set to " + defaultFailure.getClass().getName() + " (" + defaultFailure + ")");
            discordSRV.logger().error("This is unsupported, please specify your own error handling on individual requests instead.");
            RestAction.setDefaultFailure(failureCallback);
        }
    }

    @Subscribe
    public void onStatusChange(StatusChangeEvent event) {
        DiscordSRV.Status currentStatus = discordSRV.status();
        if (currentStatus.isShutdown()) {
            // Don't change the status if it's shutdown
            return;
        }

        JDA.Status status = event.getNewStatus();
        int ordinal = status.ordinal();

        DiscordSRV.Status newStatus;
        if (ordinal < JDA.Status.CONNECTED.ordinal()) {
            newStatus = DiscordSRV.Status.ATTEMPTING_TO_CONNECT;
        } else if (status == JDA.Status.DISCONNECTED || ordinal >= JDA.Status.SHUTTING_DOWN.ordinal()) {
            if (currentStatus.isError() && !currentStatus.isStartupError()) {
                return;
            }

            newStatus = DiscordSRV.Status.FAILED_TO_CONNECT;
        } else {
            newStatus = DiscordSRV.Status.CONNECTED;
        }
        discordSRV.setStatus(newStatus);
    }

    /**
     * Returns the bot owner as a {@link DiscordUser} or {@code null} within 10 seconds.
     * The owner will be cached for 5 minutes, if available it will be passed to the consumer instantly.
     * @param botOwnerConsumer the consumer that will be passed the bot owner or {@code null}
     */
    private void withBotOwner(@NotNull Consumer<DiscordUser> botOwnerConsumer) {
        Task<DiscordUser> request = botOwnerRequest.get();
        if (request != null && !botOwnerTimeout.checkAndUpdate()) {
            request.whenComplete((user, t) -> botOwnerConsumer.accept(t != null ? null : user));
            return;
        }

        Task<DiscordUser> future = discordSRV.discordAPI().toTask(instance.retrieveApplicationInfo().timeout(10, TimeUnit.SECONDS))
                .thenApply(applicationInfo -> api().getUser(applicationInfo.getOwner()));

        botOwnerRequest.set(future);
        future.whenComplete((user, t) -> botOwnerConsumer.accept(t != null ? null : user));
    }

    private DiscordAPIImpl api() {
        return discordSRV.discordAPI();
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append("Intents: ").append(
                intents.stream()
                        .map(intent -> intent.toString() + (intent.privileged() ? "*" : ""))
                        .collect(Collectors.joining(", "))
        );
        builder.append("\nCache Flags: ").append(cacheFlags);

        if (instance != null) {
            builder.append("\nUser cache size: ").append(instance.getUserCache().size());
            builder.append("\nMember cache sizes (").append(instance.getGuildCache().size()).append(" guilds): ");
            for (Guild guild : instance.getGuilds()) {
                long cacheSize = guild.getMemberCache().size();
                long memberCount = guild.getMemberCount();
                double percent = Math.round((cacheSize / (double) memberCount) * 100_00) / 100.00;

                builder.append("\n- ").append(guild.getId()).append(": ").append(cacheSize)
                        .append(", approx size: ").append(memberCount)
                        .append(" (").append(percent).append("% cached)");
            }
            builder.append("\n");

            if (instance.getStatus().ordinal() < JDA.Status.SHUTTING_DOWN.ordinal()) {
                Task<Long> restPingFuture = discordSRV.discordAPI().toTask(instance.getRestPing().timeout(5, TimeUnit.SECONDS));
                builder.append("\nGateway Ping: ").append(instance.getGatewayPing()).append("ms");

                String restPing;
                try {
                    restPing = restPingFuture.get() + "ms";
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof TimeoutException) {
                        restPing = ">5s";
                    } else {
                        restPing = ExceptionUtils.getMessage(e);
                    }
                } catch (Throwable t) {
                    restPing = ExceptionUtils.getMessage(t);
                }

                builder.append("\nRest Ping: ").append(restPing);
            }
        }

        event.addFile("jda_connection_manager.txt", new TextDebugFile(builder));
    }

    @Subscribe(priority = EventPriorities.EARLIEST)
    public void onPlaceholderContextMapping(PlaceholderContextMappingEvent event) {
        // Map JDA objects to 1st party API objects
        event.map(Channel.class, channel -> {
            try {
                return api().getChannel(channel);
            } catch (IllegalArgumentException e) {
                discordSRV.logger().debug("Failed to map " + channel.getClass().getName(), e);
                return channel;
            }
        });
        event.map(Guild.class, guild -> api().getGuild(guild));
        event.map(Member.class, member -> api().getGuildMember(member));
        event.map(Role.class, role -> api().getRole(role));
        event.map(ReceivedMessage.class, message -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, message));
        event.map(User.class, user -> api().getUser(user));

        // Add DiscordUser as context if it is missing and DiscordGuildMember is present
        List<DiscordGuildMember> members = event.getContexts().stream()
                .filter(context -> context instanceof DiscordGuildMember)
                .map(context -> (DiscordGuildMember) context)
                .collect(Collectors.toList());
        for (DiscordGuildMember member : members) {
            DiscordUser user = member.getUser();
            boolean userMissing = event.getContexts().stream()
                    .filter(context -> context instanceof DiscordUser)
                    .noneMatch(context -> ((DiscordUser) context).getId() == user.getId());
            if (!userMissing) {
                event.addContext(user);
            }
        }
    }

    //
    // (Re)connecting & Shutting down
    //

    @Override
    public void connect() {
        if (instance != null && instance.getStatus() != JDA.Status.SHUTDOWN) {
            throw new IllegalStateException("Cannot reconnect, still active");
        }

        BotConfig botConfig = discordSRV.connectionConfig().bot;
        String token = botConfig.token;
        boolean defaultToken = false;
        if (StringUtils.isBlank(token) || (defaultToken = token.equals(BotConfig.DEFAULT_TOKEN))) {
            invalidToken(defaultToken);
            return;
        }

        discordSRV.setStatus(DiscordSRV.Status.ATTEMPTING_TO_CONNECT);
        this.gatewayPool = new ScheduledThreadPoolExecutor(
                1,
                r -> new Thread(r, Scheduler.THREAD_NAME_PREFIX + "JDA Gateway")
        );
        this.rateLimitSchedulerPool = new ScheduledThreadPoolExecutor(
                2,
                new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "JDA RateLimit Scheduler #%s")
        );
        this.rateLimitElasticPool = new ThreadPoolExecutor(
                0,
                12,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "JDA RateLimit Elastic #%s")
        );
        this.failureCallbackFuture = discordSRV.scheduler().runAtFixedRate(
                this::checkDefaultFailureCallback,
                Duration.ofSeconds(30),
                Duration.ofSeconds(120)
        );

        MemberCachingConfig memberCachingConfig = discordSRV.config().memberCaching;
        DiscordConnectionDetailsImpl connectionDetails = discordSRV.discordConnectionDetails();

        this.intents.clear();
        this.intents.addAll(connectionDetails.getGatewayIntents());

        Set<CacheFlag> cacheFlags = new LinkedHashSet<>();
        this.cacheFlags.clear();
        this.cacheFlags.addAll(connectionDetails.getCacheFlags());
        this.cacheFlags.forEach(flag -> {
            cacheFlags.add(flag.asJDA());
            DiscordGatewayIntent intent = flag.requiredIntent();
            if (intent != null) {
                this.intents.add(intent);
            }
        });

        MemberCachePolicy memberCachingPolicy;
        if (memberCachingConfig.all) {
            memberCachingPolicy = MemberCachePolicy.ALL;
        } else if (memberCachingConfig.linkedUsers) {
            memberCachingPolicy = member -> {
                LinkProvider provider = discordSRV.linkProvider();
                if (provider == null) {
                    return false;
                }
                return provider.getCachedPlayerUUID(member.getIdLong()).isPresent();
            };
        } else {
            memberCachingPolicy = MemberCachePolicy.NONE;
        }

        boolean cacheAnyMembers = memberCachingPolicy != MemberCachePolicy.NONE;
        int lruAmount = memberCachingConfig.lru;
        if (lruAmount > 0 && cacheAnyMembers) {
            memberCachingPolicy = memberCachingPolicy.and(MemberCachePolicy.lru(lruAmount));
        }

        ChunkingFilter chunkingFilter;
        if (memberCachingConfig.chunk && cacheAnyMembers) {
            MemberCachingConfig.GuildFilter servers = memberCachingConfig.chunkingServerFilter;
            long[] ids = servers.ids.stream().mapToLong(l -> l).toArray();
            if (servers.blacklist) {
                chunkingFilter = ChunkingFilter.exclude(ids);
            } else {
                chunkingFilter = ChunkingFilter.include(ids);
            }
            this.intents.add(DiscordGatewayIntent.GUILD_MEMBERS);
        } else {
            chunkingFilter = ChunkingFilter.NONE;
        }

        Set<GatewayIntent> intents = new LinkedHashSet<>();
        this.intents.forEach(intent -> intents.add(intent.asJDA()));

        // Start with everything disabled & enable stuff that we actually need
        JDABuilder jdaBuilder = JDABuilder.createLight(token, intents);
        jdaBuilder.enableCache(cacheFlags);
        jdaBuilder.setMemberCachePolicy(memberCachingPolicy);
        jdaBuilder.setChunkingFilter(chunkingFilter);

        // We shut down JDA ourselves. Doing it at the JVM's shutdown may cause errors due to classloading
        jdaBuilder.setEnableShutdownHook(false);

        // We don't use MDC
        jdaBuilder.setContextEnabled(false);

        // Enable event passthrough
        jdaBuilder.setEventPassthrough(true);

        // Custom event manager to forward to the DiscordSRV event bus & block using JDA's event listeners
        jdaBuilder.setEventManager(new EventManagerProxy(new JDAEventManager(discordSRV), discordSRV.scheduler().forkJoinPool()));

        // Our own (named) threads
        jdaBuilder.setCallbackPool(discordSRV.scheduler().forkJoinPool());
        jdaBuilder.setGatewayPool(gatewayPool);
        jdaBuilder.setRateLimitScheduler(rateLimitSchedulerPool);
        jdaBuilder.setRateLimitElastic(rateLimitElasticPool, true);
        jdaBuilder.setHttpClient(discordSRV.httpClient());

        WebSocketFactory webSocketFactory = new WebSocketFactory();

        HttpProxyConfig proxyConfig = discordSRV.connectionConfig().httpProxy;
        if (proxyConfig != null && proxyConfig.enabled) {
            ProxySettings proxySettings = webSocketFactory.getProxySettings();
            proxySettings.setHost(proxyConfig.host);
            proxySettings.setPort(proxyConfig.port);

            HttpProxyConfig.BasicAuthConfig basicAuthConfig = proxyConfig.basicAuth;
            if (basicAuthConfig != null && basicAuthConfig.enabled) {
                proxySettings.setCredentials(basicAuthConfig.username, basicAuthConfig.password);
            }
        }

        jdaBuilder.setWebsocketFactory(webSocketFactory);

        try {
            instance = jdaBuilder.build();
        } catch (InvalidTokenException ignored) {
            invalidToken(false);
        } catch (Throwable t) {
            discordSRV.logger().error("Could not create JDA instance due to an unknown error", t);
        }
    }

    @Subscribe(priority = EventPriorities.LATE)
    public void onDSRVShuttingDown(DiscordSRVShuttingDownEvent event) {
        shutdown(DEFAULT_SHUTDOWN_TIMEOUT);
    }

    @SuppressWarnings("BusyWait") // Known
    public void shutdown(int timeoutSeconds) {
        if (instance == null) {
            shutdownExecutors();
            return;
        }

        instance.shutdown();

        try {
            discordSRV.logger().info("Waiting up to " + timeoutSeconds + " seconds for JDA to shutdown...");
            discordSRV.scheduler().run(() -> {
                try {
                    while (instance != null && instance.getStatus() != JDA.Status.SHUTDOWN && !rateLimitElasticPool.isShutdown()) {
                        Thread.sleep(50);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }).get(timeoutSeconds, TimeUnit.SECONDS);
            instance = null;
            shutdownExecutors();
            discordSRV.logger().info("JDA shutdown completed.");
        } catch (TimeoutException | ExecutionException e) {
            try {
                discordSRV.logger().info("JDA failed to shutdown within the timeout, cancelling any remaining requests");
                shutdownNow();
            } catch (Throwable t) {
                if (e instanceof ExecutionException) {
                    t.addSuppressed(e.getCause());
                }
                discordSRV.logger().error("Failed to shutdown JDA", t);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void shutdownNow() {
        if (instance != null) {
            instance.shutdownNow();
            instance = null;
        }
        shutdownExecutors();
        discordSRV.logger().info("JDA shutdown completed.");
    }

    private void shutdownExecutors() {
        if (gatewayPool != null) {
            gatewayPool.shutdownNow();
        }
        if (rateLimitSchedulerPool != null) {
            rateLimitSchedulerPool.shutdownNow();
        }
        if (rateLimitElasticPool != null && !rateLimitElasticPool.isShutdown()) {
            rateLimitElasticPool.shutdownNow();
        }
        if (failureCallbackFuture != null) {
            failureCallbackFuture.cancel(false);
        }
    }

    //
    // Logging
    //

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        checkCode(event.getCloseCode());
    }

    @Subscribe
    public void onDisconnect(SessionDisconnectEvent event) {
        CloseCode closeCode = event.getCloseCode();
        if (checkCode(closeCode)) {
            return;
        }

        boolean closedByServer = event.isClosedByServer();
        WebSocketFrame frame = closedByServer ? event.getServiceCloseFrame() : event.getClientCloseFrame();
        if (frame == null) {
            throw new IllegalStateException("Could not get the close frame for a disconnect");
        }

        String message;
        if (closedByServer) {
            String closeReason = frame.getCloseReason();

            message = "[JDA] [Server] Disconnected due to "
                    + frame.getCloseCode() + ": "
                    + (closeCode != null
                    ? closeCode.getMeaning()
                    : (closeReason != null ? closeReason : "(Unknown close reason)"));
        } else {
            message = "[JDA] [Client] Disconnected due to "
                    + frame.getCloseCode() + ": "
                    + frame.getCloseReason();
        }

        if (closeCode != null && !closeCode.isReconnect()) {
            discordSRV.logger().error(message);
        } else {
            discordSRV.logger().debug(message);
        }
    }

    private boolean checkCode(CloseCode closeCode) {
        if (closeCode == null) {
            return false;
        } else if (closeCode == CloseCode.DISALLOWED_INTENTS) {
            discordSRV.logger().error("+-------------------------------------->");
            discordSRV.logger().error("| Failed to connect to Discord:");
            discordSRV.logger().error("|");
            discordSRV.logger().error("| The Discord bot is lacking one or more");
            discordSRV.logger().error("| privileged intents listed below");
            discordSRV.logger().error("|");
            for (DiscordGatewayIntent intent : intents) {
                if (!intent.privileged()) {
                    continue;
                }

                String displayName = intent.portalName();
                discordSRV.logger().error("| " + displayName);
            }
            discordSRV.logger().error("|");
            discordSRV.logger().error("| Instructions for enabling privileged gateway intents:");
            discordSRV.logger().error("| 1. Go to https://discord.com/developers/applications");
            discordSRV.logger().error("| 2. Choose the bot you are using for DiscordSRV");
            discordSRV.logger().error("|     - Keep in mind it will only be visible to the ");
            discordSRV.logger().error("|       Discord user who created the bot");
            discordSRV.logger().error("| 3. Go to the \"Bot\" tab");
            discordSRV.logger().error("| 4. Make sure the intents listed above are all enabled");
            discordSRV.logger().error("| 5. Run the \"/discordsrv reload config discord_connection\" command");
            discordSRV.logger().error("+-------------------------------------->");
            discordSRV.setStatus(DiscordSRV.Status.DISALLOWED_INTENTS);
            return true;
        } else if (closeCode == CloseCode.AUTHENTICATION_FAILED) {
            invalidToken(false);
            return true;
        }
        return false;
    }

    public void invalidToken(boolean defaultToken) {
        List<String> lines = Arrays.asList(
                "+------------------------------>",
                "| Failed to connect to Discord:",
                "|",
                "| The token provided in the",
                "| " + ConnectionConfig.FILE_NAME + " is invalid",
                "|",
                "| Haven't created a bot yet? Installing the plugin for the first time?",
                "| See " + DocumentationURLs.CREATE_TOKEN,
                "|",
                "| Already have a bot? You can get the token for your bot from:",
                "| https://discord.com/developers/applications",
                "| by selecting the application, going to the \"Bot\" tab",
                "| and clicking on \"Reset Token\"",
                "| - Keep in mind the bot is only visible to",
                "|   the Discord user that created the bot",
                "|",
                "| Once the token is corrected in the " + ConnectionConfig.FILE_NAME,
                "| Run the \"/discordsrv reload config discord_connection\" command",
                "+------------------------------>"
        );
        if (defaultToken) {
            lines.forEach(line -> discordSRV.logger().warning(line));
        } else {
            lines.forEach(line -> discordSRV.logger().error(line));
        }
        discordSRV.setStatus(DiscordSRV.Status.INVALID_TOKEN);
    }

    private class FailureCallback implements Consumer<Throwable> {

        private final Logger logger;

        protected FailureCallback(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void accept(Throwable t) {
            if (t instanceof ErrorCallbackContext.Exception) {
                accept(t.getMessage(), t.getCause());
            } else {
                accept(null, t);
            }
        }

        public void accept(String context, Throwable t) {
            if ((t instanceof InterruptedIOException || t instanceof InterruptedException)
                    && discordSRV.status().isShutdown()) {
                // Ignore interrupted exceptions when DiscordSRV is shutting down or shutdown
                return;
            }

            boolean cancelled;
            if ((cancelled = t instanceof CancellationException) || t instanceof TimeoutException) {
                // Cancelling/timing out requests is always intentional
                logger.debug("A request " + (cancelled ? "was cancelled" : "timed out"), t.getCause());
            } else if (t instanceof RateLimitedException) {
                // Log route & retry after on warn & context on debug
                RateLimitedException exception = ((RateLimitedException) t);
                discordSRV.logger().warning("A request on route " + exception.getRateLimitedRoute()
                                                    + " was rate-limited for " + exception.getRetryAfter() + "ms");
                logger.debug(exception.getCause());
            } else if (t instanceof ErrorResponseException) {
                ErrorResponseException exception = (ErrorResponseException) t;
                if (exception.getErrorCode() == Response.ERROR_CODE) {
                    // There is no response due to a client error
                    Throwable cause = exception.getCause();
                    if (cause != null) {
                        // Run the cause through this method again
                        accept(context, cause);
                    } else {
                        logger.error((context != null ? context + ": " : "") + "Failed to complete request for a unknown reason", exception);
                    }
                    return;
                }

                ErrorResponse response = exception.getErrorResponse();
                switch (response) {
                    case MFA_NOT_ENABLED: {
                        if (!mfaTimeout.checkAndUpdate()) {
                            return;
                        }
                        withBotOwner(user -> {
                            discordSRV.logger().error("+----------------------------------------------->");
                            discordSRV.logger().error("| Failed to complete a request:");
                            discordSRV.logger().error("|");
                            discordSRV.logger().error("| The Discord bot's owner needs to enable 2FA");
                            discordSRV.logger().error("| on their Discord account due to a Discord");
                            discordSRV.logger().error("| server requiring 2FA for moderation actions");
                            if (user != null) {
                                discordSRV.logger().error("|");
                                discordSRV.logger().error("| The Discord bot's owner is " + user.getUsername() + " (" + user.getId() + ")");
                            }
                            discordSRV.logger().error("|");
                            discordSRV.logger().error("| You can view instructions for enabling 2FA here:");
                            discordSRV.logger().error("| https://support.discord.com/hc/en-us/articles/219576828-Setting-up-Two-Factor-Authentication");
                            discordSRV.logger().error("+----------------------------------------------->");
                        });
                        return;
                    }
                    case SERVER_ERROR: {
                        if (serverErrorTimeout.checkAndUpdate()) {
                            discordSRV.logger().error("+--------------------------------------------------------------->");
                            discordSRV.logger().error("| Failed to complete a request:");
                            discordSRV.logger().error("|");
                            discordSRV.logger().error("| Discord sent a server error (HTTP 500) as a response.");
                            discordSRV.logger().error("| This is usually caused by a outage (https://discordstatus.com)");
                            discordSRV.logger().error("| and will stop happening once Discord stabilizes");
                            discordSRV.logger().error("+--------------------------------------------------------------->");
                        } else {
                            // Log as debug as to not spam out the server console/log
                            logger.debug("Failed to complete a request, Discord returned a server error (HTTP 500)");
                        }
                        // Log context to find what made the request
                        logger.debug(exception.getCause());
                        return;
                    }
                    default: break;
                }

                logger.error((context != null ? context : "Failed to complete a request") + ": " + response.getMeaning());
                logger.debug(exception);
            } else {
                logger.error(context != null ? context : "Failed to complete a request due to unknown error", t);
            }
        }
    }
}
