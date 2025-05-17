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

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.util.ReflectionUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.ApiStatus;

import java.net.URL;
import java.util.Locale;

public final class PaperPlayerUtil {

    private PaperPlayerUtil() {}

    private static final PaperComponentHandle.Set<Player> KICK_HANDLE =
            PaperComponentHandle.setOrNull(Player.class, "kick");

    /**
     * @see com.discordsrv.bukkit.component.PaperComponentHandle#IS_AVAILABLE
     */
    public static void kick(Player player, MinecraftComponent component) {
        if (KICK_HANDLE == null) {
            throw new IllegalStateException("Not available");
        }
        KICK_HANDLE.call(player, component);
    }

    private static final PaperComponentHandle.Get<Player> DISPLAY_NAME_HANDLE =
            PaperComponentHandle.getOrNull(Player.class, "displayName");

    /**
     * @see com.discordsrv.bukkit.component.PaperComponentHandle#IS_AVAILABLE
     */
    public static MinecraftComponent displayName(Player player) {
        if (DISPLAY_NAME_HANDLE == null) {
            throw new IllegalStateException("Not available");
        }
        return DISPLAY_NAME_HANDLE.getAPI(player);
    }

    private static final PaperComponentHandle.Get<Player> TEAM_DISPLAY_NAME_HANDLE =
            PaperComponentHandle.getOrNull(Player.class, "teamDisplayName");
    public static final boolean TEAM_DISPLAY_NAME_AVAILABLE = TEAM_DISPLAY_NAME_HANDLE != null;

    /**
     * @see #TEAM_DISPLAY_NAME_AVAILABLE
     */
    public static MinecraftComponent teamDisplayName(Player player) {
        if (TEAM_DISPLAY_NAME_HANDLE == null) {
            throw new IllegalStateException("Not available");
        }
        return TEAM_DISPLAY_NAME_HANDLE.getAPI(player);
    }

    @ApiStatus.AvailableSince("Paper 1.16")
    public static final boolean LOCALE_SUPPORTED = ReflectionUtil.methodExists(Player.class, "locale", new Class[0]);

    public static Locale locale(Player player) {
        return player.locale();
    }

    private static final boolean SKIN_AVAILABLE = ReflectionUtil.classExists("com.destroystokyo.paper.profile.PlayerProfile")
            && ReflectionUtil.methodExists("com.destroystokyo.paper.profile.PlayerProfile", "getTextures");
    public static final boolean SKIN_AVAILABLE_ONLINE = SKIN_AVAILABLE && ReflectionUtil.methodExists(Player.class, "getPlayerProfile", new String[0]);
    public static final boolean SKIN_AVAILABLE_OFFLINE = SKIN_AVAILABLE && ReflectionUtil.methodExists(OfflinePlayer.class, "getPlayerProfile", new String[0]);

    @SuppressWarnings("RedundantCast") // Not redundant
    public static SkinInfo getSkinInfo(OfflinePlayer player) {
        PlayerProfile playerProfile;
        if (player instanceof Player) {
            playerProfile = ((Player) player).getPlayerProfile();
        } else {
            playerProfile = player.getPlayerProfile();
        }
        if (!playerProfile.hasTextures()) {
            return null;
        }

        PlayerTextures textures = playerProfile.getTextures();
        URL skinURL = textures.getSkin();
        if (skinURL == null) {
            return null;
        }

        SkinParts skinParts = player instanceof Player
                              ? ((Player) player).getClientOption(ClientOption.SKIN_PARTS)
                              : null;

        SkinInfo.Parts parts = null;
        if (skinParts != null) {
            parts = new SkinInfo.Parts(
                    skinParts.hasCapeEnabled(),
                    skinParts.hasJacketEnabled(),
                    skinParts.hasLeftSleeveEnabled(),
                    skinParts.hasRightSleeveEnabled(),
                    skinParts.hasLeftPantsEnabled(),
                    skinParts.hasRightPantsEnabled(),
                    skinParts.hasHatsEnabled()
            );
        }

        return new SkinInfo(skinURL, textures.getSkinModel().name(), parts);
    }

}
