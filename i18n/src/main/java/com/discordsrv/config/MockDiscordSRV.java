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

package com.discordsrv.config;

import com.discordsrv.api.discord.connection.DiscordConnectionDetails;
import com.discordsrv.api.event.bus.EventBus;
import com.discordsrv.common.DiscordSRV;
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
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MockDiscordSRV implements DiscordSRV {

    @Override
    public @NotNull Status status() {
        return null;
    }

    @Override
    public @NotNull EventBus eventBus() {
        return null;
    }

    @Override
    public @NotNull Optional<JDA> jda() {
        return Optional.empty();
    }

    @Override
    public @NotNull DiscordConnectionDetails discordConnectionDetails() {
        return null;
    }

    @Override
    public Logger logger() {
        return null;
    }

    @Override
    public Path dataDirectory() {
        return Paths.get("");
    }

    @Override
    public Scheduler scheduler() {
        return null;
    }

    @Override
    public Console console() {
        return null;
    }

    @Override
    public String version() {
        return null;
    }

    @Override
    public @NotNull ComponentFactory componentFactory() {
        return null;
    }

    @Override
    public @NotNull PlaceholderServiceImpl placeholderService() {
        return null;
    }

    @Override
    public @NotNull AbstractPlayerProvider<?> playerProvider() {
        return null;
    }

    @Override
    public @NotNull DiscordAPIImpl discordAPI() {
        return null;
    }

    @Override
    public ConnectionConfigManager<? extends ConnectionConfig> connectionConfigManager() {
        return null;
    }

    @Override
    public ConnectionConfig connectionConfig() {
        return null;
    }

    @Override
    public MainConfigManager<? extends MainConfig> configManager() {
        return null;
    }

    @Override
    public MainConfig config() {
        return null;
    }

    @Override
    public ChannelConfigHelper channelConfig() {
        return null;
    }

    @Override
    public DiscordConnectionManager discordConnectionManager() {
        return null;
    }

    @Override
    public <T extends Module> T getModule(Class<T> moduleType) {
        return null;
    }

    @Override
    public void registerModule(AbstractModule module) {

    }

    @Override
    public void unregisterModule(AbstractModule module) {

    }

    @Override
    public Locale locale() {
        return null;
    }

    @Override
    public void setStatus(Status status) {

    }

    @Override
    public CompletableFuture<Void> invokeEnable() {
        return null;
    }

    @Override
    public CompletableFuture<Void> invokeDisable() {
        return null;
    }

    @Override
    public CompletableFuture<Void> invokeReload() {
        return null;
    }
}
