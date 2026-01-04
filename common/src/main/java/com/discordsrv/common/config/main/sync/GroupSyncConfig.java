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

package com.discordsrv.common.config.main.sync;

import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.config.main.generic.SyncConfig;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.*;
import java.util.stream.Collectors;

@ConfigSerializable
public class GroupSyncConfig {

    public List<SetConfig> sets = new ArrayList<>(Collections.singletonList(new SetConfig()));

    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        for (SetConfig set : sets) {
            for (PairConfig pair : set.pairs) {
                entries.add(new Entry(
                        pair.groupName,
                        pair.roleId,
                        set.contexts,
                        set.includeInherited,
                        set.direction,
                        set.timer,
                        set.tieBreakers
                ));
            }
        }
        return entries;
    }

    @ConfigSerializable
    public static class SetConfig extends SyncConfig {

        @Comment("LuckPerms context values, used when adding, removing and checking the groups of players.\n"
                + "The format is: {\"context\": [\"value\"]}")
        public Map<String, List<String>> contexts = new LinkedHashMap<>();

        @Comment("If inherited groups should be included when checking if the Player has a group. Only works with LuckPerms")
        public boolean includeInherited = false;

        @Comment("The pairs of case-sensitive Minecraft group names from your permission plugin, and Discord role ids")
        @Order(1)
        public List<PairConfig> pairs = new ArrayList<>(Collections.singletonList(new PairConfig()));

    }

    @ConfigSerializable
    public static class PairConfig {

        public String groupName = "";
        public Long roleId = 0L;

    }

    public static class Entry extends AbstractSyncConfig<Entry, String, Long> {

        public final String groupName;
        public final long roleId;
        public final Map<String, List<String>> contexts;
        private final boolean includeInherited;

        public Entry(
                String groupName,
                long roleId,
                Map<String, List<String>> contexts,
                boolean includeInherited,
                SyncDirection direction,
                TimerConfig timer,
                TieBreakers tieBreakers
        ) {
            this.groupName = groupName;
            this.roleId = roleId;
            this.contexts = contexts;
            this.includeInherited = includeInherited;
            this.direction = direction;
            this.timer = timer;
            this.tieBreakers = tieBreakers;
        }

        public boolean isSet() {
            return roleId != 0 && StringUtils.isNotEmpty(groupName);
        }

        @Override
        public String gameId() {
            return makeGameId(groupName, contexts);
        }

        @Override
        public Long discordId() {
            return roleId;
        }

        @Override
        public boolean isSameAs(Entry config) {
            return groupName.equals(config.groupName) && Objects.equals(roleId, config.roleId);
        }

        @Override
        public String toString() {
            return "GroupSyncConfig$Entry{" + describe() + '}';
        }

        @Override
        public String describe() {
            return groupName + direction.arrow() + Long.toUnsignedString(roleId);
        }

        public Map<String, Set<String>> contexts() {
            Map<String, Set<String>> contexts = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : this.contexts.entrySet()) {
                Set<String> values = new LinkedHashSet<>(entry.getValue());
                contexts.put(entry.getKey(), values);
            }
            return contexts;
        }

        public boolean includeInherited() {
            return includeInherited;
        }

        public static String makeGameId(String groupName, Map<String, ? extends Collection<String>> contexts) {
            String joinedContexts = contexts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        List<String> values = new ArrayList<>(entry.getValue());
                        Collections.sort(values);
                        return entry.getKey() + "=" + String.join(",", values);
                    })
                    .collect(Collectors.joining(";"));
            return groupName + ">" + joinedContexts;
        }
    }

}
