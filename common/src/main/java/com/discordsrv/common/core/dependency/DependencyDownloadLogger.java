/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.dependency;

import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import dev.vankka.dependencydownload.dependency.Dependency;

public class DependencyDownloadLogger implements dev.vankka.dependencydownload.logger.Logger {

    private final Logger logger;

    public DependencyDownloadLogger(Logger logger) {
        this.logger = new NamedLogger(logger, "DependencyDownload");
    }

    @Override
    public void downloadStart() {
        logger.info("Downloading dependencies...");
    }

    @Override
    public void downloadDependency(Dependency dependency) {
        logger.info("Downloading " + dependency.getGAV());
    }

    @Override
    public void downloadSuccess(Dependency dependency) {
        logger.info("Downloaded " + dependency.getGAV());
    }

    @Override
    public void relocateStart() {
        logger.info("Relocating dependencies...");
    }

    @Override
    public void loadStart() {
        logger.info("Loading dependencies...");
    }

    @Override
    public void loadEnd() {
        logger.info("Loaded dependencies");
    }
}
