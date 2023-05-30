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

public enum GroupSyncResult {

    // Something happened
    ADD_GROUP("Success (group add)"),
    REMOVE_GROUP("Success (group remove)"),
    ADD_ROLE("Success (role add)"),
    REMOVE_ROLE("Success (role remove)"),

    // Nothing done
    ALREADY_IN_SYNC("Already in sync"),
    WRONG_DIRECTION("Wrong direction"),
    BOTH_TRUE("Both sides true"),
    BOTH_FALSE("Both sides false"),

    // Errors
    ROLE_DOESNT_EXIST("Role doesn't exist"),
    NOT_A_GUILD_MEMBER("User is not part of the server the role is in"),
    PERMISSION_BACKEND_FAIL_CHECK("Failed to check group status, error printed"),
    UPDATE_FAILED("Failed to modify role/group, error printed"),
    NO_PERMISSION_PROVIDER("No permission provider"),
    PERMISSION_PROVIDER_NO_OFFLINE_SUPPORT("Permission provider doesn't support offline players"),

    ;

    final String prettyResult;

    GroupSyncResult(String prettyResult) {
        this.prettyResult = prettyResult;
    }

    @Override
    public String toString() {
        return prettyResult;
    }
}
