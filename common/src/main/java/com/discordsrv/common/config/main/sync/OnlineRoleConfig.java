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
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class OnlineRoleConfig {

    @Comment("The ID of the role to sync to the linked Discord users of online players")
    public long roleId;

    @Comment("How long after joining should the role be added")
    public long delayAddingRoleByMs = 2000;

    @Comment("Should the role be given to vanished players as well, when false vanished players will not have the role")
    public boolean giveRoleToVanishedPlayers = false;

    public SyncConfig syncConfig() {
        return new SyncConfig(roleId);
    }

    public static class SyncConfig extends AbstractSyncConfig<OnlineRoleConfig.SyncConfig, Game, Long> {

        public final long roleId;

        public SyncConfig(long roleId) {
            this.roleId = roleId;

            // Forced values
            timer.side = SyncSide.DISABLED;
            tieBreakers.join = SyncSide.DISABLED; // Handled separately

            direction = SyncDirection.MINECRAFT_TO_DISCORD;
            tieBreakers.link = SyncSide.MINECRAFT;
            tieBreakers.resyncCommand = SyncSide.MINECRAFT;

            unlinkBehaviour = UnlinkBehaviour.REMOVE_DISCORD;
        }

        @Override
        public boolean isSet() {
            return roleId != 0L;
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
        public boolean isSameAs(OnlineRoleConfig.SyncConfig otherConfig) {
            return false;
        }

        @Override
        public String describe() {
            return Long.toUnsignedString(roleId);
        }
    }
}
