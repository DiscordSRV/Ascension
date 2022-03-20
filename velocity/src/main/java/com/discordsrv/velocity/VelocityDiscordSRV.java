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

package com.discordsrv.velocity;

import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.debug.data.OnlineMode;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.plugin.PluginManager;
import com.discordsrv.common.scheduler.StandardScheduler;
import com.discordsrv.proxy.ProxyDiscordSRV;
import com.discordsrv.velocity.command.game.handler.VelocityCommandHandler;
import com.discordsrv.velocity.console.VelocityConsole;
import com.discordsrv.velocity.player.VelocityPlayerProvider;
import com.discordsrv.velocity.plugin.VelocityPluginManager;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class VelocityDiscordSRV extends ProxyDiscordSRV<MainConfig, ConnectionConfig> {

    private final Object plugin;
    private final ProxyServer proxyServer;
    private final PluginContainer pluginContainer;

    private final Logger logger;
    private final ClasspathAppender classpathAppender;
    private final Path dataDirectory;
    private final StandardScheduler scheduler;
    private final VelocityConsole console;
    private final VelocityPlayerProvider playerProvider;
    private final VelocityPluginManager pluginManager;
    private final VelocityCommandHandler commandHandler;

    public VelocityDiscordSRV(Object plugin, Logger logger, ClasspathAppender classpathAppender, ProxyServer proxyServer, PluginContainer pluginContainer, Path dataDirectory) {
        this.plugin = plugin;
        this.logger = logger;
        this.classpathAppender = classpathAppender;
        this.proxyServer = proxyServer;
        this.pluginContainer = pluginContainer;
        this.dataDirectory = dataDirectory;

        this.scheduler = new StandardScheduler(this);
        this.console = new VelocityConsole(this);
        this.playerProvider = new VelocityPlayerProvider(this);
        this.pluginManager = new VelocityPluginManager(this);
        this.commandHandler = new VelocityCommandHandler(this);

        load();
    }

    public Object plugin() {
        return plugin;
    }

    public PluginContainer container() {
        return pluginContainer;
    }

    public ProxyServer proxy() {
        return proxyServer;
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
    public StandardScheduler scheduler() {
        return scheduler;
    }

    @Override
    public VelocityConsole console() {
        return console;
    }

    @Override
    public @NotNull VelocityPlayerProvider playerProvider() {
        return playerProvider;
    }

    @Override
    public String version() {
        return container().getDescription().getVersion().orElseThrow(() -> new IllegalStateException("No version"));
    }

    @Override
    public PluginManager pluginManager() {
        return pluginManager;
    }

    @Override
    public OnlineMode onlineMode() {
        return OnlineMode.of(proxy().getConfiguration().isOnlineMode());
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
        super.enable();
    }
}
