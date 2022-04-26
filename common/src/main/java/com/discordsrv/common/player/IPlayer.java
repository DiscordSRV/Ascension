/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.player;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.permission.util.PermissionUtil;
import com.discordsrv.common.profile.Profile;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IPlayer extends DiscordSRVPlayer, IOfflinePlayer, ICommandSender {

    @Override
    DiscordSRV discordSRV();

    @ApiStatus.NonExtendable
    default Profile profile() {
        return discordSRV().profileManager().getProfile(uniqueId()).orElseThrow(IllegalStateException::new);
    }

    @NotNull
    @Placeholder("player_name")
    String username();

    @Override
    @ApiStatus.NonExtendable
    @Placeholder("player_uuid")
    default @NotNull UUID uniqueId() {
        return identity().uuid();
    }

    @NotNull
    @Placeholder("player_display_name")
    Component displayName();

    @Nullable
    @ApiStatus.NonExtendable
    @Placeholder("player_avatar_url")
    default String getAvatarUrl(OrDefault<BaseChannelConfig> config) {
        String avatarUrlProvider = config.get(cfg -> cfg.avatarUrlProvider);
        if (avatarUrlProvider == null) {
            return null;
        }

        return new Placeholders(avatarUrlProvider)
                .replace("%uuid%", uniqueId().toString())
                .replace("%username%", username())
                .replace("%texture%", "") // TODO
                .toString();
    }

    @Nullable
    @ApiStatus.NonExtendable
    @Placeholder("player_meta_prefix")
    default Component getMetaPrefix() {
        return PermissionUtil.getMetaPrefix(discordSRV(), uniqueId());
    }

    @Nullable
    @ApiStatus.NonExtendable
    @Placeholder("player_meta_suffix")
    default Component getMetaSuffix() {
        return PermissionUtil.getMetaSuffix(discordSRV(), uniqueId());
    }

    @Nullable
    @ApiStatus.NonExtendable
    @Placeholder("player_prefix")
    default Component getPrefix() {
        return PermissionUtil.getPrefix(discordSRV(), uniqueId());
    }

    @Nullable
    @ApiStatus.NonExtendable
    @Placeholder("player_suffix")
    default Component getSuffix() {
        return PermissionUtil.getSuffix(discordSRV(), uniqueId());
    }

}
