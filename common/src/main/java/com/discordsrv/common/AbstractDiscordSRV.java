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

package com.discordsrv.common;

import com.discordsrv.api.discord.connection.DiscordConnectionDetails;
import com.discordsrv.api.event.bus.EventBus;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReloadEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.module.type.Module;
import com.discordsrv.api.profile.IProfileManager;
import com.discordsrv.common.api.util.ApiInstanceUtil;
import com.discordsrv.common.channel.ChannelConfigHelper;
import com.discordsrv.common.channel.ChannelUpdaterModule;
import com.discordsrv.common.channel.GlobalChannelLookupModule;
import com.discordsrv.common.component.ComponentFactory;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.discord.api.DiscordAPIEventModule;
import com.discordsrv.common.discord.api.DiscordAPIImpl;
import com.discordsrv.common.discord.connection.DiscordConnectionManager;
import com.discordsrv.common.discord.connection.jda.JDAConnectionManager;
import com.discordsrv.common.discord.details.DiscordConnectionDetailsImpl;
import com.discordsrv.common.event.bus.EventBusImpl;
import com.discordsrv.common.function.CheckedFunction;
import com.discordsrv.common.function.CheckedRunnable;
import com.discordsrv.common.groupsync.GroupSyncModule;
import com.discordsrv.common.integration.LuckPermsIntegration;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import com.discordsrv.common.linking.impl.MemoryLinker;
import com.discordsrv.common.logging.adapter.DependencyLoggerAdapter;
import com.discordsrv.common.logging.impl.DependencyLoggingHandler;
import com.discordsrv.common.logging.impl.DiscordSRVLogger;
import com.discordsrv.common.messageforwarding.discord.DiscordChatMessageModule;
import com.discordsrv.common.messageforwarding.discord.DiscordMessageMirroringModule;
import com.discordsrv.common.messageforwarding.game.JoinMessageModule;
import com.discordsrv.common.messageforwarding.game.LeaveMessageModule;
import com.discordsrv.common.module.ModuleManager;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.placeholder.ComponentResultStringifier;
import com.discordsrv.common.placeholder.PlaceholderServiceImpl;
import com.discordsrv.common.placeholder.context.GlobalTextHandlingContext;
import com.discordsrv.common.profile.ProfileManager;
import com.discordsrv.common.storage.Storage;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
    private ProfileManager profileManager;
    private PlaceholderServiceImpl placeholderService;
    private ComponentFactory componentFactory;
    private DiscordAPIImpl discordAPI;
    private DiscordConnectionDetails discordConnectionDetails;

    // DiscordSRV
    private DiscordSRVLogger logger;
    private ModuleManager moduleManager;

    private Storage storage;
    private LinkProvider linkProvider;
    private ChannelConfigHelper channelConfig;
    private DiscordConnectionManager discordConnectionManager;

    // Internal
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    public AbstractDiscordSRV() {
        ApiInstanceUtil.setInstance(this);
    }

    /**
     * Method that should be called at the end of implementors constructors.
     */
    protected final void load() {
        this.logger = new DiscordSRVLogger(this);
        this.eventBus = new EventBusImpl(this);
        this.moduleManager = new ModuleManager(this);
        this.profileManager = new ProfileManager(this);
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
    public IProfileManager profileManager() {
        return profileManager;
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
    public DiscordSRVLogger logger() {
        return logger;
    }

    @Override
    public Storage storage() {
        return storage;
    }

    @Override
    public LinkProvider linkProvider() {
        return linkProvider;
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

    // Module

    @Override
    public <T extends Module> T getModule(Class<T> moduleType) {
        return moduleManager.getModule(moduleType);
    }

    @Override
    public void registerModule(AbstractModule<?> module) {
        moduleManager.register(module);
    }

    @SuppressWarnings("unchecked")
    protected <T extends DiscordSRV> void registerModule(CheckedFunction<T, AbstractModule<?>> function) {
        try {
            registerModule(function.apply((T) this));
        } catch (Throwable ignored) {}
    }

    @Override
    public void unregisterModule(AbstractModule<?> module) {
        moduleManager.unregister(module);
    }

    @Override
    public Locale locale() {
        // TODO: config
        return Locale.getDefault();
    }

    // Status

    @Override
    public void setStatus(Status status) {
        synchronized (this.status) {
            this.status.set(status);
            this.status.notifyAll();
        }
    }

    @Override
    public void waitForStatus(Status statusToWaitFor, long time, TimeUnit unit) throws InterruptedException {
        long deadline = time > 0 ? System.currentTimeMillis() + unit.toMillis(time) : Long.MAX_VALUE;
        while (status().ordinal() < statusToWaitFor.ordinal()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime > deadline) {
                break;
            }

            synchronized (this.status) {
                if (time > 0) {
                    this.status.wait(deadline - currentTime);
                } else {
                    this.status.wait();
                }
            }
        }
    }

    // Lifecycle

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

        // Logging
        DependencyLoggerAdapter.setAppender(new DependencyLoggingHandler(this));

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

        discordConnectionManager = new JDAConnectionManager(this);
        discordConnectionManager.connect().join();

        linkProvider = new MemoryLinker();
        ((LinkStore) linkProvider).link(UUID.fromString("6c983d46-0631-48b8-9baf-5e33eb5ffec4"), 185828288466255874L);

        // Placeholder result stringifiers & global contexts
        placeholderService().addResultMapper(new ComponentResultStringifier(this));
        placeholderService().addGlobalContext(new GlobalTextHandlingContext(this));

        // Register PlayerProvider listeners
        playerProvider().subscribe();

        registerModule(ChannelUpdaterModule::new);
        registerModule(GlobalChannelLookupModule::new);
        registerModule(DiscordAPIEventModule::new);
        registerModule(GroupSyncModule::new);
        registerModule(LuckPermsIntegration::new);
        registerModule(DiscordChatMessageModule::new);
        registerModule(DiscordMessageMirroringModule::new);
        registerModule(JoinMessageModule::new);
        registerModule(LeaveMessageModule::new);
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
    }

    @OverridingMethodsMustInvokeSuper
    protected void reload() {

    }
}
