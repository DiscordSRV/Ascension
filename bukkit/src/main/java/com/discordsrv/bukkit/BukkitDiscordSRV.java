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
import com.discordsrv.bukkit.command.game.handler.AbstractBukkitCommandHandler;
import com.discordsrv.bukkit.config.connection.BukkitConnectionConfig;
import com.discordsrv.bukkit.config.main.BukkitConfig;
import com.discordsrv.bukkit.config.manager.BukkitConfigManager;
import com.discordsrv.bukkit.config.manager.BukkitConnectionConfigManager;
import com.discordsrv.bukkit.console.BukkitConsole;
import com.discordsrv.bukkit.listener.BukkitChatListener;
import com.discordsrv.bukkit.listener.BukkitConnectionListener;
import com.discordsrv.bukkit.listener.BukkitDeathListener;
import com.discordsrv.bukkit.listener.BukkitStatusMessageListener;
import com.discordsrv.bukkit.player.BukkitPlayerProvider;
import com.discordsrv.bukkit.plugin.BukkitPluginManager;
import com.discordsrv.bukkit.scheduler.BukkitScheduler;
import com.discordsrv.common.ServerDiscordSRV;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.component.translation.Translation;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.debug.data.OnlineMode;
import com.discordsrv.common.messageforwarding.game.minecrafttodiscord.MinecraftToDiscordChatModule;
import com.discordsrv.common.plugin.PluginManager;
import com.fasterxml.jackson.databind.JsonNode;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Server;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class BukkitDiscordSRV extends ServerDiscordSRV<DiscordSRVBukkitBootstrap, BukkitConfig, BukkitConnectionConfig> {

    private BukkitAudiences audiences;

    private final BukkitScheduler scheduler;
    private final BukkitConsole console;
    private final BukkitPlayerProvider playerProvider;
    private final BukkitPluginManager pluginManager;
    private AbstractBukkitCommandHandler commandHandler;

    private final BukkitConnectionConfigManager connectionConfigManager;
    private final BukkitConfigManager configManager;

    public BukkitDiscordSRV(DiscordSRVBukkitBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = new BukkitScheduler(this);
        this.console = new BukkitConsole(this);
        this.playerProvider = new BukkitPlayerProvider(this);
        this.pluginManager = new BukkitPluginManager(this);

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

    private URL findResource(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = null;
        while (classLoader != null && url == null) {
            url = classLoader.getResource(name);
            classLoader = classLoader.getParent();
        }
        return url;
    }

    private void loadMCTranslations() {
        Map<String, Translation> translations = new HashMap<>();
        try {
            URL enUS = findResource("assets/minecraft/lang/en_US.lang");
            if (enUS == null) {
                enUS = findResource("assets/minecraft/lang/en_us.lang");
            }
            if (enUS != null) {
                Properties properties = new Properties();
                try (InputStream inputStream = enUS.openStream()) {
                    properties.load(inputStream);
                }

                properties.forEach((k, v) -> translations.put((String) k, Translation.stringFormat((String) v)));
            }
        } catch (Throwable t) {
            logger().debug("Failed to load locale", t);
        }
        try {
            URL enUS = findResource("assets/minecraft/lang/en_us.json");
            if (enUS != null) {
                JsonNode node = json().readTree(enUS);
                node.fields().forEachRemaining(entry -> translations.put(
                        entry.getKey(),
                        Translation.stringFormat(entry.getValue().textValue()))
                );
            }
        } catch (Throwable t) {
            logger().debug("Failed to load locale", t);
        }

        if (translations.isEmpty()) {
            logger().warning("No Minecraft translations were found, some components may not render correctly");
        } else {
            componentFactory().translationRegistry().register(Locale.US, translations);
            logger().debug("Found " + translations.size() + " Minecraft translations for en_us");
        }
    }

    @Override
    protected void enable() throws Throwable {
        // Service provider
        server().getServicesManager().register(DiscordSRVApi.class, this, plugin(), ServicePriority.Normal);

        // Adventure related stuff
        this.audiences = BukkitAudiences.create(bootstrap.getPlugin());
        loadMCTranslations();

        // Command handler
        commandHandler = AbstractBukkitCommandHandler.get(this);

        // Register listeners
        server().getPluginManager().registerEvents(BukkitChatListener.get(this), plugin());
        server().getPluginManager().registerEvents(new BukkitDeathListener(this), plugin());
        server().getPluginManager().registerEvents(new BukkitStatusMessageListener(this), plugin());

        // Modules
        registerModule(MinecraftToDiscordChatModule::new);

        // Integrations
        registerIntegration("com.discordsrv.bukkit.integration.VaultIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.PlaceholderAPIIntegration");

        // Chat Integrations
        registerIntegration("com.discordsrv.bukkit.integration.chat.ChattyChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.LunaChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.TownyChatIntegration");
        registerIntegration("com.discordsrv.bukkit.integration.chat.VentureChatIntegration");

        super.enable();

        // Connection listener
        server().getPluginManager().registerEvents(new BukkitConnectionListener(this), plugin());
    }

}
