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

package com.discordsrv.common.config.helper;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.util.TaskUtil;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

public abstract class ConfigMessage {

    public void sendTo(CommandExecution execution) {
        sendTo(execution, null, null, null, null);
    }

    public void sendTo(CommandExecution execution, @Nullable DiscordSRV discordSRV, @Nullable Long userId, @Nullable UUID playerUUID, @Nullable String integrationId) {
        if (discordSRV == null) {
            sendTo(
                    execution,
                    new SinglePlaceholder("user_id", userId),
                    new SinglePlaceholder("player_uuid", playerUUID),
                    new SinglePlaceholder("integration_id", integrationId)
            );
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
        Task<PluginIntegration<?>> integrationFuture =
                integrationId == null ? Task.completed(null) :
                    Task.completed(discordSRV.moduleManager().getModules(PluginIntegration.class, true)
                    .stream().filter(integration -> integration.getIntegrationId().equalsIgnoreCase(integrationId))
                    .findFirst().orElse(null));

        playerFuture.whenComplete((player, __) -> userFuture.whenComplete((user, ___) -> integrationFuture.whenComplete((integration, _____) -> sendTo(
                execution,
                new SinglePlaceholder("user_id", userId),
                new SinglePlaceholder("player_uuid", playerUUID),
                new SinglePlaceholder("integration_id", integrationId),
                user,
                player,
                integration
        ))));
    }

    protected abstract void sendTo(CommandExecution execution, Object... context);
}
