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

import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.util.ReflectionUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.ApiStatus;

import java.net.URL;
import java.util.Collection;

public final class SpigotPlayerUtil {

    private SpigotPlayerUtil() {}

    @ApiStatus.AvailableSince("Spigot 1.18.1")
    public static boolean SKIN_AVAILABLE = ReflectionUtil.classExists("org.bukkit.profile.PlayerTextures");

    public static SkinInfo getSkinInfo(OfflinePlayer player) {
        PlayerTextures textures = player.getPlayerProfile().getTextures();
        URL skinUrl = textures.getSkin();
        if (skinUrl == null) {
            return null;
        }

        return new SkinInfo(skinUrl, textures.getSkinModel().name(), null);
    }

    @ApiStatus.AvailableSince("Spigot 1.19")
    public static boolean CHAT_SUGGESTIONS_AVAILABLE = isChatSuggestionsAvailable();

    private static boolean isChatSuggestionsAvailable() {
        try {
            Player.class.getMethod("addCustomChatCompletions", Collection.class);
            Player.class.getMethod("removeCustomChatCompletions", Collection.class);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    public static void addChatSuggestions(Player player, Collection<String> suggestions) {
        player.addCustomChatCompletions(suggestions);
    }

    public static void removeChatSuggestions(Player player, Collection<String> suggestions) {
        player.removeCustomChatCompletions(suggestions);
    }

    @ApiStatus.AvailableSince("Spigot 1.16.1")
    public static boolean LOCALE_AVAILABLE = isLocaleAvailable();

    private static boolean isLocaleAvailable() {
        try {
            Player.class.getMethod("getLocale");
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    public static String getLocale(Player player) {
        return player.getLocale();
    }
}
