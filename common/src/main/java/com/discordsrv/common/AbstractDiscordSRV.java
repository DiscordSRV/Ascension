/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.events.lifecycle.DiscordSRVConnectedEvent;
import com.discordsrv.api.events.lifecycle.DiscordSRVReadyEvent;
import com.discordsrv.api.events.lifecycle.DiscordSRVReloadedEvent;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.module.Module;
import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.command.discord.DiscordCommandModule;
import com.discordsrv.common.command.game.GameCommandModule;
import com.discordsrv.common.command.game.commands.subcommand.reload.ReloadResults;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigSingleManager;
import com.discordsrv.common.config.connection.BotConfig;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.connection.UpdateConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.linking.LinkedAccountConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.component.ComponentFactory;
import com.discordsrv.common.core.dependency.DiscordSRVDependencyManager;
import com.discordsrv.common.core.eventbus.EventBusImpl;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.impl.DependencyLoggingHandler;
import com.discordsrv.common.core.logging.impl.DiscordSRVLogger;
import com.discordsrv.common.core.module.ModuleManager;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.core.placeholder.PlaceholderServiceImpl;
import com.discordsrv.common.core.placeholder.context.GlobalDateFormattingContext;
import com.discordsrv.common.core.placeholder.context.GlobalTextHandlingContext;
import com.discordsrv.common.core.placeholder.format.DiscordMarkdownFormatImpl;
import com.discordsrv.common.core.placeholder.result.ComponentResultStringifier;
import com.discordsrv.common.core.storage.Storage;
import com.discordsrv.common.core.storage.StorageType;
import com.discordsrv.common.core.storage.impl.MemoryStorage;
import com.discordsrv.common.discord.api.DiscordAPIEventModule;
import com.discordsrv.common.discord.api.DiscordAPIImpl;
import com.discordsrv.common.discord.connection.details.DiscordConnectionDetailsImpl;
import com.discordsrv.common.discord.connection.jda.JDAConnectionManager;
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.feature.DiscordInviteModule;
import com.discordsrv.common.feature.PresenceUpdaterModule;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import com.discordsrv.common.feature.channel.ChannelLockingModule;
import com.discordsrv.common.feature.channel.TimedUpdaterModule;
import com.discordsrv.common.feature.channel.global.GlobalChannelLookupModule;
import com.discordsrv.common.feature.console.ConsoleModule;
import com.discordsrv.common.feature.debug.data.VersionInfo;
import com.discordsrv.common.feature.groupsync.GroupSyncModule;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.LinkingModule;
import com.discordsrv.common.feature.linking.impl.MinecraftAuthenticationLinker;
import com.discordsrv.common.feature.linking.impl.StorageLinker;
import com.discordsrv.common.feature.mention.MentionGameRenderingModule;
import com.discordsrv.common.feature.messageforwarding.discord.DiscordChatMessageModule;
import com.discordsrv.common.feature.messageforwarding.discord.DiscordMessageMirroringModule;
import com.discordsrv.common.feature.messageforwarding.game.JoinMessageModule;
import com.discordsrv.common.feature.messageforwarding.game.LeaveMessageModule;
import com.discordsrv.common.feature.messageforwarding.game.StartMessageModule;
import com.discordsrv.common.feature.messageforwarding.game.StopMessageModule;
import com.discordsrv.common.feature.mention.MentionCachingModule;
import com.discordsrv.common.feature.profile.ProfileManager;
import com.discordsrv.common.feature.update.UpdateChecker;
import com.discordsrv.common.helper.ChannelConfigHelper;
import com.discordsrv.common.helper.DestinationLookupHelper;
import com.discordsrv.common.logging.adapter.DependencyLoggerAdapter;
import com.discordsrv.common.util.ApiInstanceUtil;
import com.discordsrv.common.util.UUIDUtil;
import com.discordsrv.common.util.function.CheckedFunction;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * DiscordSRV's implementation's common code.
 * Implementations of this class must call {@link #load()} at the end of their constructors.
 * @param <C> the config type
 * @param <CC> the connections config type
 */
public abstract class AbstractDiscordSRV<
        B extends IBootstrap,
        C extends MainConfig,
        CC extends ConnectionConfig,
        MC extends MessagesConfig
> implements DiscordSRV {

    private final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZED);
    private final AtomicReference<Boolean> beenReady = new AtomicReference<>(false);

    // DiscordSRVApi
    private EventBusImpl eventBus;
    private ProfileManager profileManager;
    private PlaceholderServiceImpl placeholderService;
    private DiscordMarkdownFormatImpl discordMarkdownFormat;
    private ComponentFactory componentFactory;
    private DiscordAPIImpl discordAPI;
    private DiscordConnectionDetailsImpl discordConnectionDetails;

    // DiscordSRV
    protected final B bootstrap;
    private final Logger platformLogger;
    private final Path dataDirectory;
    private DiscordSRVDependencyManager dependencyManager;
    private DiscordSRVLogger logger;
    private ModuleManager moduleManager;
    private JDAConnectionManager discordConnectionManager;
    private ChannelConfigHelper channelConfig;
    private DestinationLookupHelper destinationLookupHelper;

    private Storage storage;
    private LinkProvider linkProvider;

    // Version
    private UpdateChecker updateChecker;
    protected VersionInfo versionInfo;

    private OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        this.discordMarkdownFormat = new DiscordMarkdownFormatImpl();
        this.componentFactory = new ComponentFactory(this);
        this.discordAPI = new DiscordAPIImpl(this);
        this.discordConnectionDetails = new DiscordConnectionDetailsImpl(this);
        this.discordConnectionManager = new JDAConnectionManager(this);
        this.channelConfig = new ChannelConfigHelper(this);
        this.destinationLookupHelper = new DestinationLookupHelper(this);
        this.updateChecker = new UpdateChecker(this);
        readManifest();

        ///////////////////////////////////////////////////////////////
        logger.warning("");
        logger.warning("+-----------------------------------------+");
        logger.warning("This is a testing version of DiscordSRV");
        logger.warning("Limited or no support will be provided");
        logger.warning("EVERYTHING is subject to change.");
        logger.warning("+-----------------------------------------+");
        logger.warning("");
        ///////////////////////////////////////////////////////////////

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(20); // Set maximum amount of requests at a time (to something more reasonable than 64)
        dispatcher.setMaxRequestsPerHost(16); // Most requests are to discord.com

        ConnectionPool connectionPool = new ConnectionPool(5, 10, TimeUnit.SECONDS);

        this.httpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String host = original.url().host();
                    boolean isDiscord = host.matches("(.*\\.|^)(?:discord\\.(?:com|gg)|(discordapp\\.com))");

                    String userAgent = isDiscord
                                       ? "DiscordBot (https://github.com/DiscordSRV/DiscordSRV, " + versionInfo().version() + ")"
                                               + " (" + JDAInfo.GITHUB + ", " + JDAInfo.VERSION + ")"
                                       : "DiscordSRV/" + versionInfo().version();

                    return chain.proceed(
                            original.newBuilder()
                                    .removeHeader("User-Agent")
                                    .addHeader("User-Agent", userAgent)
                                    .build()
                    );
                })
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    protected URL getManifest() {
        ClassLoader classLoader = bootstrap.classLoader();
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).findResource(JarFile.MANIFEST_NAME);
        }

        return classLoader.getResource(JarFile.MANIFEST_NAME);
    }

    private void readManifest() {
        String version = bootstrap.getClass().getPackage().getImplementationVersion();
        String gitCommit = null, gitBranch = null, buildTime = null;

        try {
            URL url = getManifest();
            if (url == null) {
                logger().error("Could not find manifest");
                return;
            }
            try (InputStream inputStream = url.openStream()) {
                Manifest manifest = new Manifest(inputStream);
                Attributes attributes = manifest.getMainAttributes();

                if (version == null) {
                    version = readAttribute(attributes, "Implementation-Version");
                    if (version == null) {
                        logger().error("Failed to get version from manifest");
                    }
                }

                gitCommit = readAttribute(attributes, "Git-Commit");
                gitBranch = readAttribute(attributes, "Git-Branch");
                buildTime = readAttribute(attributes, "Build-Time");
            }
        } catch (IOException e) {
            logger().error("Failed to read manifest", e);
        }

        versionInfo = new VersionInfo(version, gitCommit, gitBranch, buildTime);
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
    public final @NotNull DiscordMarkdownFormatImpl discordMarkdownFormat() {
        return discordMarkdownFormat;
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
    public final @Nullable JDA jda() {
        if (discordConnectionManager == null) {
            return null;
        }
        return discordConnectionManager.instance();
    }

    @Override
    public final @NotNull DiscordConnectionDetailsImpl discordConnectionDetails() {
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
    public final @NotNull VersionInfo versionInfo() {
        return versionInfo;
    }

    @Override
    public final ChannelConfigHelper channelConfig() {
        return channelConfig;
    }

    @Override
    public DestinationLookupHelper destinations() {
        return destinationLookupHelper;
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

    @Override
    public abstract MessagesConfigManager<MC> messagesConfigManager();

    @Override
    public MC messagesConfig(@Nullable Locale locale) {
        MessagesConfigSingleManager<MC> manager = locale != null ? messagesConfigManager().getManager(locale) : null;
        if (manager == null) {
            manager = messagesConfigManager().getManager(defaultLocale());
        }
        if (manager == null) {
            manager = messagesConfigManager().getManager(Locale.US);
        }
        return manager.config();
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
            String suffix = "";
            if (e instanceof LinkageError || e instanceof ClassNotFoundException) {
                suffix = " (Integration likely not installed or using wrong version)";
            }
            moduleManager.logger().debug("Failed to load integration: " + className + suffix, e);
            return;
        }
        moduleManager.registerModule(this, d -> (AbstractModule<?>) module);
    }

    @Override
    public void unregisterModule(AbstractModule<?> module) {
        moduleManager.unregister(module);
    }

    @Override
    public ModuleManager moduleManager() {
        return moduleManager;
    }

    @Override
    public Locale defaultLocale() {
        MainConfig config = config();
        if (config != null) {
            String defaultLanguage = config.messages.defaultLanguage;
            if (StringUtils.isNotBlank(defaultLanguage)) {
                return Locale.forLanguageTag(defaultLanguage);
            }
        }

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
            synchronized (beenReady) {
                if (!beenReady.get()) {
                    eventBus.publish(new DiscordSRVReadyEvent());
                    beenReady.set(true);
                }
            }
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

    @Override
    public final void runEnable() {
        try {
            this.enable();
        } catch (Throwable t) {
            logger.error("Failed to enable", t);
            setStatus(Status.FAILED_TO_START);
        }
    }

    @Override
    public final CompletableFuture<Void> invokeDisable() {
        return scheduler().execute(this::disable);
    }

    @Override
    public final List<ReloadResult> runReload(Set<ReloadFlag> flags, boolean silent) {
        try {
            return reload(flags, silent);
        } catch (Throwable e) {
            if (silent) {
                throw new RuntimeException(e);
            } else {
                logger.error("Failed to reload", e);
            }
            return Collections.singletonList(ReloadResults.FAILED);
        }
    }

    @MustBeInvokedByOverriders
    protected void enable() throws Throwable {
        if (eventBus == null) {
            // Error that should only occur with new platforms
            throw new IllegalStateException("AbstractDiscordSRV#load was not called from the end of "
                    + getClass().getName() + " constructor");
        }

        // Logging
        DependencyLoggerAdapter.setAppender(new DependencyLoggingHandler(this));

        // Placeholder result stringifiers & global contexts
        placeholderService().addResultMapper(new ComponentResultStringifier(this));
        placeholderService().addGlobalContext(new GlobalTextHandlingContext(this));
        placeholderService().addGlobalContext(new GlobalDateFormattingContext(this));
        placeholderService().addGlobalContext(UUIDUtil.class);

        // Modules
        registerModule(BanSyncModule::new);
        registerModule(ConsoleModule::new);
        registerModule(ChannelLockingModule::new);
        registerModule(TimedUpdaterModule::new);
        registerModule(DiscordCommandModule::new);
        registerModule(GameCommandModule::new);
        registerModule(GlobalChannelLookupModule::new);
        registerModule(DiscordAPIEventModule::new);
        registerModule(GroupSyncModule::new);
        registerModule(DiscordChatMessageModule::new);
        registerModule(DiscordMessageMirroringModule::new);
        registerModule(JoinMessageModule::new);
        registerModule(LeaveMessageModule::new);
        registerModule(DiscordInviteModule::new);
        registerModule(MentionCachingModule::new);
        registerModule(LinkingModule::new);
        registerModule(PresenceUpdaterModule::new);
        registerModule(MentionGameRenderingModule::new);

        // Integrations
        registerIntegration("com.discordsrv.common.integration.LuckPermsIntegration");

        // Initial load
        try {
            runReload(ReloadFlag.ALL, true);
        } catch (RuntimeException e) {
            throw e.getCause();
        }

        // Register PlayerProvider listeners
        playerProvider().subscribe();
    }

    protected final void startedMessage() {
        registerModule(StartMessageModule::new);
        registerModule(StopMessageModule::new);
        Optional.ofNullable(getModule(PresenceUpdaterModule.class)).ifPresent(PresenceUpdaterModule::serverStarted);
    }

    private StorageType getStorageType() {
        String backend = connectionConfig().storage.backend;
        switch (backend.toLowerCase(Locale.ROOT)) {
            case "h2": return StorageType.H2;
            case "mysql": return StorageType.MYSQL;
            case "mariadb": return StorageType.MARIADB;
        }
        if (backend.equals(MemoryStorage.IDENTIFIER)) {
            return StorageType.MEMORY;
        }
        throw new StorageException("Unknown storage backend \"" + backend + "\"");
    }

    @MustBeInvokedByOverriders
    protected void disable() {
        Status status = this.status.get();
        if (status == Status.INITIALIZED || status.isShutdown()) {
            // Hasn't started or already shutting down/shutdown
            return;
        }
        this.status.set(Status.SHUTTING_DOWN);
        eventBus().publish(new DiscordSRVShuttingDownEvent());
        eventBus().shutdown();
        try {
            if (storage != null) {
                storage.close();
            }
        } catch (Throwable t) {
            logger().error("Failed to close storage connection", t);
        }
        this.status.set(Status.SHUTDOWN);
    }

    @MustBeInvokedByOverriders
    public List<ReloadResult> reload(Set<ReloadFlag> flags, boolean initial) throws Throwable {
        if (!initial) {
            logger().info("Reloading DiscordSRV...");
        }

        if (flags.contains(ReloadFlag.CONFIG)) {
            try {
                connectionConfigManager().load();
                configManager().load();
                messagesConfigManager().load();

                channelConfig().reload();
            } catch (Throwable t) {
                if (initial) {
                    setStatus(Status.FAILED_TO_LOAD_CONFIG);
                }
                throw t;
            }
        }

        List<ReloadResult> results = new ArrayList<>();
        // Reload any modules that can be enabled before DiscordSRV is ready
        if (initial) {
            results.addAll(moduleManager().reload());
        }

        if (connectionConfig().bot.token.equals(BotConfig.DEFAULT_TOKEN)) {
            if (initial) {
                logger().info("");
                logger().info("Welcome to DiscordSRV!");
                logger().info("");
                logger().info("To get started with using DiscordSRV please configure a bot token, instructions will be printed below");
                logger().info("You can review and/or disable external services DiscordSRV uses in the " + ConnectionConfig.FILE_NAME + " before adding a bot token");
                logger().info("");
            }
            discordConnectionManager.invalidToken(true);
            results.add(ReloadResults.DEFAULT_BOT_TOKEN);
            return results;
        }

        // Update check
        UpdateConfig updateConfig = connectionConfig().update;
        if (updateConfig.security.enabled) {
            if (updateChecker.isSecurityFailed()) {
                // Security has already failed
                return Collections.singletonList(ReloadResults.SECURITY_FAILED);
            }

            if (initial && !updateChecker.check(true)) {
                // Security failed cancel startup & shutdown
                invokeDisable();
                return Collections.singletonList(ReloadResults.SECURITY_FAILED);
            }
        } else if (initial) {
            // Not using security, run update check off thread
            scheduler().run(() -> updateChecker.check(true));
        }
        if (initial) {
            scheduler().runAtFixedRate(() -> updateChecker.check(false), Duration.ofHours(6));
        }

        if (flags.contains(ReloadFlag.STORAGE)) {
            if (storage != null) {
                storage.close();
            }

            try {
                try {
                    StorageType storageType = getStorageType();
                    logger().info("Using " + storageType.prettyName() + " as storage");
                    if (storageType == StorageType.MEMORY) {
                        logger().warning("Using memory as storage backend.");
                        logger().warning("Data will not persist across server restarts.");
                    }
                    if (storageType.hikari()) {
                        dependencyManager().hikari().downloadRelocateAndLoad().get();
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
                if (initial) {
                    setStatus(Status.FAILED_TO_START);
                }
                return Collections.singletonList(ReloadResults.STORAGE_CONNECTION_FAILED);
            }
        }

        if (flags.contains(ReloadFlag.LINKED_ACCOUNT_PROVIDER)) {
            LinkedAccountConfig linkedAccountConfig = config().linkedAccounts;
            if (linkedAccountConfig != null && linkedAccountConfig.enabled) {
                LinkedAccountConfig.Provider provider = linkedAccountConfig.provider;
                boolean permitMinecraftAuth = connectionConfig().minecraftAuth.allow;
                if (provider == LinkedAccountConfig.Provider.AUTO) {
                    provider = permitMinecraftAuth && onlineMode().isOnline() ? LinkedAccountConfig.Provider.MINECRAFTAUTH : LinkedAccountConfig.Provider.STORAGE;
                }
                switch (provider) {
                    case MINECRAFTAUTH:
                        if (!permitMinecraftAuth) {
                            linkProvider = null;
                            logger().error("minecraftauth.me is disabled in the " + ConnectionConfig.FILE_NAME + ", "
                                                   + "but linked-accounts.provider is set to \"minecraftauth\". Linked accounts will be disabled");
                            break;
                        }
                        dependencyManager.mcAuthLib().downloadRelocateAndLoad().get();
                        linkProvider = new MinecraftAuthenticationLinker(this);
                        logger().info("Using minecraftauth.me for linked accounts");
                        break;
                    case STORAGE:
                        linkProvider = new StorageLinker(this);
                        logger().info("Using storage for linked accounts");
                        break;
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

        if (flags.contains(ReloadFlag.DISCORD_CONNECTION)) {
            try {
                if (discordConnectionManager.instance() != null) {
                    discordConnectionManager.reconnect().get();
                } else {
                    discordConnectionManager.connect().get();
                }
                if (!initial) {
                    waitForStatus(Status.CONNECTED, 20, TimeUnit.SECONDS);
                    if (status() != Status.CONNECTED) {
                        return Collections.singletonList(ReloadResults.DISCORD_CONNECTION_FAILED);
                    }
                } else {
                    JDA jda = jda();
                    if (jda != null) {
                        try {
                            jda.awaitReady();
                        } catch (IllegalStateException ignored) {
                            // JDA shutdown -> don't continue
                            return Collections.singletonList(ReloadResults.DISCORD_CONNECTION_FAILED);
                        }
                    }
                }
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }

        // Modules are reloaded upon DiscordSRV being ready, thus not needed at initial
        if (!initial && flags.contains(ReloadFlag.MODULES)) {
            results.addAll(moduleManager.reload());
        }

        if (flags.contains(ReloadFlag.DISCORD_COMMANDS)) {
            discordAPI().commandRegistry().registerCommandsFromEvent();
            discordAPI().commandRegistry().registerCommandsToDiscord();
        }

        if (!initial) {
            eventBus().publish(new DiscordSRVReloadedEvent(flags));
            logger().info("Reload complete.");
        }

        results.add(ReloadResults.SUCCESS);
        return results;
    }
}
