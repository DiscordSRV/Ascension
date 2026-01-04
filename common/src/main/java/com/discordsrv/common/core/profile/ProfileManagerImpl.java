/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.profile;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.linking.AccountLinkedEvent;
import com.discordsrv.api.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.profile.ProfileManager;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.feature.linking.AccountLink;
import com.discordsrv.common.feature.linking.LinkProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManagerImpl implements ProfileManager {

    private final DiscordSRV discordSRV;
    private final Map<UUID, Task<ProfileImpl>> profileLookups = new ConcurrentHashMap<>();
    private final Map<UUID, ProfileImpl> profiles = new ConcurrentHashMap<>();
    private final Map<Long, ProfileImpl> discordUserMap = new ConcurrentHashMap<>();

    public ProfileManagerImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        discordSRV.eventBus().subscribe(this);
    }

    @Subscribe(priority = EventPriorities.EARLIEST)
    public void onPlayerConnected(PlayerConnectedEvent event) {
        loadProfile(event.player().uniqueId());
    }

    @Subscribe(priority = EventPriorities.LAST)
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        unloadProfile(event.player().uniqueId());
    }

    @Subscribe(priority = EventPriorities.EARLIEST)
    public void onAccountLinked(AccountLinkedEvent event) {
        loadProfile(event.getPlayerUUID());
    }

    @Subscribe(priority = EventPriorities.LAST)
    public void onAccountUnlinked(AccountUnlinkedEvent event) {
        unloadProfile(event.getPlayerUUID());
    }

    public Task<ProfileImpl> loadProfile(@NotNull UUID playerUUID) {
        Task<ProfileImpl> lookup = queryProfile(playerUUID)
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

    private Task<GameProfileData> loadGameData(UUID playerUUID) {
        return discordSRV.scheduler().supply(() -> {
            GameProfileData data = discordSRV.storage().getGameProfileData(playerUUID);
            if (data != null) {
                return data;
            }
            return new GameProfileData(playerUUID);
        });
    }

    private Task<DiscordProfileData> loadDiscordData(long userId) {
        return discordSRV.scheduler().supply(() -> {
            DiscordProfileData data = discordSRV.storage().getDiscordProfileData(userId);
            if (data != null) {
                return data;
            }
            return new DiscordProfileData(userId);
        });
    }

    @Override
    public @NotNull Task<@NotNull ProfileImpl> queryProfile(UUID playerUUID) {
        LinkProvider linkProvider = discordSRV.linkProvider();
        Task<Optional<AccountLink>> linkLookup = linkProvider != null ? linkProvider.get(playerUUID) : Task.completed(Optional.empty());

        Task<GameProfileData> gameProfileLookup = loadGameData(playerUUID);
        return Task.allOf(gameProfileLookup, linkLookup).then(v -> {
            GameProfileData gameProfile = gameProfileLookup.join();
            Optional<AccountLink> link = linkLookup.join();
            if (!link.isPresent()) {
                return Task.completed(new ProfileImpl(discordSRV, playerUUID, null, gameProfile, null));
            }

            long userId = link.get().userId();
            return loadDiscordData(userId)
                    .thenApply(discordProfile -> new ProfileImpl(discordSRV, playerUUID, userId, gameProfile, discordProfile));
        });
    }

    @Override
    public @Nullable ProfileImpl getCachedProfile(UUID playerUUID) {
        return profiles.get(playerUUID);
    }

    @Override
    public @NotNull Task<@NotNull ProfileImpl> queryProfile(long userId) {
        LinkProvider linkProvider = discordSRV.linkProvider();
        Task<Optional<AccountLink>> linkLookup = linkProvider != null ? linkProvider.get(userId) : Task.completed(Optional.empty());

        Task<DiscordProfileData> discordProfileLookup = loadDiscordData(userId);
        return Task.allOf(discordProfileLookup, linkLookup).then(v -> {
            DiscordProfileData discordProfile = discordProfileLookup.join();
            Optional<AccountLink> link = linkLookup.join();
            if (!link.isPresent()) {
                return Task.completed(new ProfileImpl(discordSRV, null, userId, null, discordProfile));
            }

            UUID playerUUID = link.get().playerUUID();
            return loadGameData(playerUUID)
                    .thenApply(gameProfile -> new ProfileImpl(discordSRV, playerUUID, userId, gameProfile, discordProfile));
        });
    }

    @Override
    public @Nullable ProfileImpl getCachedProfile(long userId) {
        return discordUserMap.get(userId);
    }
}
