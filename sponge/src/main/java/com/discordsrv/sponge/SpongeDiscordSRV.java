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

package com.discordsrv.sponge;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.debug.data.OnlineMode;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.plugin.PluginManager;
import com.discordsrv.common.server.ServerDiscordSRV;
import com.discordsrv.sponge.command.game.handler.SpongeCommandHandler;
import com.discordsrv.sponge.console.SpongeConsole;
import com.discordsrv.sponge.player.SpongePlayerProvider;
import com.discordsrv.sponge.plugin.SpongePluginManager;
import com.discordsrv.sponge.scheduler.SpongeScheduler;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;

public class SpongeDiscordSRV extends ServerDiscordSRV<MainConfig, ConnectionConfig> {

    private final PluginContainer pluginContainer;
    private final Game game;

    private final Logger logger;
    private final ClasspathAppender classpathAppender;
    private final Path dataDirectory;
    private final SpongeScheduler scheduler;
    private final SpongeConsole console;
    private final SpongePlayerProvider playerProvider;
    private final SpongePluginManager pluginManager;
    private final SpongeCommandHandler commandHandler;

    public SpongeDiscordSRV(
            Logger logger,
            ClasspathAppender classpathAppender,
            Path dataDirectory,
            PluginContainer pluginContainer,
            Game game,
            SpongeCommandHandler commandHandler
    ) {
        this.logger = logger;
        this.classpathAppender = classpathAppender;
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
        this.game = game;

        this.scheduler = new SpongeScheduler(this);
        this.console = new SpongeConsole(this);
        this.playerProvider = new SpongePlayerProvider(this);
        this.pluginManager = new SpongePluginManager(this);
        this.commandHandler = commandHandler;

        load();
    }

    public PluginContainer container() {
        return pluginContainer;
    }

    public Game game() {
        return game;
    }

    @Override
    public Logger platformLogger() {
        return logger;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public SpongeScheduler scheduler() {
        return scheduler;
    }

    @Override
    public SpongeConsole console() {
        return console;
    }

    @Override
    public @NotNull SpongePlayerProvider playerProvider() {
        return playerProvider;
    }

    @Override
    public String version() {
        return pluginContainer.metadata().version().toString();
    }

    @Override
    public PluginManager pluginManager() {
        return pluginManager;
    }

    @Override
    public OnlineMode onlineMode() {
        // TODO: velocity / bungee

        return OnlineMode.of(game.server().isOnlineModeEnabled());
    }

    @Override
    public ClasspathAppender classpathAppender() {
        return classpathAppender;
    }

    @Override
    public ICommandHandler commandHandler() {
        return commandHandler;
    }

    @Override
    public ConnectionConfigManager<ConnectionConfig> connectionConfigManager() {
        return null;
    }

    @Override
    public MainConfigManager<MainConfig> configManager() {
        return null;
    }

    @Override
    protected void enable() throws Throwable {
        // Service provider
        game().eventManager().registerListeners(pluginContainer, this);

        super.enable();
    }

    @Listener
    public void onServiceProvide(ProvideServiceEvent<DiscordSRVApi> event) {
        // Service provider
        event.suggest(() -> this);
    }
}
