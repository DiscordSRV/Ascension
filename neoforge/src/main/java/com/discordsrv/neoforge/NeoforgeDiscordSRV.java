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

package com.discordsrv.neoforge;

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
import com.discordsrv.modded.config.main.ModdedConfig;
import com.discordsrv.neoforge.console.NeoforgeConsole;
import com.discordsrv.neoforge.game.NeoforgeGameCommandExecutionHelper;
import com.discordsrv.neoforge.game.handler.NeoforgeCommandHandler;
import com.discordsrv.neoforge.module.NeoforgeWorldChannelLookupModule;
import com.discordsrv.neoforge.module.ban.NeoforgeBanModule;
import com.discordsrv.neoforge.module.chat.*;
import com.discordsrv.neoforge.player.NeoforgePlayerProvider;
import com.discordsrv.neoforge.plugin.NeoforgeModManager;
import com.discordsrv.neoforge.requiredlinking.NeoforgeRequiredLinkingModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.util.UUID;
import java.util.jar.JarFile;

public class NeoforgeDiscordSRV extends AbstractDiscordSRV<DiscordSRVNeoforgeBootstrap, ModdedConfig, ConnectionConfig, MessagesConfig> {

    private final StandardScheduler scheduler;
    private final NeoforgeConsole console;
    private final NeoforgePlayerProvider playerProvider;
    private final NeoforgeModManager modManager;
    private final NeoforgeCommandHandler commandHandler;
    private final NeoforgeComponentFactory componentFactory;

    private final ConnectionConfigManager<ConnectionConfig> connectionConfigManager;
    private final MainConfigManager<ModdedConfig> configManager;
    private final MessagesConfigManager<MessagesConfig> messagesConfigManager;

    private final NeoforgeGameCommandExecutionHelper executionHelper;

    public NeoforgeDiscordSRV(DiscordSRVNeoforgeBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = new StandardScheduler(this);
        this.console = new NeoforgeConsole(this);
        this.playerProvider = new NeoforgePlayerProvider(this);
        this.modManager = new NeoforgeModManager();
        this.commandHandler = new NeoforgeCommandHandler(this);
        this.executionHelper = new NeoforgeGameCommandExecutionHelper(this);
        this.componentFactory = new NeoforgeComponentFactory(this);

        // Config
        this.connectionConfigManager = new ConnectionConfigManager<>(this, ConnectionConfig::new);
        this.configManager = new ServerConfigManager<>(this, ModdedConfig::new);
        this.messagesConfigManager = new MessagesConfigManager<>(this, MessagesConfig::new);

        load();
    }

    @Override
    protected void enable() throws Throwable {
        super.enable();

        // Chat
        registerModule(MinecraftToDiscordChatModule::new);
        registerModule(NeoforgeWorldChannelLookupModule::new);
        registerModule(NeoforgeChatModule::new);
        registerModule(NeoforgeDeathModule::new);
        registerModule(NeoforgeJoinModule::new);
        registerModule(NeoforgeQuitModule::new);
        registerModule(NeoforgeAdvancementModule::new);

        // Required linking
        registerModule(NeoforgeRequiredLinkingModule::new);

        // Punishments
        registerModule(NeoforgeBanModule::new);

        // Integrations
        registerIntegration("com.discordsrv.neoforge.integration.NeoforgeLuckPermsIntegration");
    }

    @Override
    protected URL getManifest() {
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
    public NeoforgeConsole console() {
        return console;
    }

    @Override
    public @NotNull NeoforgePlayerProvider playerProvider() {
        return playerProvider;
    }

    @Override
    public PluginManager pluginManager() {
        return modManager;
    }

    @Override
    public OnlineMode onlineMode() {
        return OnlineMode.of(getServer().usesAuthentication());
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
    public MainConfigManager<ModdedConfig> configManager() {
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
    public @NotNull NeoforgeComponentFactory componentFactory() {
        return componentFactory;
    }

    /**
     * Adapts to the {@link Identifier} changes introduced in 1.21.
     */
    public static Identifier id(String namespace, String path) {
        //? if <1.21 {
        /*return new Identifier(namespace, path);
         *///?} else
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    /**
     * Adapts to the {@link GameProfile} changes introduced in 1.21.9
     */
    public @NotNull UUID getIdFromGameProfile(GameProfile profile) {
        //? if minecraft: >=1.21.9 {
        return profile.id();
        //?} else {
        /*return profile.getId();
        *///?}
    }

    public @NotNull String getNameFromGameProfile(GameProfile profile) {
        //? if minecraft: >=1.21.9 {
        return profile.name();
        //?} else {
        /*return profile.getName();
        *///?}
    }
}
