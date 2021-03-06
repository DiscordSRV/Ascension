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

package com.discordsrv.common.player.provider;

import com.discordsrv.api.player.IPlayerProvider;
import com.discordsrv.common.player.IPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PlayerProvider<T extends IPlayer> extends IPlayerProvider {

    /**
     * Gets an online player by {@link UUID}.
     * @param uuid the uuid of the Player
     */
    @NotNull
    Optional<T> player(@NotNull UUID uuid);

    /**
     * Gets an online player by username.
     * @param username case-insensitive username for the player
     */
    @NotNull
    Optional<T> player(@NotNull String username);

    /**
     * Gets all online players.
     * @return all players that are currently online
     */
    @NotNull
    Collection<T> allPlayers();
}
