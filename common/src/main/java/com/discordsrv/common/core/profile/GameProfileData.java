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

package com.discordsrv.common.core.profile;

import java.util.*;

public class GameProfileData {

    private final UUID playerUUID;
    private final Set<String> grantedRewards;
    private final Set<String> pendingRewards;

    public GameProfileData(UUID playerUUID) {
        this(playerUUID, new HashSet<>(), new HashSet<>());
    }

    public GameProfileData(UUID playerUUID, Set<String> grantedRewards, Set<String> pendingRewards) {
        this.playerUUID = playerUUID;
        this.grantedRewards = grantedRewards;
        this.pendingRewards = pendingRewards;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Set<String> getGrantedRewards() {
        return grantedRewards;
    }

    public Set<String> getPendingRewards() {
        return pendingRewards;
    }
}
