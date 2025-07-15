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

package com.discordsrv.common.abstraction.sync;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.result.DiscordPermissionResult;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.helper.Someone;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * Shared code between sync modules that alter Discord roles.
 */
public final class RoleSyncModuleUtil {

    private RoleSyncModuleUtil() {}

    public static Task<Boolean> hasRole(DiscordSRV discordSRV, Someone.Resolved someone, long roleId) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(roleId);
        if (role == null) {
            return Task.failed(new SyncFail(GenericSyncResults.ROLE_DOESNT_EXIST));
        }

        return someone.guildMember(role.getGuild())
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(GenericSyncResults.NOT_A_GUILD_MEMBER);
                    }
                    throw t;
                })
                .thenApply(member -> member.hasRole(role));
    }

    public static Task<ISyncResult> doRoleChange(DiscordSRV discordSRV, Someone.Resolved someone, long roleId, Boolean newState) {
        return checkRoleChangePreconditions(discordSRV, roleId).then(role -> doRoleChange(someone, role, newState));
    }

    public static Task<DiscordRole> checkRoleChangePreconditions(DiscordSRV discordSRV, long roleId) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(roleId);
        if (role == null) {
            return Task.failed(new SyncFail(GenericSyncResults.ROLE_DOESNT_EXIST));
        }

        DiscordGuild guild = role.getGuild();
        if (!guild.getSelfMember().canInteract(role)) {
            return Task.failed(new SyncFail(GenericSyncResults.ROLE_CANNOT_INTERACT));
        }

        ISyncResult permissionFailReason = DiscordPermissionResult.check(
                guild.asJDA(),
                Collections.singleton(Permission.MANAGE_ROLES)
        );
        if (permissionFailReason != null) {
            return Task.failed(new SyncFail(permissionFailReason));
        }

        return Task.completed(role);
    }

    public static Task<ISyncResult> doRoleChange(Someone.Resolved someone, DiscordRole role, @Nullable Boolean newState) {
        return someone.guildMember(role.getGuild())
                .then(member -> (newState != null && newState)
                                ? member.addRole(role).thenApply(v -> GenericSyncResults.ADD_DISCORD)
                                : member.removeRole(role).thenApply(v -> GenericSyncResults.REMOVE_DISCORD)
                );
    }
}
