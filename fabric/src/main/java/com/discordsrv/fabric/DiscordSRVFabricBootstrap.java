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

package com.discordsrv.fabric;

import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.abstraction.bootstrap.LifecycleManager;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.impl.Log4JLoggerImpl;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.mcdependencydownload.fabric.classpath.FabricClasspathAppender;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.GameVersion;
import net.minecraft.MinecraftVersion;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

public class DiscordSRVFabricBootstrap implements DedicatedServerModInitializer, IBootstrap {

    private final Logger logger;
    private final ClasspathAppender classpathAppender;
    private final LifecycleManager lifecycleManager;
    private final Path dataDirectory;
    private MinecraftServer minecraftServer;
    private FabricDiscordSRV discordSRV;
    private MinecraftServerAudiences adventure;

    public DiscordSRVFabricBootstrap() {
        this.logger = new Log4JLoggerImpl(LogManager.getLogger("DiscordSRV"));
        this.classpathAppender = new FabricClasspathAppender();
        this.dataDirectory = FabricLoader.getInstance().getConfigDir().resolve("DiscordSRV");
        try {
            this.lifecycleManager = new LifecycleManager(
                    this.logger,
                    dataDirectory,
                    Collections.singletonList("dependencies/runtimeDownload-fabric.txt"),
                    classpathAppender
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.minecraftServer = null;
        this.adventure = null;
    }

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> {
            this.minecraftServer = minecraftServer;
            this.adventure = MinecraftServerAudiences.of(minecraftServer);
            lifecycleManager.loadAndEnable(() -> this.discordSRV = new FabricDiscordSRV(this));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(minecraftServer -> this.discordSRV.runServerStarted());

        ServerLifecycleEvents.SERVER_STOPPING.register(minecraftServer -> {
            if (this.discordSRV != null) this.discordSRV.runDisable();
        });
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public ClasspathAppender classpathAppender() {
        return classpathAppender;
    }

    @Override
    public ClassLoader classLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public LifecycleManager lifecycleManager() {
        return lifecycleManager;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public String platformVersion() {
        GameVersion version = MinecraftVersion.CURRENT;
        String loader_version = FabricLoaderImpl.VERSION;
        Optional<ModContainer> fabricApi = FabricLoader.getInstance().getModContainer("fabric-api");
        return "Minecraft "+ version.getName() + " with Fabric Loader " + loader_version + (fabricApi.map(modContainer -> " (Fabric API: " + modContainer.getMetadata().getVersion().getFriendlyString() + ")").orElse(""));
    }

    public MinecraftServer getServer() {
        return minecraftServer;
    }

    public FabricDiscordSRV getDiscordSRV() {
        return discordSRV;
    }

    public MinecraftServerAudiences getAdventure() {
        return adventure;
    }
}
