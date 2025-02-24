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

package com.discordsrv.common.feature.profile;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.profile.IProfile;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.IPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@PlaceholderPrefix("profile_")
public class Profile implements IProfile {

    private final DiscordSRV discordSRV;
    private final UUID playerUUID;
    private final Long userId;

    public Profile(DiscordSRV discordSRV, UUID playerUUID, Long userId) {
        this.discordSRV = discordSRV;
        this.playerUUID = playerUUID;
        this.userId = userId;
    }

    @Placeholder("player_uuid")
    @Override
    public @Nullable UUID playerUUID() {
        return playerUUID;
    }

    @Placeholder("user_id")
    @Override
    public @Nullable Long userId() {
        return userId;
    }

    @Placeholder("player")
    @Nullable
    public IPlayer player() {
        if (playerUUID == null) {
            return null;
        }
        return discordSRV.playerProvider().player(playerUUID);
    }

    @Placeholder("offline_player")
    public Task<@Nullable IOfflinePlayer> linkedOfflinePlayer() {
        if (playerUUID == null) {
            return Task.completed(null);
        }
        return discordSRV.playerProvider().lookupOfflinePlayer(playerUUID);
    }

    @Placeholder("user")
    public Task<@Nullable DiscordUser> linkedUser() {
        if (userId == null) {
            return Task.completed(null);
        }
        return discordSRV.discordAPI().retrieveUserById(userId);
    }
}
