/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.common.sync.ISyncResult;

public enum GroupSyncResult implements ISyncResult {

    // Errors
    ROLE_DOESNT_EXIST("Role doesn't exist"),
    ROLE_CANNOT_INTERACT("Bot doesn't have a role above the synced role (cannot interact)"),
    NOT_A_GUILD_MEMBER("User is not part of the server the role is in"),
    PERMISSION_BACKEND_FAILED("Failed to check group status, error printed"),
    UPDATE_FAILED("Failed to modify role/group, error printed"),
    NO_PERMISSION_PROVIDER("No permission provider"),
    PERMISSION_PROVIDER_NO_OFFLINE_SUPPORT("Permission provider doesn't support offline players"),

    ;

    final String prettyResult;
    final boolean success;

    GroupSyncResult(String prettyResult) {
        this(prettyResult, false);
    }

    GroupSyncResult(String prettyResult, boolean success) {
        this.prettyResult = prettyResult;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return prettyResult;
    }
}
