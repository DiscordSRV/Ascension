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

package com.discordsrv.velocity;

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
import com.discordsrv.velocity.command.game.handler.VelocityCommandHandler;
import com.discordsrv.velocity.console.VelocityConsole;
import com.discordsrv.velocity.player.VelocityPlayerProvider;
import com.discordsrv.velocity.plugin.VelocityPluginManager;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class VelocityDiscordSRV extends AbstractDiscordSRV<DiscordSRVVelocityBootstrap, MainConfig, ConnectionConfig, MessagesConfig> {

    private final StandardScheduler scheduler;
    private final VelocityConsole console;
    private final VelocityPlayerProvider playerProvider;
    private final VelocityPluginManager pluginManager;
    private final VelocityCommandHandler commandHandler;

    public VelocityDiscordSRV(DiscordSRVVelocityBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = new StandardScheduler(this);
        this.console = new VelocityConsole(this);
        this.playerProvider = new VelocityPlayerProvider(this);
        this.pluginManager = new VelocityPluginManager(this);
        this.commandHandler = new VelocityCommandHandler(this);

        load();
    }

    @Override
    protected URL getManifest() {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).findResource(JarFile.MANIFEST_NAME);
        } else {
            throw new IllegalStateException("Class not loaded by a URLClassLoader, unable to get manifest");
        }
    }

    public Object plugin() {
        return bootstrap;
    }

    public PluginContainer container() {
        return bootstrap.pluginContainer();
    }

    public ProxyServer proxy() {
        return bootstrap.proxyServer();
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
    public VelocityConsole console() {
        return console;
    }

    @Override
    public @NotNull VelocityPlayerProvider playerProvider() {
        return playerProvider;
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
        super.enable();
    }
}
