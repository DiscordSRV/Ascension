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

package com.discordsrv.common.abstraction.sync.result;

public enum GenericSyncResults implements ISyncResult {

    // Success, actioned
    ADD_DISCORD("Add %d", true),
    REMOVE_DISCORD("Remove %d", true),
    ADD_GAME("Add %g", true),
    REMOVE_GAME("Remove %g", true),

    // Success, Nothing done
    BOTH_TRUE("Both true"),
    BOTH_FALSE("Both false"),
    WRONG_DIRECTION("Wrong direction"),

    // Fail
    NOT_LINKED("Accounts not linked"),

    // Error
    MODULE_NOT_FOUND("Game data not available", false, true),
    GUILD_NOT_FOUND("Discord server not found", false, true),

    ;

    public static GenericSyncResults both(boolean value) {
        return value ? BOTH_TRUE : BOTH_FALSE;
    }

    private final String message;
    private final boolean update;
    private final boolean success;

    GenericSyncResults(String message) {
        this(message, false);
    }

    GenericSyncResults(String message, boolean update) {
        this(message, update, true);
    }

    GenericSyncResults(String message, boolean update, boolean success) {
        this.message = message;
        this.update = update;
        this.success = success;
    }

    @Override
    public boolean isError() {
        return !success;
    }

    @Override
    public boolean isUpdate() {
        return update;
    }

    @Override
    public String getFormat() {
        return message;
    }
}
