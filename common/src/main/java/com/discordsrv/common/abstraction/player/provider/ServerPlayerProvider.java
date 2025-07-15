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

package com.discordsrv.common.abstraction.player.provider;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.IPlayer;

import java.util.UUID;

public abstract class ServerPlayerProvider<T extends IPlayer, DT extends DiscordSRV> extends AbstractPlayerProvider<T, DT> {

    public ServerPlayerProvider(DT discordSRV) {
        super(discordSRV);
    }

    @Override
    public Task<UUID> lookupUUIDForUsername(String username) {
        return lookupOfflinePlayer(username).thenApply(IOfflinePlayer::uniqueId);
    }

    @Override
    public abstract Task<IOfflinePlayer> lookupOfflinePlayer(String username);

    @Override
    public abstract Task<IOfflinePlayer> lookupOfflinePlayer(UUID uuid);
}
