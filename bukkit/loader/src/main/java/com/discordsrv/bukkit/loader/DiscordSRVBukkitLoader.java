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

package com.discordsrv.bukkit.loader;

import com.google.common.graph.MutableGraph;
import dev.vankka.dependencydownload.jarinjar.loader.exception.LoadingException;
import dev.vankka.mcdependencydownload.bukkit.loader.BukkitLoader;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class DiscordSRVBukkitLoader extends BukkitLoader {

    private static final List<String> SOFT_DEPENDS = Arrays.asList(
            // Permissions
            "Vault",
            "LuckPerms",
            // Chat
            "Chatty",
            "GriefPrevention",
            "LunaChat",
            "McMMO",
            "TownyChat",
            "VentureChat",
            // Other
            "PlaceholderAPI",
            "Essentials",
            // Used by Adventure
            "ViaVersion"
    );

    @SuppressWarnings({"UnstableApiUsage", "unchecked"}) // Required
    private void addSoftDepends() {
        try {
            Class.forName("io.papermc.paper.plugin.storage.SimpleProviderStorage");
            // Paper circular dependency detection: don't add soft depends
            return;
        } catch (ClassNotFoundException ignored) {}

        // As far as DiscordSRV is concerned, the load order does not matter.
        // but Spigot's PluginClassLoader cares
        PluginDescriptionFile descriptionFile = getDescription();
        if (descriptionFile.getSoftDepend().contains("disable-auto-depend")) {
            return;
        }

        try {
            PluginManager pluginManager = getServer().getPluginManager();
            Class<?> pluginManagerClass = pluginManager.getClass();

            Field dependencyGraphField = pluginManagerClass.getDeclaredField("dependencyGraph");
            dependencyGraphField.setAccessible(true);
            Object dependencyGraph = dependencyGraphField.get(pluginManager);
            if (!(dependencyGraph instanceof MutableGraph<?>)) {
                return;
            }

            for (String dependency : SOFT_DEPENDS) {
                ((MutableGraph<String>) dependencyGraph).putEdge(descriptionFile.getName(), dependency);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public @NotNull String getBootstrapClassName() {
        return "com.discordsrv.bukkit.DiscordSRVBukkitBootstrap";
    }

    @Override
    public @NotNull URL getJarInJarResource() {
        addSoftDepends();

        URL resource = getClassLoader().getResource("bukkit.jarinjar");
        if (resource == null) {
            throw new IllegalStateException("Jar does not contain jarinjar");
        }
        return resource;
    }

    @Override
    public void handleLoadingException(LoadingException exception) {
        getLogger().logp(Level.SEVERE, null, null, exception.getCause(), exception::getMessage);
    }
}
