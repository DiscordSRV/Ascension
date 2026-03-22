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

package com.discordsrv.neoforge;

import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.abstraction.bootstrap.LifecycleManager;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.impl.Log4JLoggerImpl;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.util.ClassLoaderUtils;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.jarinjar.bootstrap.AbstractBootstrap;
import dev.vankka.dependencydownload.jarinjar.bootstrap.classpath.JarInJarClasspathAppender;
import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.VersionInfo;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.*;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

public class DiscordSRVNeoForgeBootstrap extends AbstractBootstrap implements IBootstrap {
    private final static String DEPENDENCIES_RUNTIME = /*$ dependencies_file*/"dependencies/runtimeDownload-1.21.11-fabric.txt";

    private final Logger logger;
    private final LifecycleManager lifecycleManager;
    private final Path dataDirectory;

    private JarInJarClasspathAppender classpathAppender;
    private ModContainer modContainer;
    private IEventBus eventBus;
    private MinecraftServer minecraftServer;
    private ModdedDiscordSRV discordSRV;

    public DiscordSRVNeoForgeBootstrap(JarInJarClassLoader classLoader, ModContainer modContainer, IEventBus eventBus) {
        super(classLoader);
        ClassLoaderUtils.setClassLoader(classLoader);

        this.classpathAppender = new JarInJarClasspathAppender(classLoader);
        this.modContainer = modContainer;
        this.eventBus = eventBus;

        this.logger = new Log4JLoggerImpl(LogManager.getLogger("DiscordSRV"));
        this.dataDirectory = FMLPaths.CONFIGDIR.get().resolve("discordsrv");

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

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new DiscordSRVNeoForgePermissionAPI(this));
    }

    @SubscribeEvent()
    public void onServerStarting(ServerAboutToStartEvent event) {
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
        return getClassLoader();
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
        //? if fml: >= 10 {
        VersionInfo versionInfo = FMLLoader.getCurrent().getVersionInfo();
        //?} else {
        /*VersionInfo versionInfo = FMLLoader.versionInfo();
        *///? }

        return "Minecraft " + versionInfo.mcVersion() + " with NeoForge " + versionInfo.neoForgeVersion();
    }

    public MinecraftServer getServer() {
        return minecraftServer;
    }

    public ModdedDiscordSRV getDiscordSRV() {
        return discordSRV;
    }
}
