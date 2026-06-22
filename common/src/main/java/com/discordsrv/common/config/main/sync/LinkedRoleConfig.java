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

package com.discordsrv.common.config.main.sync;

import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.enums.SyncSide;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.util.Game;

import java.util.ArrayList;
import java.util.List;

public class LinkedRoleConfig {

    public List<Long> roleIds = new ArrayList<>();

    public List<SyncConfig> getSyncConfigs() {
        List<SyncConfig> configs = new ArrayList<>(roleIds.size());
        for (Long roleId : roleIds) {
            configs.add(new SyncConfig(roleId));
        }
        return configs;
    }

    public static class SyncConfig extends AbstractSyncConfig<SyncConfig, Game, Long> {

        public final long roleId;

        public SyncConfig(long roleId) {
            // Forced values (Minecraft -> Discord only)
            super(
                    SyncDirection.MINECRAFT_TO_DISCORD,
                    new TimerConfig(SyncSide.DISABLED, 0),
                    new TieBreakers(SyncSide.MINECRAFT, SyncSide.MINECRAFT, SyncSide.MINECRAFT),
                    UnlinkBehaviour.REMOVE_DISCORD
            );

            this.roleId = roleId;
        }

        @Override
        public boolean isSet() {
            return roleId != 0;
        }

        @Override
        public Game gameId() {
            return Game.INSTANCE;
        }

        @Override
        public Long discordId() {
            return roleId;
        }

        @Override
        public boolean isSameAs(SyncConfig otherConfig) {
            return otherConfig.roleId == this.roleId;
        }

        @Override
        public String describe() {
            return Long.toUnsignedString(roleId);
        }
    }
}
