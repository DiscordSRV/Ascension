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

package com.discordsrv.sponge;

import com.discordsrv.common.dependency.InitialDependencyLoader;
import com.discordsrv.logging.Logger;
import com.discordsrv.logging.impl.Log4JLoggerImpl;
import com.discordsrv.sponge.bootstrap.ISpongeBootstrap;
import dev.vankka.mcdependencydownload.bootstrap.AbstractBootstrap;
import dev.vankka.mcdependencydownload.bootstrap.classpath.JarInJarClasspathAppender;
import dev.vankka.mcdependencydownload.classloader.JarInJarClassLoader;
import org.spongepowered.api.Game;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings("unused") // Reflection
public class DiscordSRVSpongeBootstrap extends AbstractBootstrap implements ISpongeBootstrap {

    private final Logger logger;
    private final InitialDependencyLoader dependencies;
    private SpongeDiscordSRV discordSRV;

    private final PluginContainer pluginContainer;
    private final Game game;
    private final JarInJarClassLoader classLoader;
    private final Path dataDirectory;

    public DiscordSRVSpongeBootstrap(PluginContainer pluginContainer, Game game, JarInJarClassLoader classLoader, Path dataDirectory) throws IOException {
        // Don't change these parameters
        super(classLoader);
        this.logger = new Log4JLoggerImpl(pluginContainer.logger());
        this.dependencies = new InitialDependencyLoader(
                logger,
                dataDirectory,
                new String[] {"dependencies/runtimeDownload-sponge.txt"},
                new JarInJarClasspathAppender(classLoader)
        );
        this.pluginContainer = pluginContainer;
        this.game = game;
        this.classLoader = classLoader;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void onConstruct() {
        // Wait until dependencies ready, then initialize DiscordSRV
        dependencies.join();
    }

    @Override
    public void onStarted() {
        this.discordSRV = new SpongeDiscordSRV(logger, pluginContainer, game, classLoader, dataDirectory);

        dependencies.runWhenComplete(discordSRV::invokeEnable);
        dependencies.runWhenComplete(discordSRV::invokeServerStarted);
    }

    @Override
    public void onRefresh() {
        dependencies.runWhenComplete(discordSRV::invokeReload);
    }

    @Override
    public void onStopping() {
        dependencies.runWhenComplete(discordSRV::invokeDisable);
    }
}
