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

package com.discordsrv.modded;

import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.abstraction.bootstrap.LifecycleManager;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.impl.Log4JLoggerImpl;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

//? if fabric {
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
//?}

//? if neoforge {
/*import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.mcdependencydownload.neoforge.bootstrap.NeoforgeBootstrap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.VersionInfo;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.*;
*///?}

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

//? if fabric {
public class DiscordSRVModdedBootstrap implements DedicatedServerModInitializer, IBootstrap {
//?}
//? if neoforge {
/*public class DiscordSRVModdedBootstrap extends NeoforgeBootstrap implements IBootstrap {
*///?}
    private final static String DEPENDENCIES_RUNTIME = /*$ dependencies_file*/"dependencies/runtimeDownload-1.21.11-fabric.txt";

    private final Logger logger;

    private final LifecycleManager lifecycleManager;
    private final Path dataDirectory;
    private MinecraftServer minecraftServer;
    private ModdedDiscordSRV discordSRV;

    //? if fabric {
    private final ClasspathAppender classpathAppender;
    public DiscordSRVModdedBootstrap() {
        this.logger = new Log4JLoggerImpl(LogManager.getLogger("DiscordSRV"));

        this.classpathAppender = new dev.vankka.mcdependencydownload.fabric.classpath.FabricClasspathAppender();
        this.dataDirectory = FabricLoader.getInstance().getConfigDir().resolve("DiscordSRV");

        try {
            this.lifecycleManager = new LifecycleManager(
                    this.logger,
                    dataDirectory,
                    Collections.singletonList(DEPENDENCIES_RUNTIME),
                    classpathAppender
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.minecraftServer = null;
    }
    //?}

    //? if neoforge {
    /*public DiscordSRVModdedBootstrap(JarInJarClassLoader classLoader) {
        super(classLoader);
        this.logger = new Log4JLoggerImpl(LogManager.getLogger("DiscordSRV"));

        this.dataDirectory = Path.of(FMLConfig.defaultConfigPath(), "../config/discordsrv");

        try {
            this.lifecycleManager = new LifecycleManager(
                    this.logger,
                    dataDirectory,
                    Collections.singletonList(DEPENDENCIES_RUNTIME),
                    getClasspathAppender()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.minecraftServer = null;

        NeoForge.EVENT_BUS.register(this);
    }
    *///?}

    //? if fabric {
    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> {
            this.minecraftServer = minecraftServer;
            lifecycleManager.loadAndEnable(() -> this.discordSRV = new ModdedDiscordSRV(this));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(minecraftServer -> {
            if (this.discordSRV == null) {
                this.logger.error("Server started but ModdedDiscordSRV hasn't initialized properly.\n" +
                        "This is likely due to an error during the loading process. Please check the full logs for more details.");
                return;
            }
            this.discordSRV.runServerStarted();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(minecraftServer -> {
            if (this.discordSRV != null) this.discordSRV.runDisable();
        });
    }
    //?}

    //? if neoforge {
    /*@SubscribeEvent()
    public void onServerStarting(ServerStartingEvent event) {
        this.minecraftServer = event.getServer();
        this.lifecycleManager.loadAndEnable(() -> this.discordSRV = new ModdedDiscordSRV(this));
    }

    @SubscribeEvent()
    public void onServerStarted(ServerStartedEvent event) {
        if (this.discordSRV == null) {
            this.logger.error("Server started but ModdedDiscordSRV hasn't initialized properly.\n" +
                    "This is likely due to an error during the loading process. Please check the full logs for more details.");
            return;
        }
        this.discordSRV.runServerStarted();
    }

    @SubscribeEvent()
    public void onServerStopping(ServerStoppingEvent event) {
        if (this.discordSRV != null) this.discordSRV.runDisable();
    }
    *///?}

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public ClasspathAppender classpathAppender() {
        //? if fabric {
        return classpathAppender;
        //? } else if neoforge {
        /*return getClasspathAppender();
        *///?}
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
        //? if fabric {
        //? if minecraft: >=1.21.6 {
        String minecraftVersion = net.minecraft.SharedConstants.getCurrentVersion().name();
         //?} else {
        /*String minecraftVersion = net.minecraft.DetectedVersion.BUILT_IN.getName();
         *///?}

        String loader_version = FabricLoaderImpl.VERSION;
        Optional<ModContainer> fabricApi = FabricLoader.getInstance().getModContainer("fabric-api");
        return "Minecraft " + minecraftVersion + " with Fabric Loader " + loader_version + (fabricApi.map(modContainer -> " (Fabric API: " + modContainer.getMetadata().getVersion().getFriendlyString() + ")").orElse(""));
        //? }
        //? if neoforge {
        /*//? if fml: >= 10 {
        VersionInfo versionInfo = FMLLoader.getCurrent().getVersionInfo();
        //?} else {
        /^VersionInfo versionInfo = FMLLoader.versionInfo();
        ^///? }

        return "Minecraft " + versionInfo.mcVersion() + " with NeoForge " + versionInfo.neoForgeVersion();
        *///? }
    }

    public MinecraftServer getServer() {
        return minecraftServer;
    }

    public ModdedDiscordSRV getDiscordSRV() {
        return discordSRV;
    }
}
