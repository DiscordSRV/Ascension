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

package com.discordsrv.bukkit;

import com.discordsrv.common.abstraction.bootstrap.LifecycleManager;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.impl.JavaLoggerImpl;
import com.discordsrv.common.util.ReflectionUtil;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.mcdependencydownload.bukkit.bootstrap.BukkitBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused") // Used in BukkitLoader via reflection
public class DiscordSRVBukkitBootstrap extends BukkitBootstrap implements IBukkitBootstrap {

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

    private static List<String> getDependencyResources() {
        List<String> resources = new ArrayList<>();
        resources.add("dependencies/runtimeDownload-bukkit.txt");
        if (ReflectionUtil.classExists("com.mojang.brigadier.CommandDispatcher")) {
            resources.add("dependencies/commodore.txt");
        }

        return resources;
    }

    @Override
    public void onEnable() {
        lifecycleManager.loadAndEnable(() -> this.discordSRV = new BukkitDiscordSRVImpl(this));
        if (discordSRV == null) return;

        discordSRV.scheduler().runOnMainThreadLaterInTicks(() -> discordSRV.runServerStarted(), 1);
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
    public List<Runnable> mainThreadTasksForDisable() {
        return mainThreadTasksForDisable;
    }
}
