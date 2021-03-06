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

package com.discordsrv.common.player;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.profile.Profile;
import net.kyori.adventure.identity.Identified;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IOfflinePlayer extends Identified {

    DiscordSRV discordSRV();

    @ApiStatus.NonExtendable
    default CompletableFuture<Profile> lookupProfile() {
        return discordSRV().profileManager().lookupProfile(uniqueId());
    }

    @Placeholder("player_name")
    @Nullable
    String username();

    @ApiStatus.NonExtendable
    @Placeholder("player_uuid")
    @NotNull
    default UUID uniqueId() {
        return identity().uuid();
    }
}
