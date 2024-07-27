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

package com.discordsrv.common.core.storage.impl;

import com.discordsrv.common.core.storage.Storage;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MemoryStorage implements Storage {

    public static String IDENTIFIER = UUID.randomUUID().toString();

    private final BidiMap<UUID, Long> linkedAccounts = new DualHashBidiMap<>();
    private final BidiMap<UUID, String> linkingCodes = new DualHashBidiMap<>();

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
    public void storeLinkingCode(@NotNull UUID player, String code) {
        linkingCodes.put(player, code);
    }

    @Override
    public UUID getLinkingCode(String code) {
        return linkingCodes.getKey(code);
    }

    @Override
    public void removeLinkingCode(@NotNull UUID player) {
        linkingCodes.remove(player);
    }

    @Override
    public int getLinkedAccountCount() {
        return linkedAccounts.size();
    }
}
