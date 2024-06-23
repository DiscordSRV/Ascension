/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.groupsync.enums;

import com.discordsrv.common.sync.cause.ISyncCause;

public enum GroupSyncCause implements ISyncCause {

    DISCORD_ROLE_CHANGE("Discord role changed", true),
    LUCKPERMS_NODE_CHANGE("LuckPerms node changed", true),
    LUCKPERMS_TRACK("LuckPerms track promotion/demotion"),
    ;

    private final String prettyCause;
    private final boolean discordSRVCanCause;

    GroupSyncCause(String prettyCause) {
        this(prettyCause, false);
    }

    GroupSyncCause(String prettyCause, boolean discordSRVCanCause) {
        this.prettyCause = prettyCause;
        this.discordSRVCanCause = discordSRVCanCause;
    }

    public boolean isDiscordSRVCanCause() {
        return discordSRVCanCause;
    }

    @Override
    public String toString() {
        return prettyCause;
    }
}
