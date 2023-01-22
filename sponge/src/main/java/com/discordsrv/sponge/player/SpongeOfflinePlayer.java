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

package com.discordsrv.sponge.player;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.player.IOfflinePlayer;
import com.discordsrv.sponge.SpongeDiscordSRV;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.entity.living.player.User;

public class SpongeOfflinePlayer implements IOfflinePlayer {

    protected final SpongeDiscordSRV discordSRV;
    private final User user;

    public SpongeOfflinePlayer(SpongeDiscordSRV discordSRV, User user) {
        this.discordSRV = discordSRV;
        this.user = user;
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @NotNull String username() {
        return user.name();
    }

    @Override
    public @NotNull Identity identity() {
        return user.profile();
    }
}
