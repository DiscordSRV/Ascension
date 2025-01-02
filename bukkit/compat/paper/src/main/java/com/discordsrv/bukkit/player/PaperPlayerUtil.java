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

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.util.ReflectionUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;

public final class PaperPlayerUtil {

    private PaperPlayerUtil() {}

    private static final PaperComponentHandle.Set<Player> KICK_HANDLE =
            PaperComponentHandle.set(Player.class, "kick");

    /**
     * @see com.discordsrv.bukkit.component.PaperComponentHandle#IS_AVAILABLE
     */
    public static void kick(Player player, MinecraftComponent component) {
        KICK_HANDLE.call(player, component);
    }

    private static final PaperComponentHandle.Get<Player> DISPLAY_NAME_HANDLE =
            PaperComponentHandle.getOrNull(Player.class, "displayName");

    /**
     * @see com.discordsrv.bukkit.component.PaperComponentHandle#IS_AVAILABLE
     */
    public static MinecraftComponent displayName(Player player) {
        if (DISPLAY_NAME_HANDLE == null) {
            return null;
        }
        return DISPLAY_NAME_HANDLE.getAPI(player);
    }

    @ApiStatus.AvailableSince("Paper 1.16")
    public static boolean LOCALE_SUPPORTED = ReflectionUtil.methodExists(Player.class, "locale", new Class[0]);

    public static Locale locale(Player player) {
        return player.locale();
    }

}
