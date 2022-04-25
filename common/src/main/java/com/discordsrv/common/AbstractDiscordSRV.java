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
import com.discordsrv.api.event.events.lifecycle.DiscordSRVConnectedEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReadyEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReloadedEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.module.type.Module;
import com.discordsrv.common.api.util.ApiInstanceUtil;
import com.discordsrv.common.bootstrap.IBootstrap;
import com.discordsrv.common.channel.ChannelConfigHelper;
import com.discordsrv.common.channel.ChannelShutdownBehaviourModule;
import com.discordsrv.common.channel.ChannelUpdaterModule;
import com.discordsrv.common.channel.GlobalChannelLookupModule;
import com.discordsrv.common.command.game.GameCommandModule;
import com.discordsrv.common.component.ComponentFactory;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.LinkedAccountConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.dependency.DiscordSRVDependencyManager;
import com.discordsrv.common.discord.api.DiscordAPIEventModule;
import com.discordsrv.common.discord.api.DiscordAPIImpl;
import com.discordsrv.common.discord.connection.DiscordConnectionManager;
import com.discordsrv.common.discord.connection.jda.JDAConnectionManager;
import com.discordsrv.common.discord.details.DiscordConnectionDetailsImpl;
import com.discordsrv.common.event.bus.EventBusImpl;
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.function.CheckedFunction;
import com.discordsrv.common.function.CheckedRunnable;
import com.discordsrv.common.groupsync.GroupSyncModule;
import com.discordsrv.common.invite.DiscordInviteModule;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.impl.MemoryLinker;
import com.discordsrv.common.linking.impl.StorageLinker;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.adapter.DependencyLoggerAdapter;
import com.discordsrv.common.logging.impl.DependencyLoggingHandler;
import com.discordsrv.common.logging.impl.DiscordSRVLogger;
import com.discordsrv.common.messageforwarding.discord.DiscordChatMessageModule;
import com.discordsrv.common.messageforwarding.discord.DiscordMessageMirroringModule;
import com.discordsrv.common.messageforwarding.game.JoinMessageModule;
import com.discordsrv.common.messageforwarding.game.LeaveMessageModule;
import com.discordsrv.common.messageforwarding.game.StartMessageModule;
import com.discordsrv.common.messageforwarding.game.StopMessageModule;
import com.discordsrv.common.module.ModuleManager;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.placeholder.PlaceholderServiceImpl;
import com.discordsrv.common.placeholder.context.GlobalTextHandlingContext;
import com.discordsrv.common.placeholder.result.ComponentResultStringifier;
import com.discordsrv.common.profile.ProfileManager;
import com.discordsrv.common.storage.Storage;
import com.discordsrv.common.storage.StorageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * DiscordSRV's implementation's common code.
 * Implementations of this class must call {@link #load()} at the end of their constructors.
 * @param <C> the config type
 * @param <CC> the connections config type
 */
public abstract class AbstractDiscordSRV<B extends IBootstrap, C extends MainConfig, CC extends ConnectionConfig> implements DiscordSRV {

    private final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZED);
    private CompletableFuture<Void> enableFuture;

    // DiscordSRVApi
    private EventBusImpl eventBus;
    private ProfileManager profileManager;
    private PlaceholderServiceImpl placeholderService;
    private ComponentFactory componentFactory;
    private DiscordAPIImpl discordAPI;
    private DiscordConnectionDetails discordConnectionDetails;

    // DiscordSRV
    protected final B bootstrap;
    private final Logger platformLogger;
    private final Path dataDirectory;
    private DiscordSRVDependencyManager dependencyManager;
    private DiscordSRVLogger logger;
    private ModuleManager moduleManager;
    private JDAConnectionManager discordConnectionManager;
    private ChannelConfigHelper channelConfig;

    private Storage storage;
    private LinkProvider linkProvider;

    // Version
    private String version;
    private String gitRevision;
    private String gitBranch;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request original = chain.request();
                return chain.proceed(
                        original.newBuilder()
                                .removeHeader("User-Agent")
                                .addHeader("User-Agent", "DiscordSRV/" + version())
                                .build()
                );
            })
            .callTimeout(1, TimeUnit.MINUTES)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Internal
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    public AbstractDiscordSRV(B bootstrap) {
        ApiInstanceUtil.setInstance(this);
        this.bootstrap = bootstrap;
        this.platformLogger = bootstrap.logger();
        this.dataDirectory = bootstrap.dataDirectory();
    }

    /**
     * Method that should be called at the end of implementors constructors.
     */
    protected final void load() {
        this.dependencyManager = new DiscordSRVDependencyManager(this, bootstrap.lifecycleManager() != null ? bootstrap.lifecycleManager().getDependencyLoader() : null);
        this.logger = new DiscordSRVLogger(this);
        this.eventBus = new EventBusImpl(this);
        this.moduleManager = new ModuleManager(this);
        this.profileManager = new ProfileManager(this);
        this.placeholderService = new PlaceholderServiceImpl(this);
        this.componentFactory = new ComponentFactory(this);
        this.discordAPI = new DiscordAPIImpl(this);
        this.discordConnectionDetails = new DiscordConnectionDetailsImpl(this);
        this.discordConnectionManager = new JDAConnectionManager(this);
        this.channelConfig = new ChannelConfigHelper(this);
        readManifest();
    }

    protected URL getManifest() {
        return getClass().getClassLoader().getResource(JarFile.MANIFEST_NAME);
    }

    private void readManifest() {
        try {
            URL url = getManifest();
            if (url == null) {
                logger().error("Could not find manifest");
                return;
            }
            try (InputStream inputStream = url.openStream()) {
                Manifest manifest = new Manifest(inputStream);
                Attributes attributes = manifest.getMainAttributes();

                version = readAttribute(attributes, "Implementation-Version");
                if (version == null) {
                    logger().error("Failed to get version from manifest");
                }
                gitRevision = readAttribute(attributes, "Git-Commit");
                gitBranch = readAttribute(attributes, "Git-Branch");
            }
        } catch (IOException e) {
            logger().error("Failed to read manifest", e);
        }
    }

    private String readAttribute(Attributes attributes, String key) {
        return attributes.getValue(key);
    }

    // DiscordSRVApi

    @Override
    public final @NotNull Status status() {
        return status.get();
    }

    @Override
    public final @NotNull EventBusImpl eventBus() {
        return eventBus;
    }

    @Override
    public final @NotNull ProfileManager profileManager() {
        return profileManager;
    }

    @Override
    public final @NotNull PlaceholderServiceImpl placeholderService() {
        return placeholderService;
    }

    @Override
    public final @NotNull ComponentFactory componentFactory() {
        return componentFactory;
    }

    @Override
    public final @NotNull DiscordAPIImpl discordAPI() {
        return discordAPI;
    }

    @Override
    public final @NotNull Optional<JDA> jda() {
        return Optional.ofNullable(discordConnectionManager)
                .map(DiscordConnectionManager::instance);
    }

    @Override
    public final @NotNull DiscordConnectionDetails discordConnectionDetails() {
        return discordConnectionDetails;
    }

    // DiscordSRV

    @Override
    public final IBootstrap bootstrap() {
        return bootstrap;
    }

    @Override
    public final Logger platformLogger() {
        return platformLogger;
    }

    @Override
    public final DiscordSRVDependencyManager dependencyManager() {
        return dependencyManager;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public final DiscordSRVLogger logger() {
        return logger;
    }

    @Override
    public final Storage storage() {
        return storage;
    }

    @Override
    public final LinkProvider linkProvider() {
        return linkProvider;
    }

    @Override
    public final @NotNull String version() {
        return version;
    }

    @Override
    public final @Nullable String gitRevision() {
        return gitRevision;
    }

    @Override
    public final @Nullable String gitBranch() {
        return gitBranch;
    }

    @Override
    public final ChannelConfigHelper channelConfig() {
        return channelConfig;
    }

    @Override
    public final JDAConnectionManager discordConnectionManager() {
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
    protected final <T extends DiscordSRV> void registerModule(CheckedFunction<T, AbstractModule<?>> function) {
        moduleManager.registerModule((T) this, function);
    }

    /**
     * @param className a class which has a constructor with {@link DiscordSRV} (or implementation specific) as the only parameter.
     */
    protected final void registerIntegration(
            @Language(value = "JAVA", prefix = "class X{static{Class.forName(\"", suffix = "\");}}") String className
    ) {
        Object module;
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructors()[0];
            module = constructor.newInstance(this);
        } catch (Throwable e) {
            moduleManager.logger().debug("Failed to load integration: " + className, e);
            return;
        }
        moduleManager.registerModule(this, d -> (AbstractModule<?>) module);
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
        if (status == Status.CONNECTED) {
            eventBus().publish(new DiscordSRVConnectedEvent());
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

    @Override
    public OkHttpClient httpClient() {
        return httpClient;
    }

    @Override
    public ObjectMapper json() {
        return objectMapper;
    }

    // Lifecycle

    protected CompletableFuture<Void> invokeLifecycle(CheckedRunnable runnable) {
        return invokeLifecycle(() -> {
            try {
                lifecycleLock.lock();
                runnable.run();
            } finally {
                lifecycleLock.unlock();
            }
        }, "Failed to enable", true);
    }

    protected CompletableFuture<Void> invokeLifecycle(CheckedRunnable runnable, String message, boolean enable) {
        return CompletableFuture.runAsync(() -> {
            if (status().isShutdown()) {
                // Already shutdown/shutting down, don't bother
                return;
            }
            try {
                runnable.run();
            } catch (Throwable t) {
                if (status().isShutdown() && t instanceof NoClassDefFoundError) {
                    // Already shutdown, ignore errors for classes that already got unloaded
                    return;
                }
                if (enable) {
                    setStatus(Status.FAILED_TO_START);
                    disable();
                }
                logger().error(message, t);
            }
        }, scheduler().executorService());
    }

    @Override
    public final CompletableFuture<Void> invokeEnable() {
        return enableFuture = invokeLifecycle(() -> {
            this.enable();
            waitForStatus(Status.CONNECTED);
            eventBus().publish(new DiscordSRVReadyEvent());
        });
    }

    @Override
    public final CompletableFuture<Void> invokeDisable() {
        if (enableFuture != null && !enableFuture.isDone()) {
            logger().warning("Start cancelled");
            enableFuture.cancel(true);
        }
        return CompletableFuture.runAsync(this::disable, scheduler().executorService());
    }

    @Override
    public final CompletableFuture<Void> invokeReload(Set<ReloadFlag> flags, boolean silent) {
        return invokeLifecycle(() -> reload(flags, silent), "Failed to reload", false);
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

        // Register PlayerProvider listeners
        playerProvider().subscribe();

        // Placeholder result stringifiers & global contexts
        placeholderService().addResultMapper(new ComponentResultStringifier(this));
        placeholderService().addGlobalContext(new GlobalTextHandlingContext(this));

        // Modules
        registerModule(ChannelShutdownBehaviourModule::new);
        registerModule(ChannelUpdaterModule::new);
        registerModule(GameCommandModule::new);
        registerModule(GlobalChannelLookupModule::new);
        registerModule(DiscordAPIEventModule::new);
        registerModule(GroupSyncModule::new);
        registerModule(DiscordChatMessageModule::new);
        registerModule(DiscordMessageMirroringModule::new);
        registerModule(JoinMessageModule::new);
        registerModule(LeaveMessageModule::new);
        registerModule(DiscordInviteModule::new);

        // Integrations
        registerIntegration("com.discordsrv.common.integration.LuckPermsIntegration");

        // Initial load
        try {
            invokeReload(ReloadFlag.ALL, true).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    protected final void startedMessage() {
        registerModule(StartMessageModule::new);
        registerModule(StopMessageModule::new);
    }

    private StorageType getStorageType() {
        String backend = connectionConfig().storage.backend;
        switch (backend.toLowerCase(Locale.ROOT)) {
            case "h2": return StorageType.H2;
            case "mysql": return StorageType.MYSQL;
        }
        throw new StorageException("Unknown storage backend \"" + backend + "\"");
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
        eventBus().shutdown();
        this.status.set(Status.SHUTDOWN);
    }

    @OverridingMethodsMustInvokeSuper
    protected void reload(Set<ReloadFlag> flags, boolean initial) throws Throwable {
        if (!initial) {
            logger().info("Reloading DiscordSRV...");
        }

        if (flags.contains(ReloadFlag.CONFIG)) {
            try {
                connectionConfigManager().load();
                configManager().load();
            } catch (Throwable t) {
                setStatus(Status.FAILED_TO_LOAD_CONFIG);
                throw t;
            }

            channelConfig().reload();
        }

        if (flags.contains(ReloadFlag.LINKED_ACCOUNT_PROVIDER)) {
            LinkedAccountConfig linkedAccountConfig = config().linkedAccounts;
            if (linkedAccountConfig != null && linkedAccountConfig.enabled) {
                String provider = linkedAccountConfig.provider;
                switch (provider) {
                    case "auto":
                    case "storage":
                        linkProvider = new StorageLinker(this);
                        logger().info("Using storage for linked accounts");
                        break;
                    case "memory": {
                        linkProvider = new MemoryLinker();
                        logger().warning("Using memory for linked accounts");
                        logger().warning("Linked accounts will be lost upon restart");
                        break;
                    }
                    default: {
                        linkProvider = null;
                        logger().error("Unknown linked account provider: \"" + provider + "\", linked accounts will not be used");
                        break;
                    }
                }
            } else {
                linkProvider = null;
                logger().info("Linked accounts are disabled");
            }
        }

        if (flags.contains(ReloadFlag.STORAGE)) {
            if (storage != null) {
                storage.close();
            }

            try {
                try {
                    StorageType storageType = getStorageType();
                    logger().info("Using " + storageType.prettyName() + " as storage");
                    if (storageType.hikari()) {
                        dependencyManager().hikari().download().get();
                    }
                    storage = storageType.storageFunction().apply(this);
                    storage.initialize();
                    logger().info("Storage connection successfully established");
                } catch (ExecutionException e) {
                    throw new StorageException(e.getCause());
                } catch (StorageException e) {
                    throw e;
                } catch (Throwable t) {
                    throw new StorageException(t);
                }
            } catch (StorageException e) {
                e.log(this);
                logger().error("Failed to connect to storage");
                setStatus(Status.FAILED_TO_START);
                return;
            }
        }

        if (flags.contains(ReloadFlag.DISCORD_CONNECTION)) {
            try {
                if (discordConnectionManager.instance() != null) {
                    discordConnectionManager.reconnect().get();
                } else {
                    discordConnectionManager.connect().get();
                }
                if (!initial) {
                    waitForStatus(Status.CONNECTED, 20, TimeUnit.SECONDS);
                } else {
                    JDA jda = jda().orElse(null);
                    if (jda != null) {
                        try {
                            jda.awaitReady();
                        } catch (IllegalStateException ignored) {
                            // JDA shutdown -> don't continue
                            return;
                        }
                    }
                }
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }

        if (flags.contains(ReloadFlag.MODULES)) {
            moduleManager.reload();
        }

        if (!initial) {
            eventBus().publish(new DiscordSRVReloadedEvent(flags));
            logger().info("Reload complete.");
        }
    }
}
