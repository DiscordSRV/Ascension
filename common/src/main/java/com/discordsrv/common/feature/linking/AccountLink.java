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

package com.discordsrv.common.feature.linking;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountLink {

    private final UUID playerUUID;
    private final long userId;
    private final LocalDateTime created;
    private final LocalDateTime lastSeen;

    public AccountLink(UUID playerUUID, long userId, LocalDateTime created, LocalDateTime lastSeen) {
        this.playerUUID = playerUUID;
        this.userId = userId;
        this.created = created;
        this.lastSeen = lastSeen;
    }

    public UUID playerUUID() {
        return playerUUID;
    }

    public long userId() {
        return userId;
    }

    public LocalDateTime created() {
        return created;
    }

    public LocalDateTime lastSeen() {
        return lastSeen;
    }
}
