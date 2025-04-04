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

package com.discordsrv.common.abstraction.player;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class OfflinePlayer implements IOfflinePlayer {

    private final DiscordSRV discordSRV;
    private final String username;
    private final Identity identity;
    private final SkinInfo skinInfo;

    public OfflinePlayer(DiscordSRV discordSRV, String username, UUID uuid, SkinInfo skinInfo) {
        this.discordSRV = discordSRV;
        this.username = username;
        this.identity = Identity.identity(uuid);
        this.skinInfo = skinInfo;
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @Nullable String username() {
        return username;
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        return skinInfo;
    }
}
