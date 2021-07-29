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

package com.discordsrv.common.server.player;

import com.discordsrv.common.player.IOfflinePlayer;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.provider.AbstractPlayerProvider;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class ServerPlayerProvider<T extends IPlayer> extends AbstractPlayerProvider<T> {

    public abstract CompletableFuture<Optional<IOfflinePlayer>> offlinePlayer(UUID uuid);
    public abstract CompletableFuture<Optional<IOfflinePlayer>> offlinePlayer(String username);
}
