/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.bungee.command.game.handler.BungeeCommandHandler;
import com.discordsrv.bungee.console.BungeeConsole;
import com.discordsrv.bungee.player.BungeePlayerProvider;
import com.discordsrv.bungee.plugin.BungeePluginManager;
import com.discordsrv.common.AbstractDiscordSRV;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.scheduler.StandardScheduler;
import com.discordsrv.common.feature.debug.data.OnlineMode;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class BungeeDiscordSRV extends AbstractDiscordSRV<DiscordSRVBungeeBootstrap, MainConfig, ConnectionConfig, MessagesConfig> {

    private BungeeAudiences audiences;

    private final StandardScheduler scheduler;
    private final BungeeConsole console;
    private final BungeePlayerProvider playerProvider;
    private final BungeePluginManager pluginManager;
    private BungeeCommandHandler commandHandler;

    public BungeeDiscordSRV(DiscordSRVBungeeBootstrap bootstrap) {
        super(bootstrap);

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
    public ServerType serverType() {
        return ServerType.PROXY;
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
    public PluginManager pluginManager() {
        return pluginManager;
    }

    @Override
    public OnlineMode onlineMode() {
        return OnlineMode.of(proxy().getConfig().isOnlineMode());
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
    public MessagesConfigManager<MessagesConfig> messagesConfigManager() {
        return null;
    }

    @Override
    protected void enable() throws Throwable {
        // Player related
        this.audiences = BungeeAudiences.create(bootstrap.getPlugin());

        this.commandHandler = new BungeeCommandHandler(this);

        super.enable();
    }
}
