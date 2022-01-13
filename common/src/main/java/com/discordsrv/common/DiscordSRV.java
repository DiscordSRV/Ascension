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

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.common.channel.ChannelConfigHelper;
import com.discordsrv.common.component.ComponentFactory;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.console.Console;
import com.discordsrv.common.discord.api.DiscordAPIImpl;
import com.discordsrv.common.discord.connection.DiscordConnectionManager;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.module.type.Module;
import com.discordsrv.common.placeholder.PlaceholderServiceImpl;
import com.discordsrv.common.player.provider.AbstractPlayerProvider;
import com.discordsrv.common.scheduler.Scheduler;
import com.discordsrv.common.logging.Logger;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public interface DiscordSRV extends DiscordSRVApi {

    // Platform
    Logger logger();
    Path dataDirectory();
    Scheduler scheduler();
    Console console();
    String version();

    // DiscordSRVApi
    @Override
    @NotNull
    ComponentFactory componentFactory();

    @Override
    @NotNull
    PlaceholderServiceImpl placeholderService();

    @Override
    @NotNull
    AbstractPlayerProvider<?> playerProvider();

    @Override
    @NotNull
    DiscordAPIImpl discordAPI();

    // Config
    ConnectionConfigManager<? extends ConnectionConfig> connectionConfigManager();
    ConnectionConfig connectionConfig();
    MainConfigManager<? extends MainConfig> configManager();
    MainConfig config();
    // Config helper
    ChannelConfigHelper channelConfig();

    // Internal
    DiscordConnectionManager discordConnectionManager();

    // Modules
    <T extends Module> T getModule(Class<T> moduleType);
    void registerModule(AbstractModule module);
    void unregisterModule(AbstractModule module);

    Locale locale();
    void setStatus(Status status);

    @SuppressWarnings("unchecked")
    @ApiStatus.NonExtendable
    default <K, V> Caffeine<K, V> caffeineBuilder() {
        return (Caffeine<K, V>) Caffeine.newBuilder()
                .executor(scheduler().forkJoinPool());
    }

    // Lifecycle
    CompletableFuture<Void> invokeEnable();
    CompletableFuture<Void> invokeDisable();
    CompletableFuture<Void> invokeReload();

}
