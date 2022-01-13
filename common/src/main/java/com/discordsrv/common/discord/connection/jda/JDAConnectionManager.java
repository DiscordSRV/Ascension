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

package com.discordsrv.common.discord.connection.jda;

import com.discordsrv.api.discord.api.entity.DiscordUser;
import com.discordsrv.api.discord.connection.DiscordConnectionDetails;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.event.events.placeholder.PlaceholderLookupEvent;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.discord.api.entity.channel.DiscordDMChannelImpl;
import com.discordsrv.common.discord.api.entity.channel.DiscordTextChannelImpl;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildImpl;
import com.discordsrv.common.discord.api.entity.guild.DiscordGuildMemberImpl;
import com.discordsrv.common.discord.api.entity.guild.DiscordRoleImpl;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.DiscordUserImpl;
import com.discordsrv.common.discord.connection.DiscordConnectionManager;
import com.discordsrv.common.scheduler.Scheduler;
import com.discordsrv.common.scheduler.threadfactory.CountingThreadFactory;
import com.discordsrv.common.time.util.Timeout;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.entities.ReceivedMessage;
import net.dv8tion.jda.internal.hooks.EventManagerProxy;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class JDAConnectionManager implements DiscordConnectionManager {

    private static final Map<GatewayIntent, String> PRIVILEGED_INTENTS = new HashMap<>();

    static {
        PRIVILEGED_INTENTS.put(GatewayIntent.GUILD_MEMBERS, "Server Members Intent");
        PRIVILEGED_INTENTS.put(GatewayIntent.GUILD_PRESENCES, "Presence Intent");
    }

    private final DiscordSRV discordSRV;
    private final ScheduledExecutorService gatewayPool;
    private final ScheduledExecutorService rateLimitPool;

    private CompletableFuture<Void> connectionFuture;
    private JDA instance;
    private boolean detailsAccepted = true;

    // Bot owner details
    private final Timeout botOwnerTimeout = new Timeout(5, TimeUnit.MINUTES);
    private final AtomicReference<CompletableFuture<DiscordUser>> botOwnerRequest = new AtomicReference<>();

    // Logging timeouts
    private final Timeout mfaTimeout = new Timeout(30, TimeUnit.SECONDS);
    private final Timeout serverErrorTimeout = new Timeout(20, TimeUnit.SECONDS);

    public JDAConnectionManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.gatewayPool = new ScheduledThreadPoolExecutor(
                1,
                r -> new Thread(r, Scheduler.THREAD_NAME_PREFIX + "JDA Gateway")
        );
        this.rateLimitPool = new ScheduledThreadPoolExecutor(
                5,
                new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "JDA RateLimit #%s")
        );

        // Set default failure handling
        RestAction.setDefaultFailure(new DefaultFailureCallback());

        // Disable all mentions by default for safety
        AllowedMentions.setDefaultMentions(Collections.emptyList());

        discordSRV.eventBus().subscribe(this);
    }

    @Override
    public JDA instance() {
        return instance;
    }

    @Override
    public boolean areDetailsAccepted() {
        return detailsAccepted;
    }

    @Subscribe
    public void onStatusChange(StatusChangeEvent event) {
        DiscordSRV.Status currentStatus = discordSRV.status();
        if (currentStatus.isShutdown() || currentStatus.isStartupError()) {
            // Don't change the status if it's shutdown or failed to start
            return;
        }

        JDA.Status status = event.getNewStatus();
        int ordinal = status.ordinal();

        DiscordSRV.Status newStatus;
        if (ordinal < JDA.Status.CONNECTED.ordinal()) {
            newStatus = DiscordSRV.Status.ATTEMPTING_TO_CONNECT;
        } else if (ordinal < JDA.Status.SHUTTING_DOWN.ordinal()) {
            newStatus = DiscordSRV.Status.CONNECTED;
        } else {
            newStatus = DiscordSRV.Status.FAILED_TO_CONNECT;
        }
        discordSRV.setStatus(newStatus);
    }

    /**
     * Returns the bot owner as a {@link DiscordUser} or {@code null} within 10 seconds.
     * The owner will be cached for 5 minutes, if available it will be passed to the consumer instantly.
     * @param botOwnerConsumer the consumer that will be passed the bot owner or {@code null}
     */
    private void withBotOwner(@NotNull Consumer<DiscordUser> botOwnerConsumer) {
        CompletableFuture<DiscordUser> request = botOwnerRequest.get();
        if (request != null && !botOwnerTimeout.checkAndUpdate()) {
            request.whenComplete((user, t) -> botOwnerConsumer.accept(t != null ? null : user));
            return;
        }

        CompletableFuture<DiscordUser> future = instance.retrieveApplicationInfo()
                .timeout(10, TimeUnit.SECONDS)
                .map(applicationInfo -> (DiscordUser) new DiscordUserImpl(discordSRV, applicationInfo.getOwner()))
                .submit();

        botOwnerRequest.set(future);
        future.whenComplete((user, t) -> botOwnerConsumer.accept(t != null ? null : user));
    }

    @Subscribe(priority = EventPriority.EARLIEST)
    public void onPlaceholderLookup(PlaceholderLookupEvent event) {
        if (event.isProcessed()) {
            return;
        }

        // Map JDA objects to 1st party API objects
        Set<Object> newContext = new HashSet<>();
        boolean anyConverted = false;
        for (Object o : event.getContexts()) {
            Object converted;
            boolean isConversion = true;
            if (o instanceof PrivateChannel) {
                converted = new DiscordDMChannelImpl(discordSRV, (PrivateChannel) o);
            } else if (o instanceof TextChannel) {
                converted = new DiscordTextChannelImpl(discordSRV, (TextChannel) o);
            } else if (o instanceof Guild) {
                converted = new DiscordGuildImpl(discordSRV, (Guild) o);
            } else if (o instanceof Member) {
                converted = new DiscordGuildMemberImpl(discordSRV, (Member) o);
            } else if (o instanceof Role) {
                converted = new DiscordRoleImpl((Role) o);
            } else if (o instanceof ReceivedMessage) {
                converted = ReceivedDiscordMessageImpl.fromJDA(discordSRV, (Message) o);
            } else if (o instanceof User) {
                converted = new DiscordUserImpl(discordSRV, (User) o);
            } else {
                converted = o;
                isConversion = false;
            }
            if (isConversion) {
                anyConverted = true;
            }
            newContext.add(converted);
        }

        // Prevent infinite recursion
        if (anyConverted) {
            event.process(PlaceholderLookupResult.newLookup(event.getPlaceholder(), newContext));
        }
    }

    //
    // (Re)connecting & Shutting down
    //

    @Override
    public CompletableFuture<Void> connect() {
        if (connectionFuture != null && !connectionFuture.isDone()) {
            throw new IllegalStateException("Already connecting");
        } else if (instance != null && instance.getStatus() != JDA.Status.SHUTDOWN) {
            throw new IllegalStateException("Cannot reconnect, still active");
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(this::connectInternal, discordSRV.scheduler().executor());
        connectionFuture = future;
        return future;
    }

    @SuppressWarnings("BusyWait")
    private void connectInternal() {
        discordSRV.discordConnectionDetails().requestGatewayIntent(GatewayIntent.GUILD_MESSAGES); // TODO: figure out how DiscordSRV required intents are going to work
        discordSRV.discordConnectionDetails().requestGatewayIntent(GatewayIntent.GUILD_MEMBERS); // TODO: figure out how DiscordSRV required intents are going to work
        detailsAccepted = false;

        ConnectionConfig.Bot botConfig = discordSRV.connectionConfig().bot;
        DiscordConnectionDetails connectionDetails = discordSRV.discordConnectionDetails();
        Set<GatewayIntent> intents = connectionDetails.getGatewayIntents();
        boolean membersIntent = intents.contains(GatewayIntent.GUILD_MEMBERS);

        // Start with everything disabled & enable stuff that we actually need
        JDABuilder jdaBuilder = JDABuilder.createLight(botConfig.token, intents);
        jdaBuilder.enableCache(connectionDetails.getCacheFlags());
        jdaBuilder.setMemberCachePolicy(membersIntent ? MemberCachePolicy.ALL : MemberCachePolicy.OWNER);
        jdaBuilder.setChunkingFilter(membersIntent ? ChunkingFilter.ALL : ChunkingFilter.NONE);

        jdaBuilder.setEventManager(new EventManagerProxy(new JDAEventManager(discordSRV), discordSRV.scheduler().forkJoinPool()));

        jdaBuilder.setCallbackPool(discordSRV.scheduler().forkJoinPool());
        jdaBuilder.setGatewayPool(gatewayPool);
        jdaBuilder.setRateLimitPool(rateLimitPool);

        OkHttpClient.Builder httpBuilder = IOUtil.newHttpClientBuilder();
        // These 3 are 10 seconds by default
        httpBuilder.connectTimeout(20, TimeUnit.SECONDS);
        httpBuilder.readTimeout(20, TimeUnit.SECONDS);
        httpBuilder.writeTimeout(20, TimeUnit.SECONDS);
        jdaBuilder.setHttpClientBuilder(httpBuilder);

        WebSocketFactory webSocketFactory = new WebSocketFactory();
        jdaBuilder.setWebsocketFactory(webSocketFactory);

        int timeoutSeconds = 0;
        while (true) {
            try {
                instance = jdaBuilder.build();
                break;
            } catch (LoginException ignored) {
                invalidToken();
                break;
            } catch (Throwable t) {
                discordSRV.logger().error(t);
                // TODO
            }

            try {
                // Doubles the seconds. Min 2s, max 300s (5 minutes)
                timeoutSeconds = Math.min(Math.max(1, timeoutSeconds) * 2, 300);
                Thread.sleep(timeoutSeconds);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public CompletableFuture<Void> reconnect() {
        return CompletableFuture.runAsync(() -> {
            shutdown().join();
            connect().join();
        }, discordSRV.scheduler().executor());
    }

    @Subscribe(priority = EventPriority.LATE)
    public void onDSRVShuttingDown(DiscordSRVShuttingDownEvent event) {
        // This has a timeout
        shutdown().join();
    }

    @Override
    public CompletableFuture<Void> shutdown(long timeoutMillis) {
        return CompletableFuture.runAsync(() -> shutdownInternal(timeoutMillis), discordSRV.scheduler().executor());
    }

    @SuppressWarnings("BusyWait")
    private void shutdownInternal(long timeoutMillis) {
        if (instance == null) {
            shutdownExecutors();
            return;
        }

        instance.shutdown();

        try {
            discordSRV.scheduler().run(() -> {
                try {
                    while (instance.getStatus() != JDA.Status.SHUTDOWN) {
                        Thread.sleep(50);
                    }
                } catch (InterruptedException ignored) {}
            }).get(timeoutMillis, TimeUnit.MILLISECONDS);
            instance = null;
            shutdownExecutors();
        } catch (TimeoutException | ExecutionException e) {
            try {
                shutdownNow();
            } catch (Throwable t) {
                if (e instanceof ExecutionException) {
                    t.addSuppressed(e.getCause());
                }
                discordSRV.logger().error("Failed to shutdown JDA", t);
            }
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void shutdownNow() {
        if (instance != null) {
            instance.shutdownNow();
            instance = null;
        }
        shutdownExecutors();
    }

    private void shutdownExecutors() {
        gatewayPool.shutdownNow();
        rateLimitPool.shutdownNow();
    }

    //
    // Logging
    //

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        checkCode(event.getCloseCode());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
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
            Set<GatewayIntent> intents = discordSRV.discordConnectionDetails().getGatewayIntents();
            discordSRV.logger().error("+-------------------------------------->");
            discordSRV.logger().error("| Failed to connect to Discord:");
            discordSRV.logger().error("|");
            discordSRV.logger().error("| The Discord bot is lacking one or more");
            discordSRV.logger().error("| privileged intents listed below");
            discordSRV.logger().error("|");
            for (GatewayIntent intent : intents) {
                String displayName = PRIVILEGED_INTENTS.get(intent);
                if (displayName != null) {
                    discordSRV.logger().error("| " + displayName);
                }
            }
            discordSRV.logger().error("|");
            discordSRV.logger().error("| Instructions for enabling privileged gateway intents:");
            discordSRV.logger().error("| 1. Go to https://discord.com/developers/applications");
            discordSRV.logger().error("| 2. Choose the bot you are using for DiscordSRV");
            discordSRV.logger().error("|     - Keep in mind it will only be visible to the ");
            discordSRV.logger().error("|       Discord user who created the bot");
            discordSRV.logger().error("| 3. Go to the \"Bot\" tab");
            discordSRV.logger().error("| 4. Make sure the intents listed above are all enabled");
            discordSRV.logger().error("| 5. "); // TODO
            discordSRV.logger().error("+-------------------------------------->");
            return true;
        } else if (closeCode == CloseCode.AUTHENTICATION_FAILED) {
            invalidToken();
            return true;
        }
        return false;
    }

    private void invalidToken() {
        discordSRV.logger().error("+------------------------------>");
        discordSRV.logger().error("| Failed to connect to Discord:");
        discordSRV.logger().error("|");
        discordSRV.logger().error("| The token provided in the");
        discordSRV.logger().error("| " + ConnectionConfig.FILE_NAME + " is invalid");
        discordSRV.logger().error("|");
        discordSRV.logger().error("| You can get the token for your bot from:");
        discordSRV.logger().error("| https://discord.com/developers/applications");
        discordSRV.logger().error("| - Keep in mind the bot is only visible to");
        discordSRV.logger().error("|   the Discord user that created the bot");
        discordSRV.logger().error("+------------------------------>");
    }

    private class DefaultFailureCallback implements Consumer<Throwable> {

        @Override
        public void accept(Throwable t) {
            if ((t instanceof InterruptedIOException || t instanceof InterruptedException)
                    && discordSRV.status().isShutdown()) {
                // Ignore interrupted exceptions when DiscordSRV is shutting down or shutdown
                return;
            }

            boolean cancelled;
            if ((cancelled = t instanceof CancellationException) || t instanceof TimeoutException) {
                // Cancelling/timing out requests is always intentional
                discordSRV.logger().debug("A request " + (cancelled ? "was cancelled" : "timed out"), t.getCause());
            } else if (t instanceof RateLimitedException) {
                // Log route & retry after on warn & context on debug
                RateLimitedException exception = ((RateLimitedException) t);
                discordSRV.logger().warning("A request on route " + exception.getRateLimitedRoute()
                        + " was rate-limited for " + exception.getRetryAfter() + "ms");
                discordSRV.logger().debug(exception.getCause());
            } else if (t instanceof ErrorResponseException) {
                ErrorResponseException exception = (ErrorResponseException) t;
                if (exception.getErrorCode() == Response.ERROR_CODE) {
                    // There is no response due to a client error
                    Throwable cause = exception.getCause();
                    if (cause != null) {
                        // Run the cause through this method again
                        accept(cause);
                    } else {
                        discordSRV.logger().error("Failed to complete request for a unknown reason", exception);
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
                                discordSRV.logger().error("| The Discord bot's owner is " + user.getAsTag() + " (" + user.getId() + ")");
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
                            discordSRV.logger().debug("Failed to complete a request, Discord returned a server error (HTTP 500)");
                        }
                        // Log context to find what made the request
                        discordSRV.logger().debug(exception.getCause());
                        return;
                    }
                    default: break;
                }

                discordSRV.logger().error("Failed to complete a request: " + response.getMeaning());
                discordSRV.logger().debug(exception);
            }
        }
    }
}
