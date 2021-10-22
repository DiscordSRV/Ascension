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

package com.discordsrv.common;

import com.discordsrv.api.discord.connection.DiscordConnectionDetails;
import com.discordsrv.api.event.bus.EventBus;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReloadEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.api.util.ApiInstanceUtil;
import com.discordsrv.common.channel.ChannelConfigHelper;
import com.discordsrv.common.channel.DefaultGlobalChannel;
import com.discordsrv.common.component.ComponentFactory;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.discord.api.DiscordAPIImpl;
import com.discordsrv.common.discord.connection.DiscordConnectionManager;
import com.discordsrv.common.discord.connection.jda.JDAConnectionManager;
import com.discordsrv.common.discord.details.DiscordConnectionDetailsImpl;
import com.discordsrv.common.event.bus.EventBusImpl;
import com.discordsrv.common.function.CheckedRunnable;
import com.discordsrv.common.listener.ChannelLookupListener;
import com.discordsrv.common.listener.DiscordAPIListener;
import com.discordsrv.common.listener.DiscordChatListener;
import com.discordsrv.common.listener.GameChatListener;
import com.discordsrv.common.logging.DependencyLoggingFilter;
import com.discordsrv.common.logging.logger.backend.LoggingBackend;
import com.discordsrv.common.placeholder.ComponentResultStringifier;
import com.discordsrv.common.placeholder.PlaceholderServiceImpl;
import com.discordsrv.common.placeholder.context.GlobalTextHandlingContext;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DiscordSRV's implementation's common code.
 * Implementations of this class must call {@link #load()} at the end of their constructors.
 * @param <C> the config type
 * @param <CC> the connections config type
 */
public abstract class AbstractDiscordSRV<C extends MainConfig, CC extends ConnectionConfig> implements DiscordSRV {

    private final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZED);

    // DiscordSRVApi
    private EventBus eventBus;
    private PlaceholderServiceImpl placeholderService;
    private ComponentFactory componentFactory;
    private DiscordAPIImpl discordAPI;
    private DiscordConnectionDetails discordConnectionDetails;

    // DiscordSRV
    private final DefaultGlobalChannel defaultGlobalChannel = new DefaultGlobalChannel(this);
    private ChannelConfigHelper channelConfig;
    private DiscordConnectionManager discordConnectionManager;

    // Internal
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final DependencyLoggingFilter dependencyLoggingFilter = new DependencyLoggingFilter(this);

    public AbstractDiscordSRV() {
        ApiInstanceUtil.setInstance(this);
    }

    protected final void load() {
        this.eventBus = new EventBusImpl(this);
        this.placeholderService = new PlaceholderServiceImpl(this);
        this.componentFactory = new ComponentFactory(this);
        this.discordAPI = new DiscordAPIImpl(this);
        this.discordConnectionDetails = new DiscordConnectionDetailsImpl(this);
    }

    // DiscordSRVApi

    @Override
    public @NotNull Status status() {
        return status.get();
    }

    @Override
    public @NotNull EventBus eventBus() {
        return eventBus;
    }

    @Override
    public @NotNull PlaceholderServiceImpl placeholderService() {
        return placeholderService;
    }

    @Override
    public @NotNull ComponentFactory componentFactory() {
        return componentFactory;
    }

    @Override
    public @NotNull DiscordAPIImpl discordAPI() {
        return discordAPI;
    }

    @Override
    public @NotNull Optional<JDA> jda() {
        return Optional.ofNullable(discordConnectionManager)
                .map(DiscordConnectionManager::instance);
    }

    @Override
    public @NotNull DiscordConnectionDetails discordConnectionDetails() {
        return discordConnectionDetails;
    }

    // DiscordSRV

    @Override
    public DefaultGlobalChannel defaultGlobalChannel() {
        return defaultGlobalChannel;
    }

    @Override
    public ChannelConfigHelper channelConfig() {
        return channelConfig;
    }

    @Override
    public DiscordConnectionManager discordConnectionManager() {
        return discordConnectionManager;
    }

    // Config
    @Override
    public abstract ConnectionConfigManager<CC> connectionConfigManager();

    @Override
    public CC connectionConfig() {
        return connectionConfigManager().config();
    }

    @Override
    public abstract MainConfigManager<C> configManager();

    @Override
    public C config() {
        return configManager().config();
    }

    @Override
    public Locale locale() {
        // TODO: config
        return Locale.getDefault();
    }

    @Override
    public void setStatus(Status status) {
        this.status.set(status);
    }

    protected CompletableFuture<Void> invokeLifecycle(CheckedRunnable runnable, String message, boolean enable) {
        return invoke(() -> {
            try {
                lifecycleLock.lock();
                runnable.run();
            } finally {
                lifecycleLock.unlock();
            }
        }, message, enable);
    }

    protected CompletableFuture<Void> invoke(CheckedRunnable runnable, String message, boolean enable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                if (enable) {
                    setStatus(Status.FAILED_TO_START);
                    disable();
                }
                logger().error(message, t);
            }
        }, scheduler().executor());
    }

    @Override
    public final CompletableFuture<Void> invokeEnable() {
        return invokeLifecycle(this::enable, "Failed to enable", true);
    }

    @Override
    public final CompletableFuture<Void> invokeDisable() {
        return invokeLifecycle(this::disable, "Failed to disable", false);
    }

    @Override
    public final CompletableFuture<Void> invokeReload() {
        return invoke(this::reload, "Failed to reload", false);
    }

    @OverridingMethodsMustInvokeSuper
    protected void enable() throws Throwable {
        if (eventBus == null) {
            // Error that should only occur with new platforms
            throw new IllegalStateException("AbstractDiscordSRV#load was not called from the end of "
                    + getClass().getName() + " constructor");
        }

        // Config
        try {
            connectionConfigManager().load();
            configManager().load();

            // Utility
            channelConfig = new ChannelConfigHelper(this);

            eventBus().publish(new DiscordSRVReloadEvent(true));
        } catch (Throwable t) {
            setStatus(Status.FAILED_TO_LOAD_CONFIG);
            throw t;
        }

        // Logging
        LoggingBackend backend = console().loggingBackend();
        backend.addFilter(dependencyLoggingFilter);

        discordConnectionManager = new JDAConnectionManager(this);
        discordConnectionManager.connect().join();

        // Placeholder result stringifiers & global contexts
        placeholderService().addResultMapper(new ComponentResultStringifier(this));
        placeholderService().addGlobalContext(new GlobalTextHandlingContext(this));

        // Register PlayerProvider listeners
        playerProvider().subscribe();

        // Register listeners
        // DiscordAPI
        eventBus().subscribe(new DiscordAPIListener(this));
        // Chat
        eventBus().subscribe(new ChannelLookupListener(this));
        eventBus().subscribe(new GameChatListener(this));
        eventBus().subscribe(new DiscordChatListener(this));
    }

    @OverridingMethodsMustInvokeSuper
    protected void disable() {
        Status status = this.status.get();
        if (status == Status.INITIALIZED || status.isShutdown()) {
            // Hasn't started or already shutting down/shutdown
            return;
        }
        this.status.set(Status.SHUTTING_DOWN);
        eventBus().publish(new DiscordSRVShuttingDownEvent());

        // Logging
        LoggingBackend backend = console().loggingBackend();
        backend.removeFilter(dependencyLoggingFilter);
    }

    @OverridingMethodsMustInvokeSuper
    protected void reload() {

    }
}
