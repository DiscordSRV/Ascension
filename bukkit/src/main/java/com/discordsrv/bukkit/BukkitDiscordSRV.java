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

package com.discordsrv.bukkit;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.bukkit.config.connection.BukkitConnectionConfig;
import com.discordsrv.bukkit.config.main.BukkitConfig;
import com.discordsrv.bukkit.config.manager.BukkitConfigManager;
import com.discordsrv.bukkit.config.manager.BukkitConnectionConfigManager;
import com.discordsrv.bukkit.console.BukkitConsole;
import com.discordsrv.bukkit.listener.BukkitChatListener;
import com.discordsrv.bukkit.listener.BukkitDeathListener;
import com.discordsrv.bukkit.listener.BukkitStatusMessageListener;
import com.discordsrv.bukkit.player.BukkitPlayerProvider;
import com.discordsrv.bukkit.scheduler.BukkitScheduler;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.server.ServerDiscordSRV;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Server;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class BukkitDiscordSRV extends ServerDiscordSRV<BukkitConfig, BukkitConnectionConfig> {

    private final DiscordSRVBukkitBootstrap bootstrap;
    private BukkitAudiences audiences;

    private final Logger logger;
    private final Path dataDirectory;
    private final BukkitScheduler scheduler;
    private final BukkitConsole console;
    private final BukkitPlayerProvider playerProvider;

    private final BukkitConnectionConfigManager connectionConfigManager;
    private final BukkitConfigManager configManager;

    public BukkitDiscordSRV(DiscordSRVBukkitBootstrap bootstrap, Logger logger) {
        this.bootstrap = bootstrap;
        this.logger = logger;

        this.dataDirectory = bootstrap.getPlugin().getDataFolder().toPath();
        this.scheduler = new BukkitScheduler(this);
        this.console = new BukkitConsole(this);
        this.playerProvider = new BukkitPlayerProvider(this);

        // Config
        this.connectionConfigManager = new BukkitConnectionConfigManager(this);
        this.configManager = new BukkitConfigManager(this);

        load();
    }

    public JavaPlugin plugin() {
        return bootstrap.getPlugin();
    }

    public Server server() {
        return plugin().getServer();
    }

    public BukkitAudiences audiences() {
        return audiences;
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
    public BukkitScheduler scheduler() {
        return scheduler;
    }

    @Override
    public BukkitConsole console() {
        return console;
    }

    @Override
    public @NotNull BukkitPlayerProvider playerProvider() {
        return playerProvider;
    }

    @Override
    public String version() {
        return bootstrap.getPlugin().getDescription().getVersion();
    }

    @Override
    public ConnectionConfigManager<BukkitConnectionConfig> connectionConfigManager() {
        return connectionConfigManager;
    }

    @Override
    public MainConfigManager<BukkitConfig> configManager() {
        return configManager;
    }

    @Override
    protected void enable() throws Throwable {
        // Service provider
        server().getServicesManager().register(DiscordSRVApi.class, this, plugin(), ServicePriority.Normal);

        // Player related
        this.audiences = BukkitAudiences.create(bootstrap.getPlugin());

        super.enable();

        // Register listeners
        server().getPluginManager().registerEvents(BukkitChatListener.get(this), plugin());
        server().getPluginManager().registerEvents(new BukkitDeathListener(this), plugin());
        server().getPluginManager().registerEvents(new BukkitStatusMessageListener(this), plugin());
    }
}
