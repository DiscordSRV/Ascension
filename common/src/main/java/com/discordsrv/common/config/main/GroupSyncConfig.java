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

package com.discordsrv.common.config.main;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.groupsync.enums.GroupSyncDirection;
import com.discordsrv.common.groupsync.enums.GroupSyncSide;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.*;

@ConfigSerializable
public class GroupSyncConfig {

    @Comment("Group-Role pairs for group synchronization")
    public List<PairConfig> pairs = new ArrayList<>(Collections.singletonList(new PairConfig()));

    @ConfigSerializable
    public static class PairConfig {

        @Comment("The case-sensitive group name from your permissions plugin")
        public String groupName = "";

        @Comment("The Discord role id")
        public Long roleId = 0L;

        @Comment("The direction this group-role pair will synchronize in.\n"
                + "Valid options: %1, %2, %3")
        @Constants.Comment({"bidirectional", "minecraft_to_discord", "discord_to_minecraft"})
        public GroupSyncDirection direction = GroupSyncDirection.BIDIRECTIONAL;

        @Comment("Timed resynchronization.\n"
                + "This is required if you're not using LuckPerms and want to use Minecraft to Discord synchronization")
        public TimerConfig timer = new TimerConfig();

        @ConfigSerializable
        public static class TimerConfig {

            @Comment("If timed synchronization of this group-role pair is enabled")
            public boolean enabled = true;

            @Comment("The amount of minutes between cycles")
            public int cycleTime = 5;
        }

        @Comment("Decides which side takes priority when using timed synchronization or the resync command\n"
                + "Valid options: %1, %2")
        @Constants.Comment({"minecraft", "discord"})
        public GroupSyncSide tieBreaker = GroupSyncSide.MINECRAFT;

        @Comment("The LuckPerms \"%1\" context value, used when adding, removing and checking the groups of players.\n"
                + "Make this blank (\"\") to use the current server's value, or \"%2\" to not use the context")
        @Constants.Comment({"server", "global"})
        public String serverContext = "global";

        public boolean isTheSameAs(PairConfig config) {
            return groupName.equals(config.groupName) && Objects.equals(roleId, config.roleId);
        }

        public boolean validate(DiscordSRV discordSRV) {
            String label = "Group synchronization (" + groupName + ":" + Long.toUnsignedString(roleId) + ")";
            boolean invalidTieBreaker, invalidDirection = false;
            if ((invalidTieBreaker = (tieBreaker == null)) || (invalidDirection = (direction == null))) {
                if (invalidTieBreaker) {
                    discordSRV.logger().error(label + " has invalid tie-breaker: " + tieBreaker
                                                      + ", should be one of " + Arrays.toString(GroupSyncSide.values()));
                }
                if (invalidDirection) {
                    discordSRV.logger().error(label + " has invalid direction: " + direction
                                                      + ", should be one of " + Arrays.toString(GroupSyncDirection.values()));
                }
                return false;
            } else if (direction != GroupSyncDirection.BIDIRECTIONAL) {
                boolean minecraft;
                if ((direction == GroupSyncDirection.MINECRAFT_TO_DISCORD) != (minecraft = (tieBreaker == GroupSyncSide.MINECRAFT))) {
                    GroupSyncSide opposite = (minecraft ? GroupSyncSide.DISCORD : GroupSyncSide.MINECRAFT);
                    discordSRV.logger().warning(label + " with direction "
                                                        + direction + " with tie-breaker "
                                                        + tieBreaker + " (should be " + opposite + ")");
                    tieBreaker = opposite; // Fix the config
                }
            }
            return true;
        }

        @Override
        public String toString() {
            String arrow;
            switch (direction) {
                default:
                case BIDIRECTIONAL:
                    arrow = "<->";
                    break;
                case DISCORD_TO_MINECRAFT:
                    arrow = "<-";
                    break;
                case MINECRAFT_TO_DISCORD:
                    arrow = "->";
                    break;
            }
            return "PairConfig{" + groupName + arrow + roleId + '}';
        }
    }

}
