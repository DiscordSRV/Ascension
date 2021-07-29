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
import org.slf4j.Logger;

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

    private final InitialDependencyLoader dependencies;
    private VelocityDiscordSRV discordSRV;

    @Inject
    public DiscordSRVVelocityBootstrap(Logger logger, ProxyServer proxyServer, PluginContainer pluginContainer, @DataDirectory Path dataDirectory) throws IOException {
        this.dependencies = new InitialDependencyLoader(
                dataDirectory,
                new String[] {"dependencies/runtimeDownloadApi-velocity.txt"},
                new VelocityClasspathAppender(this, proxyServer)
        );
        dependencies.whenComplete(() -> this.discordSRV = new VelocityDiscordSRV(this, pluginContainer, proxyServer, logger, dataDirectory));
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        dependencies.whenComplete(discordSRV::invokeEnable);
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        dependencies.whenComplete(discordSRV::invokeReload);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        dependencies.whenComplete(discordSRV::invokeDisable);
    }

}
