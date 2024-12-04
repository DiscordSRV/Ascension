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

package com.discordsrv.bukkit;

import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.abstraction.bootstrap.LifecycleManager;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.impl.JavaLoggerImpl;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.mcdependencydownload.bukkit.bootstrap.BukkitBootstrap;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DiscordSRVBukkitBootstrap extends BukkitBootstrap implements IBootstrap {

    private final Logger logger;
    private final LifecycleManager lifecycleManager;
    private BukkitDiscordSRV discordSRV;
    private final List<Runnable> mainThreadTasksForDisable = new ArrayList<>();

    // Don't change these parameters
    public DiscordSRVBukkitBootstrap(JarInJarClassLoader classLoader, JavaPlugin plugin) throws IOException {
        super(classLoader, plugin);
        this.logger = new JavaLoggerImpl(plugin.getLogger());
        this.lifecycleManager = new LifecycleManager(
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
            // CommandDispatcher not present, don't need to bother downloading commodore
        }

        return resources.toArray(new String[0]);
    }

    @Override
    public void onEnable() {
        lifecycleManager.loadAndEnable(() -> this.discordSRV = new BukkitDiscordSRV(this));
        if (discordSRV == null) return;

        discordSRV.scheduler().runOnMainThreadLaterInTicks(() -> discordSRV.invokeServerStarted(), 1);
    }

    @Override
    public void onDisable() {
        lifecycleManager.disable(discordSRV);

        // Run tasks on the main thread (scheduler cannot be used when disabling)
        for (Runnable runnable : mainThreadTasksForDisable) {
            runnable.run();
        }
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public ClasspathAppender classpathAppender() {
        return getClasspathAppender();
    }

    @Override
    public ClassLoader classLoader() {
        return getClassLoader();
    }

    @Override
    public LifecycleManager lifecycleManager() {
        return lifecycleManager;
    }

    @Override
    public Path dataDirectory() {
        return getPlugin().getDataFolder().toPath();
    }

    @Override
    public String platformVersion() {
        Server server = getPlugin().getServer();
        return server.getName() + " version " + server.getVersion() + " (implementation version " + server.getBukkitVersion() + ")";
    }

    public List<Runnable> mainThreadTasksForDisable() {
        return mainThreadTasksForDisable;
    }
}
