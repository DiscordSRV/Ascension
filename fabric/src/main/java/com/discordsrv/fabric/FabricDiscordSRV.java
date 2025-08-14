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

package com.discordsrv.fabric;

import com.discordsrv.common.AbstractDiscordSRV;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutionHelper;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.configurate.manager.abstraction.ServerConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.scheduler.StandardScheduler;
import com.discordsrv.common.core.debug.data.OnlineMode;
import com.discordsrv.common.feature.messageforwarding.game.MinecraftToDiscordChatModule;
import com.discordsrv.fabric.command.game.FabricGameCommandExecutionHelper;
import com.discordsrv.fabric.command.game.handler.FabricCommandHandler;
import com.discordsrv.fabric.config.main.FabricConfig;
import com.discordsrv.fabric.console.FabricConsole;
import com.discordsrv.fabric.module.ban.FabricBanModule;
import com.discordsrv.fabric.module.chat.*;
import com.discordsrv.fabric.player.FabricPlayerProvider;
import com.discordsrv.fabric.plugin.FabricModManager;
import com.discordsrv.fabric.requiredlinking.FabricRequiredLinkingModule;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.util.jar.JarFile;

public class FabricDiscordSRV extends AbstractDiscordSRV<DiscordSRVFabricBootstrap, FabricConfig, ConnectionConfig, MessagesConfig> {

    private final StandardScheduler scheduler;
    private final FabricConsole console;
    private final FabricPlayerProvider playerProvider;
    private final FabricModManager modManager;
    private final FabricCommandHandler commandHandler;
    private final FabricComponentFactory componentFactory;

    private final ConnectionConfigManager<ConnectionConfig> connectionConfigManager;
    private final MainConfigManager<FabricConfig> configManager;
    private final MessagesConfigManager<MessagesConfig> messagesConfigManager;

    private final FabricGameCommandExecutionHelper executionHelper;

    public FabricDiscordSRV(DiscordSRVFabricBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = new StandardScheduler(this);
        this.console = new FabricConsole(this);
        this.playerProvider = new FabricPlayerProvider(this);
        this.modManager = new FabricModManager();
        this.commandHandler = new FabricCommandHandler(this);
        this.executionHelper = new FabricGameCommandExecutionHelper(this);
        this.componentFactory = new FabricComponentFactory(this);

        // Config
        this.connectionConfigManager = new ConnectionConfigManager<>(this, ConnectionConfig::new);
        this.configManager = new ServerConfigManager<>(this, FabricConfig::new);
        this.messagesConfigManager = new MessagesConfigManager<>(this, MessagesConfig::new);

        load();
    }

    @Override
    protected void enable() throws Throwable {
        super.enable();

        this.translationLoader = new FabricTranslationLoader(this);

        // Chat
        registerModule(MinecraftToDiscordChatModule::new);
        registerModule(FabricChatModule::new);
        registerModule(FabricDeathModule::new);
        registerModule(FabricJoinModule::new);
        registerModule(FabricQuitModule::new);
        registerModule(FabricAdvancementModule::new);

        // Required linking
        registerModule(FabricRequiredLinkingModule::new);

        // Punishments
        registerModule(FabricBanModule::new);

        // Integrations
        registerIntegration("com.discordsrv.fabric.integration.TextPlaceholderIntegration");
    }

    @Override
    protected URL getManifest() {
        // Referenced from ManifestUtil in Fabric API
        try {
            CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
            return URI.create("jar:" + codeSource.getLocation().toString() + "!/" + JarFile.MANIFEST_NAME).toURL();
        } catch (MalformedURLException e) {
            this.logger().error("Failed to get manifest URL", e);
            return null;
        }
    }

    public MinecraftServer getServer() {
        return bootstrap.getServer();
    }

    @Override
    public ServerType serverType() {
        return ServerType.SERVER;
    }

    @Override
    public StandardScheduler scheduler() {
        return scheduler;
    }

    @Override
    public FabricConsole console() {
        return console;
    }

    @Override
    public @NotNull FabricPlayerProvider playerProvider() {
        return playerProvider;
    }

    @Override
    public PluginManager pluginManager() {
        return modManager;
    }

    @Override
    public OnlineMode onlineMode() {
        return OnlineMode.of(getServer().isOnlineMode());
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
    public MainConfigManager<FabricConfig> configManager() {
        return configManager;
    }

    @Override
    public MessagesConfigManager<MessagesConfig> messagesConfigManager() {
        return messagesConfigManager;
    }

    @Override
    public @Nullable GameCommandExecutionHelper executeHelper() {
        return executionHelper;
    }

    @Override
    public @NotNull FabricComponentFactory componentFactory() {
        return componentFactory;
    }
}
