/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.profile.IProfile;
import com.discordsrv.common.DiscordSRV;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfileManager implements IProfileManager {

    private final DiscordSRV discordSRV;

    public ProfileManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public CompletableFuture<Optional<IProfile>> lookupProfile(UUID playerUUID) {
        return discordSRV.linkProvider().getUserId(playerUUID)
                .thenApply(opt -> Optional.of(new Profile(playerUUID, opt.orElse(null))));
    }

    @Override
    public Optional<IProfile> getProfile(UUID playerUUID) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<IProfile>> lookupProfile(long userId) {
        return discordSRV.linkProvider().getPlayerUUID(userId)
                .thenApply(opt -> Optional.of(new Profile(opt.orElse(null), userId)));
    }

    @Override
    public Optional<IProfile> getProfile(long userId) {
        return Optional.empty();
    }
}
