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

package com.discordsrv.common.core.storage.impl;

import com.discordsrv.common.core.storage.Storage;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStorage implements Storage {

    public static String IDENTIFIER = UUID.randomUUID().toString();

    private final BidiMap<UUID, Long> linkedAccounts = new DualHashBidiMap<>();
    private final Map<String, Pair<UUID, String>> linkingCodes = new ConcurrentHashMap<>();

    public MemoryStorage() {}

    @Override
    public void initialize() {}

    @Override
    public void close() {
        linkedAccounts.clear();
    }

    @Override
    public @Nullable Long getUserId(@NotNull UUID player) {
        return linkedAccounts.get(player);
    }

    @Override
    public @Nullable UUID getPlayerUUID(long userId) {
        return linkedAccounts.getKey(userId);
    }

    @Override
    public void createLink(@NotNull UUID player, long userId) {
        linkedAccounts.put(player, userId);
    }

    @Override
    public void removeLink(@NotNull UUID player, long userId) {
        linkedAccounts.remove(player, userId);
    }

    @Override
    public void storeLinkingCode(@NotNull UUID player, @NotNull String username, String code) {
        linkingCodes.put(code, Pair.of(player, username));
    }

    @Override
    public Pair<UUID, String> getLinkingCode(String code) {
        return linkingCodes.get(code);
    }

    @Override
    public void removeLinkingCode(@NotNull UUID player) {
        linkingCodes.values().removeIf(code -> code.getKey() == player);
    }

    @Override
    public int getLinkedAccountCount() {
        return linkedAccounts.size();
    }

}
