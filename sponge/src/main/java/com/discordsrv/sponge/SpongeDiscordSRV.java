/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.common.ServerDiscordSRV;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.debug.data.OnlineMode;
import com.discordsrv.common.plugin.PluginManager;
import com.discordsrv.sponge.console.SpongeConsole;
import com.discordsrv.sponge.player.SpongePlayerProvider;
import com.discordsrv.sponge.plugin.SpongePluginManager;
import com.discordsrv.sponge.scheduler.SpongeScheduler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.plugin.PluginContainer;

public class SpongeDiscordSRV extends ServerDiscordSRV<DiscordSRVSpongeBootstrap, MainConfig, ConnectionConfig, MessagesConfig> {

    private final SpongeScheduler scheduler;
    private final SpongeConsole console;
    private final SpongePlayerProvider playerProvider;
    private final SpongePluginManager pluginManager;

    public SpongeDiscordSRV(DiscordSRVSpongeBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = new SpongeScheduler(this);
        this.console = new SpongeConsole(this);
        this.playerProvider = new SpongePlayerProvider(this);
        this.pluginManager = new SpongePluginManager(this);

        load();
    }

    public PluginContainer container() {
        return bootstrap.pluginContainer();
    }

    public Game game() {
        return bootstrap.game();
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
    public PluginManager pluginManager() {
        return pluginManager;
    }

    @Override
    public OnlineMode onlineMode() {
        // TODO: velocity / bungee

        return OnlineMode.of(game().server().isOnlineModeEnabled());
    }

    @Override
    public ICommandHandler commandHandler() {
        return bootstrap.commandHandler();
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
        // Service provider
        game().eventManager().registerListeners(container(), this);

        super.enable();
    }

    @Listener
    public void onServiceProvide(ProvideServiceEvent<DiscordSRVApi> event) {
        // Service provider
        event.suggest(() -> this);
    }
}
