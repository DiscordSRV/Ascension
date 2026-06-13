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

package com.discordsrv.bukkit.module;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.PaperPlayerUtil;
import com.discordsrv.bukkit.player.SpigotPlayerUtil;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.provider.PlayerSkinProvider;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.core.module.type.AbstractModule;
import org.bukkit.OfflinePlayer;

public class BukkitSkinProvider extends AbstractModule<BukkitDiscordSRV> implements PlayerSkinProvider.Platform {

    public BukkitSkinProvider(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public SkinInfo getSkinForPlayer(IOfflinePlayer player) {
        if (PaperPlayerUtil.SKIN_AVAILABLE_ONLINE) {
            return PaperPlayerUtil.getSkinInfo((OfflinePlayer) player);
        }
        if (SpigotPlayerUtil.SKIN_AVAILABLE) {
            return SpigotPlayerUtil.getSkinInfo((OfflinePlayer) player);
        }
        return null;
    }
}
