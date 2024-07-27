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

package com.discordsrv.common.feature.groupsync.enums;

import com.discordsrv.common.abstraction.sync.result.ISyncResult;

public enum GroupSyncResult implements ISyncResult {

    // Fail
    NOT_A_GUILD_MEMBER("User is not part of the server the role is in", true),

    // Error
    ROLE_DOESNT_EXIST("Role doesn't exist"),
    ROLE_CANNOT_INTERACT("Bot doesn't have a role above the synced role (cannot interact)"),
    PERMISSION_BACKEND_FAILED("Failed to interact with permission backend, error printed"),

    ;

    private final String format;
    private final boolean success;

    GroupSyncResult(String format) {
        this(format, false);
    }

    GroupSyncResult(String format, boolean success) {
        this.format = format;
        this.success = success;
    }

    public boolean isError() {
        return !success;
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public String getFormat() {
        return format;
    }
}
