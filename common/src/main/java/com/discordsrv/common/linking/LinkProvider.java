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

package com.discordsrv.common.linking;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LinkProvider {

    CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID);

    default CompletableFuture<Optional<Long>> getUserId(@NotNull UUID playerUUID) {
        Optional<Long> userId = getCachedUserId(playerUUID);
        if (userId.isPresent()) {
            return CompletableFuture.completedFuture(userId);
        }
        return queryUserId(playerUUID);
    }

    default Optional<Long> getCachedUserId(@NotNull UUID playerUUID) {
        return Optional.empty();
    }

    CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId);

    default CompletableFuture<Optional<UUID>> getPlayerUUID(long userId) {
        Optional<UUID> playerUUID = getCachedPlayerUUID(userId);
        if (playerUUID.isPresent()) {
            return CompletableFuture.completedFuture(playerUUID);
        }
        return queryPlayerUUID(userId);
    }

    default Optional<UUID> getCachedPlayerUUID(long userId) {
        return Optional.empty();
    }
}
