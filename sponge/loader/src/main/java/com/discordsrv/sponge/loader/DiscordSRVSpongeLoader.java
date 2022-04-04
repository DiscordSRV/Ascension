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

package com.discordsrv.sponge.loader;

import com.discordsrv.sponge.bootstrap.ISpongeBootstrap;
import com.google.inject.Inject;
import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.dependencydownload.jarinjar.loader.ILoader;
import dev.vankka.dependencydownload.jarinjar.loader.exception.LoadingException;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

@Plugin("discordsrv")
public class DiscordSRVSpongeLoader implements ILoader {

    private final PluginContainer pluginContainer;
    private final Game game;
    private final Path dataDirectory;
    private final JarInJarClassLoader classLoader;
    private ISpongeBootstrap bootstrap;

    @Inject
    public DiscordSRVSpongeLoader(PluginContainer pluginContainer, Game game, @ConfigDir(sharedRoot = false) Path dataDirectory) {
        this.pluginContainer = pluginContainer;
        this.game = game;
        this.dataDirectory = dataDirectory;

        if (!game.platform().type().isServer()) {
            Logger logger = pluginContainer.logger();
            logger.error("+---------------------------------------------+");
            logger.error("| DiscordSRV does not run on clients          |");
            logger.error("| DiscordSRV can only be installed on servers |");
            logger.error("+---------------------------------------------+");
            this.classLoader = null;
            return;
        }

        this.classLoader = initialize();
    }

    private Optional<ISpongeBootstrap> bootstrap() {
        return Optional.ofNullable(bootstrap);
    }

    @Override
    public @NotNull String getBootstrapClassName() {
        return "com.discordsrv.sponge.DiscordSRVSpongeBootstrap";
    }

    @Override
    public void initiateBootstrap(Class<?> bootstrapClass, @NotNull JarInJarClassLoader classLoader) throws ReflectiveOperationException {
        Constructor<?> constructor = bootstrapClass.getConstructor(PluginContainer.class, Game.class, JarInJarClassLoader.class, Path.class);
        bootstrap = (ISpongeBootstrap) constructor.newInstance(pluginContainer, game, classLoader, dataDirectory);
    }

    @Override
    public void handleLoadingException(LoadingException exception) {
        pluginContainer.logger().error(exception.getMessage(), exception.getCause());
    }

    @Override
    public @NotNull String getName() {
        return "DiscordSRV";
    }

    @Override
    public @NotNull ClassLoader getParentClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public @NotNull URL getJarInJarResource() {
        URL resource = getParentClassLoader().getResource("sponge.jarinjar");
        if (resource == null) {
            throw new IllegalStateException("Jar does not contain jarinjar");
        }
        return resource;
    }

    @Listener
    public void onConstructPlugin(ConstructPluginEvent event) {
        bootstrap().ifPresent(ISpongeBootstrap::onConstruct);
    }

    @Listener
    public void onRefreshGame(RefreshGameEvent event) {
        bootstrap().ifPresent(ISpongeBootstrap::onRefresh);
    }

    @Listener
    public void onStartedEngine(StartedEngineEvent<?> event) {
        bootstrap().ifPresent(ISpongeBootstrap::onStarted);
    }

    @Listener
    public void onStoppingEngine(StoppingEngineEvent<?> event) {
        bootstrap().ifPresent(ISpongeBootstrap::onStopping);
        try {
            classLoader.close();
        } catch (IOException e) {
            pluginContainer.logger().error("Failed to close JarInJarClassLoader", e);
        }
    }
}
