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
import dev.vankka.dependencydownload.classloader.IsolatedClassLoader;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.VersionInfo;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

@Mod(value = "discordsrv")
public class DiscordSRVNeoforgeBootstrap implements IBootstrap {
    private final static String DEPENDENCIES_RUNTIME = /*$ dependencies_file*/"dependencies/runtimeDownload-1.21.11.txt";

    private final Logger logger;
    private final ClasspathAppender classpathAppender;
    private final LifecycleManager lifecycleManager;
    private final Path dataDirectory;
    private MinecraftServer minecraftServer;
    private NeoforgeDiscordSRV discordSRV;

    public DiscordSRVNeoforgeBootstrap() {
        this.logger = new Log4JLoggerImpl(LogManager.getLogger("DiscordSRV"));
        this.classpathAppender = new IsolatedClassLoader();
        this.dataDirectory = Path.of(FMLConfig.defaultConfigPath(), "discordsrv");
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
    }

    @SubscribeEvent()
    public void onServerStarting(ServerStartingEvent event) {
        this.minecraftServer = event.getServer();
        this.lifecycleManager.loadAndEnable(() -> this.discordSRV = new NeoforgeDiscordSRV(this));
    }

    @SubscribeEvent()
    public void onServerStarted(ServerStartedEvent event) {
        if (this.discordSRV == null) {
            this.logger.error("Server started but FabricDiscordSRV hasn't initialized properly.\n" +
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
        //? if fml: > 5 {
        VersionInfo versionInfo = FMLLoader.getCurrent().getVersionInfo();
        //?} else {
        //VersionInfo versionInfo = FMLLoader.versionInfo();
        //? }

        return "Minecraft " + versionInfo.mcVersion() + " with NeoForge " + versionInfo.neoForgeVersion();
    }

    public MinecraftServer getServer() {
        return minecraftServer;
    }

    public NeoforgeDiscordSRV getDiscordSRV() {
        return discordSRV;
    }
}
