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

package com.discordsrv.bungee;

import com.discordsrv.common.dependency.InitialDependencyLoader;
import dev.vankka.mcdependencydownload.bungee.bootstrap.BungeeBootstrap;
import dev.vankka.mcdependencydownload.classloader.JarInJarClassLoader;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;

public class DiscordSRVBungeeBootstrap extends BungeeBootstrap {

    private final InitialDependencyLoader dependencies;
    private BungeeDiscordSRV discordSRV;

    public DiscordSRVBungeeBootstrap(JarInJarClassLoader classLoader, Plugin plugin) throws IOException {
        // Don't change these parameters
        super(classLoader, plugin);
        this.dependencies = new InitialDependencyLoader(
                plugin.getDataFolder().toPath(),
                new String[] {"dependencies/runtimeDownloadApi-bungee.txt"},
                getClasspathAppender()
        );
        dependencies.whenComplete(() -> this.discordSRV = new BungeeDiscordSRV(this));
    }

    @Override
    public void onEnable() {
        dependencies.whenComplete(discordSRV::invokeEnable);
    }

    @Override
    public void onDisable() {
        dependencies.whenComplete(discordSRV::invokeDisable);
    }
}
