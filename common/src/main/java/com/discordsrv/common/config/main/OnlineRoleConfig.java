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

package com.discordsrv.common.config.main;

import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.enums.SyncSide;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.util.Game;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class OnlineRoleConfig extends AbstractSyncConfig<OnlineRoleConfig, Game, Long> {

    public OnlineRoleConfig() {
        // Overriding defaults
        timer.enabled = false;
        direction = SyncDirection.MINECRAFT_TO_DISCORD;
        tieBreaker = SyncSide.MINECRAFT;
    }

    @Comment("The ID of the role to sync to the linked Discord users of online players")
    public long roleId;

    public boolean isSet() {
        return roleId != 0L;
    }

    public Game gameId() {
        return Game.INSTANCE;
    }

    public Long discordId() {
        return roleId;
    }

    public boolean isSameAs(OnlineRoleConfig otherConfig) {
        return false;
    }

    public String describe() {
        return Long.toUnsignedString(roleId);
    }
}
