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

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.module.Module;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.player.provider.AbstractPlayerProvider;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import com.discordsrv.common.command.game.GameCommandExecutionHelper;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.component.ComponentFactory;
import com.discordsrv.common.core.dependency.DiscordSRVDependencyManager;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.impl.DiscordSRVLogger;
import com.discordsrv.common.core.module.ModuleManager;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.core.placeholder.PlaceholderServiceImpl;
import com.discordsrv.common.core.scheduler.Scheduler;
import com.discordsrv.common.core.storage.Storage;
import com.discordsrv.common.discord.api.DiscordAPIImpl;
import com.discordsrv.common.discord.connection.details.DiscordConnectionDetailsImpl;
import com.discordsrv.common.discord.connection.jda.JDAConnectionManager;
import com.discordsrv.common.feature.console.Console;
import com.discordsrv.common.feature.debug.data.OnlineMode;
import com.discordsrv.common.feature.debug.data.VersionInfo;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.profile.ProfileManager;
import com.discordsrv.common.helper.ChannelConfigHelper;
import com.discordsrv.common.helper.DestinationLookupHelper;
import com.discordsrv.common.helper.TemporaryLocalData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface DiscordSRV extends DiscordSRVApi {

    String WEBSITE = "https://discordsrv.vankka.dev";

    // Platform
    ServerType serverType();
    IBootstrap bootstrap();
    Logger platformLogger();
    Path dataDirectory();
    Scheduler scheduler();
    Console console();
    PluginManager pluginManager();
    OnlineMode onlineMode();
    DiscordSRVDependencyManager dependencyManager();
    ICommandHandler commandHandler();
    @NotNull AbstractPlayerProvider<?, ?> playerProvider();

    // DiscordSRVApi
    @Override
    @NotNull
    ComponentFactory componentFactory();

    @Override
    @NotNull
    ProfileManager profileManager();

    @Override
    @NotNull
    PlaceholderServiceImpl placeholderService();

    @Override
    @NotNull
    PlainPlaceholderFormat discordMarkdownFormat();

    @Override
    @NotNull
    DiscordAPIImpl discordAPI();

    // Logger
    DiscordSRVLogger logger();

    // Storage
    Storage storage();
    TemporaryLocalData temporaryLocalData();

    // Link Provider
    LinkProvider linkProvider();

    // Version
    @NotNull
    VersionInfo versionInfo();

    // Config
    ConnectionConfigManager<? extends ConnectionConfig> connectionConfigManager();
    ConnectionConfig connectionConfig();
    MainConfigManager<? extends MainConfig> configManager();
    MainConfig config();
    MessagesConfigManager<? extends MessagesConfig> messagesConfigManager();
    default MessagesConfig messagesConfig() {
        return messagesConfig((Locale) null);
    }
    default MessagesConfig.Minecraft messagesConfig(@Nullable ICommandSender sender) {
        return sender instanceof IPlayer ? messagesConfig((IPlayer) sender) : messagesConfig((Locale) null).minecraft;
    }
    default MessagesConfig.Minecraft messagesConfig(@Nullable IPlayer player) {
        return messagesConfig(player != null ? player.locale() : null).minecraft;
    }
    MessagesConfig messagesConfig(@Nullable Locale locale);

    // Config helper
    ChannelConfigHelper channelConfig();
    DestinationLookupHelper destinations();

    // Internal
    JDAConnectionManager discordConnectionManager();

    @NotNull DiscordConnectionDetailsImpl discordConnectionDetails();

    // Modules
    @Nullable
    <T extends Module> T getModule(Class<T> moduleType);
    void registerModule(AbstractModule<?> module);
    void unregisterModule(AbstractModule<?> module);
    ModuleManager moduleManager();

    Locale defaultLocale();

    // Status
    void setStatus(Status status);
    default void waitForStatus(Status status) throws InterruptedException {
        waitForStatus(status, -1, TimeUnit.MILLISECONDS);
    }
    void waitForStatus(Status status, long time, TimeUnit unit) throws InterruptedException;

    @SuppressWarnings("unchecked")
    @ApiStatus.NonExtendable
    default <K, V> Caffeine<K, V> caffeineBuilder() {
        return (Caffeine<K, V>) Caffeine.newBuilder()
                .executor(scheduler().executorService());
    }
    OkHttpClient httpClient();
    ObjectMapper json();

    // Lifecycle
    void runEnable();
    List<ReloadResult> runReload(Set<ReloadFlag> flags, boolean silent);
    CompletableFuture<Void> invokeDisable();
    boolean isServerStarted();
    ZonedDateTime getInitializeTime();

    @Nullable
    default GameCommandExecutionHelper executeHelper() {
        return null;
    }

    enum ServerType {
        SERVER,
        PROXY
    }

}
