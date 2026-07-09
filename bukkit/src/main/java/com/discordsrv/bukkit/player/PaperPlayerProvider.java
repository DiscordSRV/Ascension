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

package com.discordsrv.bukkit.player;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import net.kyori.adventure.audience.Audience;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PaperPlayerProvider extends AbstractBukkitPlayerProvider {

    public PaperPlayerProvider(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    protected BukkitPlayer makePlayer(Player player) {
        return new BukkitPlayerImpl(discordSRV, player, () -> (Audience) player);
    }

    @Override
    protected BukkitOfflinePlayer makeOfflinePlayer(OfflinePlayer offlinePlayer) {
        return new BukkitOfflinePlayerImpl(discordSRV, offlinePlayer);
    }

    @Override
    public Audience toAudience(CommandSender commandSender) {
        return (Audience) commandSender;
    }
}
