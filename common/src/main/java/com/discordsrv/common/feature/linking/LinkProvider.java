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

package com.discordsrv.common.feature.linking;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.abstraction.player.IPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public interface LinkProvider {

    default Task<Optional<AccountLink>> query(@NotNull UUID playerUUID) {
        return query(playerUUID, false);
    }

    Task<Optional<AccountLink>> query(@NotNull UUID playerUUID, boolean canCauseLink);

    default Task<Optional<AccountLink>> get(@NotNull UUID playerUUID) {
        Optional<AccountLink> userId = getCached(playerUUID);
        if (userId.isPresent()) {
            return Task.completed(userId);
        }
        return query(playerUUID);
    }

    default Optional<AccountLink> getCached(@NotNull UUID playerUUID) {
        return Optional.empty();
    }

    default Task<Optional<AccountLink>> query(long userId) {
        return query(userId, false);
    }

    Task<Optional<AccountLink>> query(long userId, boolean canCauseLink);

    default Task<Optional<AccountLink>> get(long userId) {
        Optional<AccountLink> playerUUID = getCached(userId);
        if (playerUUID.isPresent()) {
            return Task.completed(playerUUID);
        }
        return query(userId);
    }

    default Optional<AccountLink> getCached(long userId) {
        return Optional.empty();
    }

    default Task<MinecraftComponent> getLinkingInstructions(@NotNull IPlayer player, @Nullable String requestReason) {
        return getLinkingInstructions(player.username(), player.uniqueId(), player.locale(), requestReason);
    }

    Task<MinecraftComponent> getLinkingInstructions(
            String username,
            UUID playerUUID,
            @Nullable Locale locale,
            @Nullable String requestReason,
            Object... additionalContext
    );
    boolean isValidCode(@NotNull String code);

    @NotNull
    LinkStore store();

    default boolean usesLocalLinking() {
        return this instanceof LinkStore;
    }
}
