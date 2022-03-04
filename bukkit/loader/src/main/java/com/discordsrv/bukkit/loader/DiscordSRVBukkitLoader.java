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

package com.discordsrv.bukkit.loader;

import dev.vankka.mcdependencydownload.bukkit.loader.BukkitLoader;
import dev.vankka.mcdependencydownload.loader.exception.LoadingException;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.logging.Level;

@SuppressWarnings("unused") // Used by Bukkit
public class DiscordSRVBukkitLoader extends BukkitLoader {

    @Override
    public @NotNull String getBootstrapClassName() {
        return "com.discordsrv.bukkit.DiscordSRVBukkitBootstrap";
    }

    @Override
    public @NotNull URL getJarInJarResource() {
        URL resource = getClassLoader().getResource("bukkit.jarinjar");
        if (resource == null) {
            throw new IllegalStateException("Jar does not contain jarinjar");
        }
        return resource;
    }

    @Override
    public void handleLoadingException(LoadingException exception) {
        getLogger().logp(Level.SEVERE, null, null, exception.getCause(), exception::getMessage);
    }
}
