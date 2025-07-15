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

import com.discordsrv.common.core.profile.DiscordProfileData;
import com.discordsrv.common.core.profile.GameProfileData;
import com.discordsrv.common.core.storage.Storage;
import com.discordsrv.common.feature.linking.AccountLink;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStorage implements Storage {

    public static String IDENTIFIER = UUID.randomUUID().toString();

    private final Map<UUID, AccountLink> gameLinks = new ConcurrentHashMap<>();
    private final Map<Long, AccountLink> discordLinks = new ConcurrentHashMap<>();
    private final Map<String, Pair<UUID, String>> linkingCodes = new ConcurrentHashMap<>();

    private final Map<UUID, GameProfileData> gameProfiles = new ConcurrentHashMap<>();
    private final Map<Long, DiscordProfileData> discordProfiles = new ConcurrentHashMap<>();

    private final Set<UUID> requiredLinkingBypass = new HashSet<>();

    public MemoryStorage() {}

    @Override
    public void initialize() {}

    @Override
    public void close() {
        gameLinks.clear();
        discordLinks.clear();
    }

    @Override
    public @Nullable AccountLink getLinkByPlayerUUID(@NotNull UUID playerUUID) {
        return gameLinks.get(playerUUID);
    }

    @Override
    public @Nullable AccountLink getLinkByUserId(long userId) {
        return discordLinks.get(userId);
    }

    @Override
    public void createLink(@NotNull AccountLink link) {
        gameLinks.put(link.playerUUID(), link);
        discordLinks.put(link.userId(), link);
    }

    @Override
    public void removeLink(@NotNull UUID playerUUID, long userId) {
        gameLinks.remove(playerUUID);
        discordLinks.remove(userId);
    }

    @Override
    public void storeLinkingCode(@NotNull UUID playerUUID, @NotNull String username, String code) {
        linkingCodes.put(code, Pair.of(playerUUID, username));
    }

    @Override
    public Pair<UUID, String> getLinkingCode(String code) {
        return linkingCodes.get(code);
    }

    @Override
    public void removeLinkingCode(@NotNull UUID playerUUID) {
        linkingCodes.values().removeIf(code -> code.getKey() == playerUUID);
    }

    @Override
    public int getLinkedAccountCount() {
        return gameLinks.size();
    }

    @Override
    public GameProfileData getGameProfileData(@NotNull UUID playerUUID) {
        return gameProfiles.get(playerUUID);
    }

    @Override
    public void saveGameProfileData(@NotNull GameProfileData profile) {
        gameProfiles.put(profile.getPlayerUUID(), profile);
    }

    @Override
    public DiscordProfileData getDiscordProfileData(long userId) {
        return discordProfiles.get(userId);
    }

    @Override
    public void saveDiscordProfileData(@NotNull DiscordProfileData profile) {
        discordProfiles.put(profile.getUserId(), profile);
    }

    @Override
    public void addRequiredLinkingBypass(UUID playerUUID) {
        requiredLinkingBypass.add(playerUUID);
    }

    @Override
    public void removeRequiredLinkingBypass(UUID playerUUID) {
        requiredLinkingBypass.remove(playerUUID);
    }

    @Override
    public Set<UUID> getRequiredLinkingBypass() {
        return requiredLinkingBypass;
    }
}
