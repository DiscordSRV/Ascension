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

package com.discordsrv.common.feature.linking;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.sync.LinkedRoleConfig;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.Game;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;

/**
 * One-way sync linking status -> Discord roles.
 */
public class LinkedRoleModule extends AbstractSyncModule<DiscordSRV, LinkedRoleConfig.SyncConfig, Game, Long, Boolean> {

    public LinkedRoleModule(DiscordSRV discordSRV) {
        super(discordSRV, "LINKED_ROLE");
    }

    @Override
    protected String syncName() {
        return "Linked Role";
    }

    @Override
    protected @Nullable String logFileName() {
        return null;
    }

    @Override
    protected String gameTerm() {
        return "linking status";
    }

    @Override
    protected String discordTerm() {
        return "role";
    }

    @Override
    protected List<LinkedRoleConfig.SyncConfig> configs() {
        return discordSRV.config().linkedRole.getSyncConfigs();
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(Boolean one, Boolean two) {
        return (one == two) ? GenericSyncResults.both(one) : null;
    }

    @Override
    protected Task<Boolean> getDiscord(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId());
        if (role == null) {
            return Task.failed(new SyncFail(GenericSyncResults.ROLE_DOESNT_EXIST));
        }

        DiscordGuild guild = role.getGuild();
        if (!guild.getSelfMember().canInteract(role)) {
            return Task.failed(new SyncFail(GenericSyncResults.ROLE_CANNOT_INTERACT));
        }

        return someone.guildMember(guild).mapException(RestErrorResponseException.class, ex -> {
            if (ex.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                throw new SyncFail(GenericSyncResults.NOT_A_GUILD_MEMBER);
            }
            throw new CompletionException(ex);
        }).thenApply(member -> member.getRoles().stream().anyMatch(r -> r.getId() == role.getId()));
    }

    @Override
    protected Task<Boolean> getGame(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone) {
        return someone.resolve().thenApply(Objects::nonNull);
    }

    @Override
    protected Task<ISyncResult> applyDiscord(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone, Boolean newState) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId());
        if (role == null) {
            return Task.failed(new SyncFail(GenericSyncResults.ROLE_DOESNT_EXIST));
        }

        DiscordGuild guild = role.getGuild();
        return someone.guildMember(guild)
                .then(member -> newState
                                     ? member.addRole(role).thenApply(v -> GenericSyncResults.ADD_DISCORD)
                                     : member.removeRole(role).thenApply(v -> GenericSyncResults.REMOVE_DISCORD)
                );
    }

    @Override
    protected Task<ISyncResult> applyGame(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone, Boolean newState) {
        return Task.completed(GenericSyncResults.WRONG_DIRECTION);
    }
}
