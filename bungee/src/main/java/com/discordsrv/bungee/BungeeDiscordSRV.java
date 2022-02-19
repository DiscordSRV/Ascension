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

package com.discordsrv.bungee;

import com.discordsrv.bungee.console.BungeeConsole;
import com.discordsrv.bungee.player.BungeePlayerProvider;
import com.discordsrv.bungee.plugin.BungeePluginManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.debug.data.OnlineMode;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.plugin.PluginManager;
import com.discordsrv.common.scheduler.StandardScheduler;
import com.discordsrv.proxy.ProxyDiscordSRV;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class BungeeDiscordSRV extends ProxyDiscordSRV<MainConfig, ConnectionConfig> {

    private final DiscordSRVBungeeBootstrap bootstrap;
    private BungeeAudiences audiences;

    private final Logger logger;
    private final Path dataDirectory;
    private final StandardScheduler scheduler;
    private final BungeeConsole console;
    private final BungeePlayerProvider playerProvider;
    private final BungeePluginManager pluginManager;

    public BungeeDiscordSRV(DiscordSRVBungeeBootstrap bootstrap, Logger logger) {
        this.bootstrap = bootstrap;
        this.logger = logger;

        this.dataDirectory = bootstrap.getPlugin().getDataFolder().toPath();
        this.scheduler = new StandardScheduler(this);
        this.console = new BungeeConsole(this);
        this.playerProvider = new BungeePlayerProvider(this);
        this.pluginManager = new BungeePluginManager(this);

        load();
    }

    public Plugin plugin() {
        return bootstrap.getPlugin();
    }

    public ProxyServer proxy() {
        return plugin().getProxy();
    }

    public BungeeAudiences audiences() {
        return audiences;
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
    public BungeeConsole console() {
        return console;
    }

    @Override
    public @NotNull BungeePlayerProvider playerProvider() {
        return playerProvider;
    }

    @Override
    public String version() {
        return bootstrap.getPlugin().getDescription().getVersion();
    }

    @Override
    public PluginManager pluginManager() {
        return pluginManager;
    }

    @Override
    public OnlineMode onlineMode() {
        return OnlineMode.of(proxy().getConfig().isOnlineMode());
    }

    @Override
    public ClasspathAppender classpathAppender() {
        return bootstrap.getClasspathAppender();
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
        // Player related
        this.audiences = BungeeAudiences.create(bootstrap.getPlugin());

        super.enable();
    }
}
