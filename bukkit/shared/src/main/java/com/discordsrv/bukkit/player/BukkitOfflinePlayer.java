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

package com.discordsrv.bukkit.player;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import net.kyori.adventure.identity.Identity;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public abstract class BukkitOfflinePlayer implements IOfflinePlayer {

    protected final BukkitDiscordSRV discordSRV;
    protected final OfflinePlayer offlinePlayer;
    private final Identity identity;

    public BukkitOfflinePlayer(BukkitDiscordSRV discordSRV, @NotNull OfflinePlayer offlinePlayer) {
        this.discordSRV = discordSRV;
        this.offlinePlayer = offlinePlayer;
        this.identity = Identity.identity(offlinePlayer.getUniqueId());
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public String username() {
        return offlinePlayer.getName();
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }
}
