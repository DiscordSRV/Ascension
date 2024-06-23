/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.common.player.provider.model.SkinInfo;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Locale;

public final class SpigotPlayer {

    private SpigotPlayer() {}

    private static final boolean playerProfileExists;

    static {
        Class<?> playerClass = Player.class;

        boolean playerProfile = false;
        try {
            playerClass.getMethod("getPlayerProfile");
            playerProfile = true;
        } catch (ReflectiveOperationException ignored) {}
        playerProfileExists = playerProfile;
    }

    public static SkinInfo getSkinInfo(Player player) {
        if (!playerProfileExists) {
            return null;
        }

        PlayerTextures textures = player.getPlayerProfile().getTextures();

        URL skinUrl = textures.getSkin();
        if (skinUrl == null) {
            return null;
        }

        String skinUrlPlain = skinUrl.toString();
        return new SkinInfo(
                skinUrlPlain.substring(skinUrlPlain.lastIndexOf('/') + 1),
                textures.getSkinModel().toString().toLowerCase(Locale.ROOT)
        );
    }
}
