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

package com.discordsrv.common.dependency;

import com.discordsrv.common.DiscordSRV;
import dev.vankka.dependencydownload.ApplicationDependencyManager;
import dev.vankka.dependencydownload.DependencyManager;

import java.io.IOException;
import java.nio.file.Path;

public class DiscordSRVDependencyManager {

    private final DiscordSRV discordSRV;
    private final ApplicationDependencyManager dependencyManager;

    public DiscordSRVDependencyManager(DiscordSRV discordSRV, DependencyLoader initialLoader) {
        this.discordSRV = discordSRV;
        Path cacheDirectory = DependencyLoader.resolvePath(discordSRV.dataDirectory());
        this.dependencyManager = new ApplicationDependencyManager(cacheDirectory);

        if (initialLoader != null) {
            //noinspection ResultOfMethodCallIgnored
            dependencyManager.include(initialLoader.getDependencyManager());
        }
    }

    private DependencyLoader loader(DependencyManager manager) {
        return new DependencyLoader(discordSRV, dependencyManager.include(manager));
    }

    private DependencyLoader loader(String[] paths) throws IOException {
        return loader(DependencyLoader.fromPaths(discordSRV.dataDirectory(), paths));
    }

    public DependencyLoader hikari() throws IOException {
        return loader(new String[] {"dependencies/hikari.txt"});
    }

    public DependencyLoader h2() throws IOException {
        return loader(new String[] {"dependencies/h2Driver.txt"});
    }

    public DependencyLoader mysql() throws IOException {
        return loader(new String[] {"dependencies/mysqlDriver.txt"});
    }

}
