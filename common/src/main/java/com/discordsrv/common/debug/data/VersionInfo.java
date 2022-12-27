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

package com.discordsrv.common.debug.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VersionInfo {

    private final String version;
    private final String gitRevision;
    private final String gitBranch;
    private final String buildTime;

    public VersionInfo(String version, String gitRevision, String gitBranch, String buildTime) {
        this.version = version;
        this.gitRevision = gitRevision;
        this.gitBranch = gitBranch;
        this.buildTime = buildTime;
    }

    @NotNull
    public String version() {
        return version;
    }

    @Nullable
    public String gitRevision() {
        return gitRevision;
    }

    @Nullable
    public String gitBranch() {
        return gitBranch;
    }

    @NotNull
    public String buildTime() {
        return buildTime;
    }
}
