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

package com.discordsrv.common.feature;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.config.main.AvatarProviderConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.util.UUIDUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.function.Consumer;

public class AvatarProviderModule extends AbstractModule<DiscordSRV> {

    public static final String HEADS_DOMAIN = "heads.discordsrv.com";
    public static final String HEADS_URL_FORMAT = "https://" + HEADS_DOMAIN + "/%s/%s";

    private AvatarProviderConfig.Services services;

    public AvatarProviderModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "AVATAR_PROVIDER"));
    }

    private AvatarProviderConfig config() {
        return discordSRV.config().avatarProvider;
    }

    @Override
    public void enable() {
        discordSRV.placeholderService().addGlobalContext(this);
    }

    @Override
    public void disable() {
        discordSRV.placeholderService().removeGlobalContext(this);
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        this.services = getServices();
    }

    private AvatarProviderConfig.Services getServices() {
        AvatarProviderConfig.Services configServices = config().services;
        AvatarProviderConfig.AvatarServiceMode serviceMode = config().avatarServiceMode;

        if (serviceMode == AvatarProviderConfig.AvatarServiceMode.CONFIG_ONLY) {
            return configServices;
        }
        boolean scaled = serviceMode != AvatarProviderConfig.AvatarServiceMode.AUTO_UNSCALED;

        AvatarProviderConfig.Services active = configServices.clone();
        if (discordSRV.connectionConfig().avatarService.discordSRVHeads) {
            String resource = scaled
                              ? "%player_skin_parts_hat:'helm;head'|text:'helm'%"
                              : "%player_skin_parts_hat:'overlay;head'|text:'overlay'%";

            if (StringUtils.isEmpty(active.textureTemplate)) {
                active.textureTemplate = String.format(HEADS_URL_FORMAT, "%player_skin_texture_id%", resource);
            }
            if (StringUtils.isEmpty(active.onlineUuidTemplate)) {
                active.textureTemplate = String.format(HEADS_URL_FORMAT, "%player_uuid_short%", resource);
            }
            if (StringUtils.isEmpty(active.offlineTemplate)) {
                active.textureTemplate = String.format(HEADS_URL_FORMAT, "%player_name%", resource);
            }
        }
        return active;
    }

    @Placeholder("player_avatar_url")
    public String getAvatarUrl(IOfflinePlayer player) {
        UUID playerUUID = player.uniqueId();
        SkinInfo skinInfo = player.skinInfo();

        String urlTemplate = null;
        if (skinInfo != null) {
            urlTemplate = services.textureTemplate;
        }

        if (StringUtils.isEmpty(urlTemplate)) {
            if (UUIDUtil.isGeyser(playerUUID)) {
                urlTemplate = services.floodgateTemplate;
            } else if (UUIDUtil.isOffline(playerUUID)) {
                urlTemplate = services.offlineTemplate;
            } else {
                urlTemplate = services.onlineUuidTemplate;
            }
        }

        if (StringUtils.isEmpty(urlTemplate)) {
            urlTemplate = services.defaultUrl;
        }
        if (StringUtils.isEmpty(urlTemplate)) {
            return null;
        }

        return discordSRV.placeholderService().replacePlaceholders(urlTemplate, player);
    }
}
