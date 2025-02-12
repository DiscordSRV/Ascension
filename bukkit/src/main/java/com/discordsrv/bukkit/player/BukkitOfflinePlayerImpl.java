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
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitOfflinePlayerImpl extends BukkitOfflinePlayer {

    public BukkitOfflinePlayerImpl(
            BukkitDiscordSRV discordSRV,
            @NotNull OfflinePlayer offlinePlayer
    ) {
        super(discordSRV, offlinePlayer);
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        if (PaperPlayerUtil.SKIN_AVAILABLE) {
            return PaperPlayerUtil.getSkinInfo(offlinePlayer);
        }
        if (SpigotPlayerUtil.SKIN_AVAILABLE) {
            return SpigotPlayerUtil.getSkinInfo(offlinePlayer);
        }
        return null;
    }
}
