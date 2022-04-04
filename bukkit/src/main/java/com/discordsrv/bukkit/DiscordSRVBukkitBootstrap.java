/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.common.dependency.InitialDependencyLoader;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.backend.impl.JavaLoggerImpl;
import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.mcdependencydownload.bukkit.bootstrap.BukkitBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DiscordSRVBukkitBootstrap extends BukkitBootstrap {

    private final Logger logger;
    private final InitialDependencyLoader dependencies;
    private BukkitDiscordSRV discordSRV;

    public DiscordSRVBukkitBootstrap(JarInJarClassLoader classLoader, JavaPlugin plugin) throws IOException {
        // Don't change these parameters
        super(classLoader, plugin);
        this.logger = new JavaLoggerImpl(plugin.getLogger());
        this.dependencies = new InitialDependencyLoader(
                logger,
                plugin.getDataFolder().toPath(),
                getDependencyResources(),
                getClasspathAppender()
        );
    }

    private static String[] getDependencyResources() {
        List<String> resources = new ArrayList<>();
        resources.add("dependencies/runtimeDownload-bukkit.txt");

        try {
            Class.forName("com.mojang.brigadier.CommandDispatcher");
            resources.add("dependencies/commodore.txt");
        } catch (ClassNotFoundException ignored) {
            // CommandDispatches not present, don't need to bother downloading commodore
        }

        return resources.toArray(new String[0]);
    }

    @Override
    public void onEnable() {
        dependencies.loadAndEnable(() -> this.discordSRV = new BukkitDiscordSRV(this, logger));
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> discordSRV.invokeServerStarted(), 1L);
    }

    @Override
    public void onDisable() {
        dependencies.disable(discordSRV);
    }
}
