/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.player.IPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface LinkProvider {

    default CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID) {
        return queryUserId(playerUUID, false);
    }

    CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID, boolean canCauseLink);

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

    default CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId) {
        return queryPlayerUUID(userId, false);
    }

    CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId, boolean canCauseLink);

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

    default CompletableFuture<MinecraftComponent> getLinkingInstructions(@NotNull IPlayer player, @Nullable String requestReason) {
        return getLinkingInstructions(player.username(), player.uniqueId(), player.locale(), requestReason);
    }

    CompletableFuture<MinecraftComponent> getLinkingInstructions(
            String username,
            UUID playerUUID,
            @Nullable Locale locale,
            @Nullable String requestReason
    );
}
