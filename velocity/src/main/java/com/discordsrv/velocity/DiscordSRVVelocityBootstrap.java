/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.velocity;

import com.discordsrv.common.dependency.InitialDependencyLoader;
import com.discordsrv.common.logging.logger.Logger;
import com.discordsrv.common.logging.logger.impl.SLF4JLoggerImpl;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.vankka.mcdependencydownload.velocity.classpath.VelocityClasspathAppender;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "discordsrv",
        name = "DiscordSRV",
        version = "@VERSION@",
        description = "",
        authors = {"Scarsz", "Vankka"},
        url = "https://discordsrv.com"
)
public class DiscordSRVVelocityBootstrap {

    private final Logger logger;
    private final InitialDependencyLoader dependencies;
    private final ProxyServer proxyServer;
    private final PluginContainer pluginContainer;
    private final Path dataDirectory;
    private VelocityDiscordSRV discordSRV;

    @Inject
    public DiscordSRVVelocityBootstrap(org.slf4j.Logger logger, ProxyServer proxyServer, PluginContainer pluginContainer, @DataDirectory Path dataDirectory) throws IOException {
        this.logger = new SLF4JLoggerImpl(logger);
        this.dependencies = new InitialDependencyLoader(
                this.logger,
                dataDirectory,
                new String[] {"dependencies/runtimeDownload-velocity.txt"},
                new VelocityClasspathAppender(this, proxyServer)
        );
        this.proxyServer = proxyServer;
        this.pluginContainer = pluginContainer;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Wait until dependencies ready, then initialize DiscordSRV
        dependencies.join();
        this.discordSRV = new VelocityDiscordSRV(this, logger, proxyServer, pluginContainer, dataDirectory);

        dependencies.runWhenComplete(discordSRV::invokeEnable);
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        dependencies.runWhenComplete(discordSRV::invokeReload);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        dependencies.runWhenComplete(discordSRV::invokeDisable);
    }

}
