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

package com.discordsrv.common.config.main;

import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.config.main.generic.SyncConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class OnlineSyncConfig {

    @Comment("Condition-Role pairs for online synchronization")
    public List<SetConfig> sets = new ArrayList<>(Collections.singletonList(new SetConfig()));

    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        for (SetConfig set : sets) {
            for (PairConfig pair : set.pairs) {
                entries.add(new Entry(
                        pair.conditionName,
                        pair.roleId
                ));
            }
        }
        return entries;
    }

    @ConfigSerializable
    public static class SetConfig extends SyncConfig {

        @Comment("The pairs of case-sensitive condition (online or world name) names and Discord role ids")
        @Order(1)
        public List<PairConfig> pairs = new ArrayList<>(Collections.singletonList(new PairConfig()));
    }

    @ConfigSerializable
    public static class PairConfig {

        public String conditionName = "online";
        public Long roleId = 0L;
    }

    public static class Entry extends AbstractSyncConfig<Entry, String, Long> {

        public final String conditionName;
        public final long roleId;

        public Entry(String conditionName, long roleId) {
            // Change defaults
            timer.enabled = false;
            direction = SyncDirection.MINECRAFT_TO_DISCORD;

            this.conditionName = conditionName;
            this.roleId = roleId;
        }

        @Override
        public boolean isSet() {
            return roleId != 0L && !conditionName.isEmpty();
        }

        @Override
        public String gameId() {
            return conditionName;
        }

        @Override
        public Long discordId() {
            return roleId;
        }

        @Override
        public boolean isSameAs(Entry otherConfig) {
            return false;
        }

        @Override
        public String describe() {
            return Long.toUnsignedString(roleId);
        }
    }
}
