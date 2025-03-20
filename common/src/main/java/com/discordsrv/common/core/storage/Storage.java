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

package com.discordsrv.common.core.storage;

import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.feature.linking.LinkStore;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Blocking
public interface Storage {

    void initialize();
    void close() throws StorageException;

    @Nullable
    Long getUserId(@NotNull UUID player);

    @Nullable
    UUID getPlayerUUID(long userId);

    void createLink(@NotNull UUID player, long userId);
    void removeLink(@NotNull UUID player, long userId);

    /**
     * Inserts the given code for the given player, removing any existing code if any, with a {@link LinkStore#LINKING_CODE_EXPIRY_TIME} expiry.
     */
    void storeLinkingCode(@NotNull UUID player, String username, String code);
    Pair<UUID, String> getLinkingCode(String code);
    void removeLinkingCode(@NotNull UUID player);

    int getLinkedAccountCount();

}
