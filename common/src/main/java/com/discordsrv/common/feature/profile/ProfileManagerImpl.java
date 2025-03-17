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

package com.discordsrv.common.feature.profile;

import com.discordsrv.api.profile.ProfileManager;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.feature.linking.LinkProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManagerImpl implements ProfileManager {

    private final DiscordSRV discordSRV;
    private final Map<UUID, Task<ProfileImpl>> profileLookups = new ConcurrentHashMap<>();
    private final Map<UUID, ProfileImpl> profiles = new ConcurrentHashMap<>();
    private final Map<Long, ProfileImpl> discordUserMap = new ConcurrentHashMap<>();

    public ProfileManagerImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public Task<ProfileImpl> loadProfile(@NotNull UUID playerUUID) {
        Task<ProfileImpl> lookup = lookupProfile(playerUUID)
                .thenApply(profile -> {
                    profiles.put(playerUUID, profile);
                    if (profile.isLinked()) {
                        discordUserMap.put(profile.userId(), profile);
                    }
                    return profile;
                });
        profileLookups.put(playerUUID, lookup);
        lookup.whenComplete((__, ___) -> profileLookups.remove(playerUUID));
        return lookup;
    }

    public void unloadProfile(@NotNull UUID playerUUID) {
        Task<ProfileImpl> lookup = profileLookups.remove(playerUUID);
        if (lookup != null) {
            lookup.cancel(false);
        }

        ProfileImpl profile = profiles.remove(playerUUID);
        if (profile == null) {
            return;
        }

        if (profile.isLinked()) {
            discordUserMap.remove(profile.userId());
        }
    }

    @Override
    public @NotNull Task<@NotNull ProfileImpl> lookupProfile(UUID playerUUID) {
        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) return Task.completed(null);
        return linkProvider.getUserId(playerUUID)
                .thenApply(opt -> new ProfileImpl(discordSRV, playerUUID, opt.orElse(null)));
    }

    @Override
    public @Nullable ProfileImpl getProfile(UUID playerUUID) {
        return profiles.get(playerUUID);
    }

    @Override
    public @NotNull Task<@NotNull ProfileImpl> lookupProfile(long userId) {
        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) return Task.completed(null);
        return linkProvider.getPlayerUUID(userId)
                .thenApply(opt -> new ProfileImpl(discordSRV, opt.orElse(null), userId));
    }

    @Override
    public @Nullable ProfileImpl getProfile(long userId) {
        return discordUserMap.get(userId);
    }
}
