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

package com.discordsrv.common.profile;

import com.discordsrv.api.profile.IProfileManager;
import com.discordsrv.common.DiscordSRV;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager implements IProfileManager {

    private final DiscordSRV discordSRV;
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private final Map<Long, Profile> discordUserMap = new ConcurrentHashMap<>();

    public ProfileManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Blocking
    public void loadProfile(UUID playerUUID) {
        Profile profile = lookupProfile(playerUUID).join();
        profiles.put(playerUUID, profile);
        if (profile.isLinked()) {
            discordUserMap.put(profile.userId(), profile);
        }
    }

    public void unloadProfile(UUID playerUUID) {
        Profile profile = profiles.remove(playerUUID);
        if (profile == null) {
            return;
        }

        if (profile.isLinked()) {
            discordUserMap.remove(profile.userId());
        }
    }

    @Override
    public @NotNull CompletableFuture<Profile> lookupProfile(UUID playerUUID) {
        return discordSRV.linkProvider().getUserId(playerUUID)
                .thenApply(opt -> new Profile(playerUUID, opt.orElse(null)));
    }

    @Override
    public @Nullable Profile getProfile(UUID playerUUID) {
        return profiles.get(playerUUID);
    }

    @Override
    public @NotNull CompletableFuture<Profile> lookupProfile(long userId) {
        return discordSRV.linkProvider().getPlayerUUID(userId)
                .thenApply(opt -> new Profile(opt.orElse(null), userId));
    }

    @Override
    public @Nullable Profile getProfile(long userId) {
        return discordUserMap.get(userId);
    }
}
