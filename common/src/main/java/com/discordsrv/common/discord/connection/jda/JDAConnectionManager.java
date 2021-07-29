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

package com.discordsrv.common.discord.connection.jda;

import com.discordsrv.api.discord.connection.DiscordConnectionDetails;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.discord.connection.DiscordConnectionManager;
import com.discordsrv.common.scheduler.Scheduler;
import com.discordsrv.common.scheduler.threadfactory.CountingThreadFactory;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.requests.CloseCode;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.hooks.EventManagerProxy;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;

import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

public class JDAConnectionManager implements DiscordConnectionManager {

    private final DiscordSRV discordSRV;
    private final ScheduledExecutorService gatewayPool;
    private final ScheduledExecutorService rateLimitPool;

    private CompletableFuture<Void> connectionFuture;
    private JDA instance;
    private boolean detailsAccepted = true;

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
        RestAction.setDefaultFailure(t -> discordSRV.logger().error("Callback failed", t));
        AllowedMentions.setDefaultMentions(Collections.emptyList());
        discordSRV.eventBus().subscribe(this);
    }

    @Subscribe(priority = EventPriority.LATE)
    public void onDSRVShuttingDown(DiscordSRVShuttingDownEvent event) {
        // This has a timeout
        shutdown().join();
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

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (checkCode(event.getCloseCode())) {
            return;
        }

        boolean closedByServer = event.isClosedByServer();
        WebSocketFrame frame = closedByServer ? event.getServiceCloseFrame() : event.getClientCloseFrame();
        if (frame == null) {
            throw new IllegalStateException("Could not get the close frame for a disconnect");
        }

        if (closedByServer) {
            CloseCode closeCode = event.getCloseCode();
            String closeReason = frame.getCloseReason();

            discordSRV.logger().debug("[JDA] [Server] Disconnected due to "
                    + frame.getCloseCode() + ": "
                    + (closeCode != null
                        ? closeCode.getMeaning()
                        : (closeReason != null ? closeReason : "(Unknown close reason)")));
        } else {
            discordSRV.logger().debug("[JDA] [Client] Disconnected due to "
                    + frame.getCloseCode() + ": "
                    + frame.getCloseReason());
        }
    }

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        checkCode(event.getCloseCode());
    }

    private boolean checkCode(CloseCode closeCode) {
        if (closeCode == null) {
            return false;
        } else if (closeCode == CloseCode.DISALLOWED_INTENTS) {
            // TODO
            return true;
        } else if (closeCode.isReconnect()) {
            // TODO
            return true;
        }
        return false;
    }

    @Override
    public JDA instance() {
        return instance;
    }

    @Override
    public boolean areDetailsAccepted() {
        return detailsAccepted;
    }

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
        detailsAccepted = false;

        ConnectionConfig.Bot botConfig = discordSRV.connectionConfig().bot;
        DiscordConnectionDetails connectionDetails = discordSRV.discordConnectionDetails();
        Set<GatewayIntent> intents = connectionDetails.getGatewayIntents();
        boolean membersIntent = intents.contains(GatewayIntent.GUILD_MEMBERS);

        // Start with everything disabled & enable stuff that we actually need
        JDABuilder jdaBuilder = JDABuilder.createLight(botConfig.token, intents);
        jdaBuilder.enableCache(connectionDetails.getCacheFlags());
        jdaBuilder.setMemberCachePolicy(membersIntent ? MemberCachePolicy.ALL : MemberCachePolicy.OWNER);

        jdaBuilder.setEventManager(new EventManagerProxy(new JDAEventManager(discordSRV), discordSRV.scheduler().forkExecutor()));

        jdaBuilder.setCallbackPool(discordSRV.scheduler().forkExecutor());
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
                discordSRV.logger().error("+-------------------------------+");
                discordSRV.logger().error("| Failed to connect to Discord: |");
                discordSRV.logger().error("|                               |");
                discordSRV.logger().error("| The token provided in the     |");
                discordSRV.logger().error("| " + ConnectionConfig.FILE_NAME + " is invalid   |");
                discordSRV.logger().error("+-------------------------------+");
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
}
