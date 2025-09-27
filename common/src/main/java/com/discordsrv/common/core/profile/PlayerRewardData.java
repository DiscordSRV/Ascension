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

package com.discordsrv.common.core.profile;

import java.util.Objects;

public class PlayerRewardData {

    private final Integer databaseId;
    private final String id;
    private boolean pending;

    public PlayerRewardData(Integer databaseId, String id, boolean pending) {
        this.databaseId = databaseId;
        this.id = id;
        this.pending = pending;
    }

    public Integer getDatabaseId() {
        if (databaseId == null) {
            throw new IllegalStateException("Querying id for a non-database reward");
        }
        return databaseId;
    }

    public String getId() {
        return id;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerRewardData)) return false;
        PlayerRewardData that = (PlayerRewardData) o;
        return Objects.equals(databaseId, that.databaseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(databaseId);
    }
}
