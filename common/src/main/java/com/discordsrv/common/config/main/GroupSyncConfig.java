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

package com.discordsrv.common.config.main;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.*;

@ConfigSerializable
public class GroupSyncConfig {

    @Comment("Group-Role pairs for group synchronization\n"
            + "\n"
            + "If you are not using LuckPerms and want to use Minecraft -> Discord synchronization, you must specify timed synchronization")
    public List<PairConfig> pairs = new ArrayList<>(Collections.singletonList(new PairConfig()));

    @ConfigSerializable
    public static class PairConfig extends AbstractSyncConfig<PairConfig, String, Long> {

        @Comment("The case-sensitive group name from your permissions plugin")
        public String groupName = "";

        @Comment("The Discord role id")
        public Long roleId = 0L;

        @Comment("The LuckPerms \"%1\" context value, used when adding, removing and checking the groups of players.\n"
                + "Make this blank (\"\") to use the current server's value, or \"%2\" to not use the context")
        @Constants.Comment({"server", "global"})
        public String serverContext = "global";

        public boolean isSet() {
            return roleId != 0 && StringUtils.isNotEmpty(groupName);
        }

        @Override
        public String gameId() {
            return makeGameId(groupName, serverContext != null ? Collections.singleton(serverContext) : null);
        }

        @Override
        public Long discordId() {
            return roleId;
        }

        @Override
        public boolean isSameAs(PairConfig config) {
            return groupName.equals(config.groupName) && Objects.equals(roleId, config.roleId);
        }

        @Override
        public String toString() {
            return "GroupSyncConfig$PairConfig{" + describe() + '}';
        }

        @Override
        public String describe() {
            return groupName + direction.arrow() + Long.toUnsignedString(roleId);
        }

        public static String makeGameId(String groupName, Set<String> serverContext) {
            return groupName + (serverContext != null ? String.join(" ", serverContext) : "");
        }
    }

}
