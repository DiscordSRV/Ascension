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

package com.discordsrv.common.core.debug.data;

public enum OnlineMode {

    ONLINE(true),
    OFFLINE(false),
    BUNGEE(true),
    VELOCITY(true);

    private final boolean online;

    OnlineMode(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public static OnlineMode of(boolean onlineMode) {
        return onlineMode ? OnlineMode.ONLINE : OnlineMode.OFFLINE;
    }

}
