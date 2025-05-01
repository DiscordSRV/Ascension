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

package com.discordsrv.common.config.helper;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.util.TaskUtil;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class ConfigMessage {

    public void sendTo(CommandExecution execution) {
        sendTo(execution, null, null, null);
    }

    public void sendTo(CommandExecution execution, @Nullable DiscordSRV discordSRV, @Nullable Long userId, @Nullable UUID playerUUID) {
        if (discordSRV == null) {
            sendTo(execution, null, userId, null, playerUUID, null);
            return;
        }

        Task<IOfflinePlayer> playerFuture = playerUUID == null ? Task.completed(null) : TaskUtil.timeout(
                discordSRV,
                discordSRV.playerProvider().lookupOfflinePlayer(playerUUID),
                Duration.ofSeconds(5)
        );
        Task<DiscordUser> userFuture = userId == null ? Task.completed(null) : TaskUtil.timeout(
                discordSRV,
                discordSRV.discordAPI().retrieveUserById(userId),
                Duration.ofSeconds(5)
        );

        playerFuture.whenComplete((player, __) -> userFuture.whenComplete((user, ___) -> {
            List<Object> context = new ArrayList<>(4);
            context.add(new SinglePlaceholder("user_id", userId));
            context.add(new SinglePlaceholder("player_uuid", playerUUID));
            context.add(user);
            context.add(player);

            sendTo(execution, discordSRV, context);
        }));
    }

    protected abstract void sendTo(CommandExecution execution, Object... context);
}
