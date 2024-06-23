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

package com.discordsrv.common.groupsync;

import com.discordsrv.common.config.main.GroupSyncConfig;
import com.discordsrv.common.groupsync.enums.GroupSyncCause;
import com.discordsrv.common.groupsync.enums.GroupSyncResult;

import java.util.*;

public class SynchronizationSummary {

    private final EnumMap<GroupSyncResult, Set<GroupSyncConfig.PairConfig>> pairs = new EnumMap<>(GroupSyncResult.class);
    private final UUID player;
    private final GroupSyncCause cause;

    public SynchronizationSummary(UUID player, GroupSyncCause cause, GroupSyncConfig.PairConfig config, GroupSyncResult result) {
        this(player, cause);
        add(config, result);
    }

    public SynchronizationSummary(UUID player, GroupSyncCause cause) {
        this.player = player;
        this.cause = cause;
    }

    public void add(GroupSyncConfig.PairConfig config, GroupSyncResult result) {
        pairs.computeIfAbsent(result, key -> new LinkedHashSet<>()).add(config);
    }

    public boolean anySuccess() {
        for (GroupSyncResult result : pairs.keySet()) {
            if (result.isSuccess()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        int count = pairs.size();
        StringBuilder message = new StringBuilder(
                "Group synchronization (of " + count + " pair" + (count == 1 ? "" : "s") + ") for " + player + " (" + cause + ")");

        for (Map.Entry<GroupSyncResult, Set<GroupSyncConfig.PairConfig>> entry : pairs.entrySet()) {
            message.append(count == 1 ? ": " : "\n")
                    .append(entry.getKey().toString())
                    .append(": ")
                    .append(entry.getValue().toString());
        }
        return message.toString();
    }
}
