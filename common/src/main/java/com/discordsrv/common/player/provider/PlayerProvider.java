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

package com.discordsrv.common.player.provider;

import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.player.IPlayerProvider;
import com.discordsrv.common.player.IOfflinePlayer;
import com.discordsrv.common.player.IPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerProvider<T extends IPlayer> extends IPlayerProvider {

    /**
     * Gets an online player by {@link UUID}.
     *
     * @param uuid the uuid of the Player
     */
    @Nullable
    DiscordSRVPlayer player(@NotNull UUID uuid);

    /**
     * Gets an online player by username.
     *
     * @param username case-insensitive username for the player
     */
    @Nullable
    DiscordSRVPlayer player(@NotNull String username);

    /**
     * Gets all online players.
     * @return all players that are currently online
     */
    @NotNull
    Collection<T> allPlayers();

    CompletableFuture<UUID> lookupUUIDForUsername(String username);

    default CompletableFuture<IOfflinePlayer> lookupOfflinePlayer(String username) {
        return lookupUUIDForUsername(username).thenCompose(this::lookupOfflinePlayer);
    }
    CompletableFuture<IOfflinePlayer> lookupOfflinePlayer(UUID uuid);
}
