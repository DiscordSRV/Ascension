/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.bungee.config.main.BungeeConfig;
import com.discordsrv.bungee.console.BungeeConsole;
import com.discordsrv.bungee.module.BungeeChatModule;
import com.discordsrv.bungee.module.BungeeJoinModule;
import com.discordsrv.bungee.module.BungeeQuitModule;
import com.discordsrv.bungee.module.BungeeServerSwitchModule;
import com.discordsrv.bungee.player.BungeePlayerProvider;
import com.discordsrv.bungee.plugin.BungeePluginManager;
import com.discordsrv.common.AbstractDiscordSRV;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.configurate.manager.abstraction.ProxyConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.scheduler.StandardScheduler;
import com.discordsrv.common.core.debug.data.OnlineMode;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class BungeeDiscordSRV extends AbstractDiscordSRV<DiscordSRVBungeeBootstrap, BungeeConfig, ConnectionConfig, MessagesConfig> {

    private BungeeAudiences audiences;

    private final StandardScheduler scheduler;
    private final BungeeConsole console;
    private final BungeePlayerProvider playerProvider;
    private final BungeePluginManager pluginManager;
    private BungeeCommandHandler commandHandler;

    private final ConnectionConfigManager<ConnectionConfig> connectionConfigManager;
    private final MainConfigManager<BungeeConfig> configManager;
    private final MessagesConfigManager<MessagesConfig> messagesConfigManager;

    public BungeeDiscordSRV(DiscordSRVBungeeBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = new StandardScheduler(this);
        this.console = new BungeeConsole(this);
        this.playerProvider = new BungeePlayerProvider(this);
        this.pluginManager = new BungeePluginManager(this);

        // Config
        this.connectionConfigManager = new ConnectionConfigManager<>(this, ConnectionConfig::new);
        this.configManager = new ProxyConfigManager<>(this, BungeeConfig::new);
        this.messagesConfigManager = new MessagesConfigManager<>(this, MessagesConfig::new);

        load();
    }

    @Override
    protected void enable() throws Throwable {
        // Player related
        this.audiences = BungeeAudiences.create(bootstrap.getPlugin());

        this.commandHandler = new BungeeCommandHandler(this);

        super.enable();

        // Chat
        registerModule(BungeeChatModule::new);
        registerModule(BungeeJoinModule::new);
        registerModule(BungeeQuitModule::new);
        registerModule(BungeeServerSwitchModule::new);

        // Integrations
        registerIntegration("com.discordsrv.bungee.integration.BungeeLuckPermsIntegration");
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
        return connectionConfigManager;
    }

    @Override
    public MainConfigManager<BungeeConfig> configManager() {
        return configManager;
    }

    @Override
    public MessagesConfigManager<MessagesConfig> messagesConfigManager() {
        return messagesConfigManager;
    }
}
