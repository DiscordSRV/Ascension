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

package com.discordsrv.common.sync.result;

public enum GenericSyncResults implements ISyncResult {

    // Success, actioned
    ADD_DISCORD("Add %d"),
    REMOVE_DISCORD("Remove %d"),
    ADD_GAME("Add %g"),
    REMOVE_GAME("Remove %g"),

    // Success, Nothing done
    BOTH_TRUE("Both true"),
    BOTH_FALSE("Both false"),
    WRONG_DIRECTION("Wrong direction"),

    // Error
    NOT_LINKED("Accounts not linked"),

    ;

    public static GenericSyncResults both(boolean value) {
        return value ? BOTH_TRUE : BOTH_FALSE;
    }

    private final String message;
    private final boolean success;

    GenericSyncResults(String message) {
        this(message, true);
    }

    GenericSyncResults(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String getFormat() {
        return message;
    }
}
