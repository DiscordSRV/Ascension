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

package com.discordsrv.bukkit;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.bukkit.command.game.BukkitGameCommandExecutionHelper;
import com.discordsrv.bukkit.command.game.handler.AbstractBukkitCommandHandler;
import com.discordsrv.bukkit.component.translation.BukkitTranslationLoader;
import com.discordsrv.bukkit.config.connection.BukkitConnectionConfig;
import com.discordsrv.bukkit.config.main.BukkitConfig;
import com.discordsrv.bukkit.config.manager.BukkitConfigManager;
import com.discordsrv.bukkit.config.manager.BukkitConnectionConfigManager;
import com.discordsrv.bukkit.console.BukkitConsole;
import com.discordsrv.bukkit.listener.BukkitConnectionListener;
import com.discordsrv.bukkit.listener.BukkitDeathListener;
import com.discordsrv.bukkit.listener.BukkitRequiredLinkingListener;
import com.discordsrv.bukkit.listener.BukkitStatusMessageListener;
import com.discordsrv.bukkit.listener.award.BukkitAwardForwarder;
import com.discordsrv.bukkit.listener.chat.BukkitChatForwarder;
import com.discordsrv.bukkit.player.BukkitPlayerProvider;
import com.discordsrv.bukkit.plugin.BukkitPluginManager;
import com.discordsrv.bukkit.requiredlinking.BukkitRequiredLinkingModule;
import com.discordsrv.bukkit.scheduler.BukkitScheduler;
import com.discordsrv.bukkit.scheduler.FoliaScheduler;
import com.discordsrv.bukkit.scheduler.IBukkitScheduler;
import com.discordsrv.common.ServerDiscordSRV;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.debug.data.OnlineMode;
import com.discordsrv.common.messageforwarding.game.minecrafttodiscord.MinecraftToDiscordChatModule;
import com.discordsrv.common.plugin.PluginManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Server;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class BukkitDiscordSRV extends ServerDiscordSRV<DiscordSRVBukkitBootstrap, BukkitConfig, BukkitConnectionConfig> {

    private BukkitAudiences audiences;
    private BukkitTranslationLoader translationLoader;

    private final IBukkitScheduler scheduler;
    private final BukkitConsole console;
    private final BukkitPlayerProvider playerProvider;
    private final BukkitPluginManager pluginManager;
    private AbstractBukkitCommandHandler commandHandler;
    private final BukkitRequiredLinkingListener requiredLinkingListener;
    private final BukkitGameCommandExecutionHelper autoCompleteHelper;

    private final BukkitConnectionConfigManager connectionConfigManager;
    private final BukkitConfigManager configManager;

    private static IBukkitScheduler createScheduler(BukkitDiscordSRV discordSRV) {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return new FoliaScheduler(discordSRV);
        } catch (ClassNotFoundException ignored) {
            return new BukkitScheduler(discordSRV);
        }
    }

    public BukkitDiscordSRV(DiscordSRVBukkitBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = createScheduler(this);
        this.console = new BukkitConsole(this);
        this.playerProvider = new BukkitPlayerProvider(this);
        this.pluginManager = new BukkitPluginManager(this);

        // Config
        this.connectionConfigManager = new BukkitConnectionConfigManager(this);
        this.configManager = new BukkitConfigManager(this);

        load();

        this.requiredLinkingListener = new BukkitRequiredLinkingListener(this);
        this.autoCompleteHelper = new BukkitGameCommandExecutionHelper(this);
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
    public IBukkitScheduler scheduler() {
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
    public PluginManager pluginManager() {
        return pluginManager;
    }

    @Override
    public OnlineMode onlineMode() {
        try {
            Class<?> paperConfig = Class.forName("com.destroystokyo.paper.PaperConfig");
            Field velocitySupport = paperConfig.getField("velocitySupport");
            Field velocityOnlineMode = paperConfig.getField("velocityOnlineMode");

            if (velocitySupport.getBoolean(null) && velocityOnlineMode.getBoolean(null)) {
                return OnlineMode.VELOCITY;
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> spigotConfig = Class.forName("org.spigotmc.SpigotConfig");
            Field bungee = spigotConfig.getField("bungee");

            if (bungee.getBoolean(null)) {
                return OnlineMode.BUNGEE;
            }
        } catch (Throwable ignored) {}

        return OnlineMode.of(server().getOnlineMode());
    }

    @Override
    public ICommandHandler commandHandler() {
        return commandHandler;
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

        // Adventure related stuff
        this.audiences = BukkitAudiences.create(bootstrap.getPlugin());
        this.translationLoader = new BukkitTranslationLoader(this);

        // Command handler
        commandHandler = AbstractBukkitCommandHandler.get(this);

        // Modules
        registerModule(MinecraftToDiscordChatModule::new);
        registerModule(BukkitRequiredLinkingModule::new);

        // Integrations
        registerIntegration("com.discordsrv.bukkit.integration.VaultIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.PlaceholderAPIIntegration");

        // Chat Integrations
        registerIntegration("com.discordsrv.bukkit.integration.chat.ChattyChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.GriefPreventionChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.LunaChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.McMMOChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.TownyChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.VentureChatIntegration");

        super.enable();

        // Register listeners
        server().getPluginManager().registerEvents(BukkitAwardForwarder.get(this), plugin());
        server().getPluginManager().registerEvents(BukkitChatForwarder.get(this), plugin());
        server().getPluginManager().registerEvents(new BukkitDeathListener(this), plugin());
        server().getPluginManager().registerEvents(new BukkitStatusMessageListener(this), plugin());

        // Connection listener
        server().getPluginManager().registerEvents(new BukkitConnectionListener(this), plugin());
    }

    @Override
    public List<ReloadResult> reload(Set<ReloadFlag> flags, boolean initial) throws Throwable {
        List<ReloadResult> results = super.reload(flags, initial);

        if (flags.contains(ReloadFlag.TRANSLATIONS)) {
            translationLoader.reload();
        }

        return results;
    }

    @Override
    protected void disable() {
        super.disable();

        requiredLinkingListener.disable();
        audiences.close();
    }

    @Override
    public BukkitGameCommandExecutionHelper executeHelper() {
        return autoCompleteHelper;
    }
}
