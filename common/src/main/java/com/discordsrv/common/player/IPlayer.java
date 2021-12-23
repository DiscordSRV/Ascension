/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IPlayer extends DiscordSRVPlayer, IOfflinePlayer, ICommandSender {

    DiscordSRV discordSRV();

    @Override
    @NotNull
    String username();

    @Override
    @ApiStatus.NonExtendable
    default @NotNull UUID uniqueId() {
        return identity().uuid();
    }

    @NotNull
    @Placeholder("player_display_name")
    Component displayName();

    @Nullable
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

}
