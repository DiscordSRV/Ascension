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

package com.discordsrv.bungee;

import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.abstraction.bootstrap.LifecycleManager;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.impl.JavaLoggerImpl;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.mcdependencydownload.bungee.bootstrap.BungeeBootstrap;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Path;

public class DiscordSRVBungeeBootstrap extends BungeeBootstrap implements IBootstrap {

    private final Logger logger;
    private final LifecycleManager lifecycleManager;
    private BungeeDiscordSRV discordSRV;

    // Don't change these parameters
    public DiscordSRVBungeeBootstrap(JarInJarClassLoader classLoader, Plugin plugin) throws IOException {
        super(classLoader, plugin);
        this.logger = new JavaLoggerImpl(plugin.getLogger());
        this.lifecycleManager = new LifecycleManager(
                logger,
                plugin.getDataFolder().toPath(),
                new String[] {"dependencies/runtimeDownload-bungee.txt"},
                getClasspathAppender()
        );
    }

    @Override
    public void onEnable() {
        lifecycleManager.loadAndEnable(() -> this.discordSRV = new BungeeDiscordSRV(this));
    }

    @Override
    public void onDisable() {
        lifecycleManager.disable(discordSRV);
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
        ProxyServer proxyServer = getPlugin().getProxy();
        return proxyServer.getName() + " version " + proxyServer.getVersion();
    }
}
