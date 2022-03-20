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

package com.discordsrv.sponge;

import com.discordsrv.common.dependency.InitialDependencyLoader;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.backend.impl.Log4JLoggerImpl;
import com.discordsrv.sponge.bootstrap.ISpongeBootstrap;
import com.discordsrv.sponge.command.game.handler.SpongeCommandHandler;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
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
    private final ClasspathAppender classpathAppender;
    private final InitialDependencyLoader dependencies;
    private SpongeDiscordSRV discordSRV;
    private SpongeCommandHandler commandHandler;

    private final PluginContainer pluginContainer;
    private final Game game;
    private final Path dataDirectory;

    public DiscordSRVSpongeBootstrap(PluginContainer pluginContainer, Game game, JarInJarClassLoader classLoader, Path dataDirectory) throws IOException {
        // Don't change these parameters
        super(classLoader);
        this.logger = new Log4JLoggerImpl(pluginContainer.logger());
        this.classpathAppender = new JarInJarClasspathAppender(classLoader);
        this.dependencies = new InitialDependencyLoader(
                logger,
                dataDirectory,
                new String[] {"dependencies/runtimeDownload-sponge.txt"},
                classpathAppender
        );
        this.pluginContainer = pluginContainer;
        this.game = game;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void onConstruct() {
        dependencies.load();

        this.commandHandler = new SpongeCommandHandler(() -> discordSRV, pluginContainer);
        game.eventManager().registerListeners(pluginContainer, commandHandler);
    }

    @Override
    public void onStarted() {
        dependencies.enable(() -> this.discordSRV = new SpongeDiscordSRV(logger, classpathAppender, dataDirectory, pluginContainer, game, commandHandler));
        if (discordSRV != null) {
            discordSRV.invokeServerStarted();
        }
    }

    @Override
    public void onRefresh() {
        dependencies.reload(discordSRV);
    }

    @Override
    public void onStopping() {
        dependencies.disable(discordSRV);
    }
}
