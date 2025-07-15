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

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.RoleSyncModuleUtil;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.sync.LinkedRoleConfig;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.Game;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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
    public Boolean getRemovedState() {
        return false;
    }

    @Override
    protected Task<Boolean> getDiscord(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone) {
        return RoleSyncModuleUtil.hasRole(discordSRV, someone, config.roleId);
    }

    @Override
    protected Task<Boolean> getGame(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone) {
        // Checks that the user is linked
        return someone.resolve().thenApply(Objects::nonNull);
    }

    @Override
    protected Task<ISyncResult> applyDiscord(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone, Boolean newState) {
        return RoleSyncModuleUtil.doRoleChange(discordSRV, someone, config.roleId, newState);
    }

    @Override
    protected Task<ISyncResult> applyGame(LinkedRoleConfig.SyncConfig config, Someone.Resolved someone, Boolean newState) {
        // One-way sync
        return Task.completed(GenericSyncResults.WRONG_DIRECTION);
    }
}
