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
import com.discordsrv.bukkit.component.PaperComponentCheck;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.common.util.ReflectionUtil;
import net.kyori.adventure.audience.Audience;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BukkitPlayerProvider extends AbstractBukkitPlayerProvider {

    private static final boolean IS_PAPER = PaperComponentCheck.IS_AVAILABLE && !ComponentUtil.IS_RELOCATED;
    private static final boolean IS_SPIGOT = ReflectionUtil.methodExists(
            "org.bukkit.entity.Player$Spigot",
            "sendMessage",
            "net.md_5.bungee.api.chat.BaseComponent"
    );

    public BukkitPlayerProvider(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    protected BukkitPlayer makePlayer(Player player) {
        return new BukkitPlayerImpl(discordSRV, player, toAudience(player));
    }

    @Override
    protected BukkitOfflinePlayer makeOfflinePlayer(OfflinePlayer offlinePlayer) {
        return new BukkitOfflinePlayerImpl(discordSRV, offlinePlayer);
    }

    @Override
    public Audience toAudience(CommandSender commandSender) {
        if (IS_PAPER) {
            return (Audience) commandSender;
        } else if (IS_SPIGOT) {
            return new SpigotAudience(discordSRV, commandSender);
        } else {
            return new BukkitAudience(discordSRV, commandSender);
        }
    }
}
