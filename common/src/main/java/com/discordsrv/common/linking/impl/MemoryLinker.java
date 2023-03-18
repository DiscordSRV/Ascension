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

package com.discordsrv.common.linking.impl;

import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MemoryLinker implements LinkProvider, LinkStore {

    private final BidiMap<UUID, Long> map = new DualHashBidiMap<>();

    @Override
    public CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID) {
        return CompletableFuture.completedFuture(Optional.ofNullable(map.get(playerUUID)));
    }

    @Override
    public CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId) {
        return CompletableFuture.completedFuture(Optional.ofNullable(map.getKey(userId)));
    }

    @Override
    public CompletableFuture<Void> createLink(@NotNull UUID playerUUID, long userId) {
        map.put(playerUUID, userId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeLink(@NotNull UUID playerUUID, long userId) {
        map.remove(playerUUID);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Integer> getLinkedAccountCount() {
        return CompletableFuture.completedFuture(map.size());
    }
}
