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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.config.main.AvatarProviderConfig;
import com.discordsrv.common.util.UUIDUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class AvatarProviderContext {

    private final DiscordSRV discordSRV;

    public AvatarProviderContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    private AvatarProviderConfig config() {
        return discordSRV.config().avatarProvider;
    }

    private String getDefaultAvatarUrl(IPlayer player) {
        AvatarProviderConfig config = config();
        return discordSRV.placeholderService().replacePlaceholders(config.defaultUrl, player);
    }

    private String getUrlByTemplate(IPlayer player) {
        AvatarProviderConfig.Services config = config().services;

        UUID playerUUID = player.uniqueId();
        SkinInfo skinInfo = player.skinInfo();

        String urlTemplate = null;
        if (skinInfo != null) {
            urlTemplate = config.textureTemplate;
        }

        if (StringUtils.isEmpty(urlTemplate)) {
            if (UUIDUtil.isGeyser(playerUUID)) {
                urlTemplate = config.floodgateTemplate;
            } else if (UUIDUtil.isOffline(playerUUID)) {
                urlTemplate = config.offlineTemplate;
            } else {
                urlTemplate = config.onlineUuidTemplate;
            }
        }

        if (StringUtils.isEmpty(urlTemplate)) {
            return null;
        }

        return discordSRV.placeholderService().replacePlaceholders(urlTemplate, player);
    }

    @Placeholder("player_avatar_url")
    public String getAvatarUrl(IPlayer player) {
        AvatarProviderConfig config = discordSRV.config().avatarProvider;

        if (config.provider != AvatarProviderConfig.Provider.OFF) {
            String avatarUrl = getUrlByTemplate(player);
            if (avatarUrl != null) {
                return avatarUrl;
            }
        }

        return getDefaultAvatarUrl(player);
    }
}
