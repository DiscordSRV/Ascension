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

package com.discordsrv.velocity;

import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.logging.logger.Logger;
import com.discordsrv.common.proxy.ProxyDiscordSRV;
import com.discordsrv.common.scheduler.StandardScheduler;
import com.discordsrv.velocity.console.VelocityConsole;
import com.discordsrv.velocity.player.VelocityPlayerProvider;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class VelocityDiscordSRV extends ProxyDiscordSRV<MainConfig, ConnectionConfig> {

    private final Object plugin;
    private final ProxyServer proxyServer;
    private final PluginContainer pluginContainer;

    private final Logger logger;
    private final Path dataDirectory;
    private final StandardScheduler scheduler;
    private final VelocityConsole console;
    private final VelocityPlayerProvider playerProvider;

    public VelocityDiscordSRV(Object plugin, Logger logger, ProxyServer proxyServer, PluginContainer pluginContainer, Path dataDirectory) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        this.scheduler = new StandardScheduler(this);
        this.console = new VelocityConsole(this);
        this.playerProvider = new VelocityPlayerProvider(this);

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
    public Logger logger() {
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
        return pluginContainer.getDescription().getVersion().orElseThrow(() -> new IllegalStateException("No version"));
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
