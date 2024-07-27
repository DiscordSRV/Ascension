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

package com.discordsrv.common.helper;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.feature.profile.Profile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Someone {

    public static Someone.Resolved of(@NotNull DiscordSRVPlayer player, @NotNull DiscordUser user) {
        return of(player.uniqueId(), user.getId());
    }

    public static Someone.Resolved of(@NotNull UUID playerUUID, long userId) {
        return new Someone.Resolved(playerUUID, userId);
    }

    public static Someone of(@NotNull DiscordSRVPlayer player) {
        return of(player.uniqueId());
    }

    public static Someone of(@NotNull UUID playerUUID) {
        return new Someone(playerUUID, null);
    }

    public static Someone of(@NotNull DiscordUser user) {
        return of(user.getId());
    }

    public static Someone of(long userId) {
        return new Someone(null, userId);
    }

    private final UUID playerUUID;
    private final Long userId;

    private Someone(@Nullable UUID playerUUID, @Nullable Long userId) {
        this.playerUUID = playerUUID;
        this.userId = userId;
    }

    private <T> T throwIllegal() {
        throw new IllegalStateException("Cannot have Someone instance without either a Player UUID or User Id");
    }

    @NotNull
    public CompletableFuture<@NotNull Profile> profile(DiscordSRV discordSRV) {
        if (playerUUID != null) {
            return discordSRV.profileManager().lookupProfile(playerUUID);
        } else if (userId != null) {
            return discordSRV.profileManager().lookupProfile(userId);
        } else {
            return throwIllegal();
        }
    }

    @NotNull
    public CompletableFuture<Someone.@Nullable Resolved> withLinkedAccounts(DiscordSRV discordSRV) {
        if (playerUUID != null && userId != null) {
            return CompletableFuture.completedFuture(of(playerUUID, userId));
        }

        if (playerUUID != null) {
            return withUserId(discordSRV).thenApply(userId -> userId != null ? of(playerUUID, userId) : null);
        } else if (userId != null) {
            return withPlayerUUID(discordSRV).thenApply(playerUUID -> playerUUID != null ? of(playerUUID, userId) : null);
        } else {
            return throwIllegal();
        }
    }

    public CompletableFuture<@Nullable Long> withUserId(DiscordSRV discordSRV) {
        if (userId != null) {
            return CompletableFuture.completedFuture(userId);
        } else if (playerUUID == null) {
            return throwIllegal();
        }
        return discordSRV.linkProvider().getUserId(playerUUID).thenApply(opt -> opt.orElse(null));
    }

    public CompletableFuture<@Nullable UUID> withPlayerUUID(DiscordSRV discordSRV) {
        if (playerUUID != null) {
            return CompletableFuture.completedFuture(playerUUID);
        } else if (userId == null) {
            return throwIllegal();
        }
        return discordSRV.linkProvider().getPlayerUUID(userId).thenApply(opt -> opt.orElse(null));
    }

    @Nullable
    public UUID playerUUID() {
        return playerUUID;
    }

    @Nullable
    public Long userId() {
        return userId;
    }

    @Override
    public String toString() {
        return playerUUID != null ? playerUUID.toString() : Long.toUnsignedString(Objects.requireNonNull(userId));
    }

    @SuppressWarnings("DataFlowIssue")
    public static class Resolved extends Someone {

        private Resolved(@NotNull UUID playerUUID, @NotNull Long userId) {
            super(playerUUID, userId);
        }

        @Override
        public @NotNull UUID playerUUID() {
            return super.playerUUID();
        }

        @Override
        public @NotNull Long userId() {
            return super.userId();
        }
    }
}
