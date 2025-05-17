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

import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RewardsConfig {

    public List<LinkingReward> linkingRewards = new ArrayList<>(Collections.singleton(new LinkingReward()));
    public List<BoostingReward> boostingRewards = new ArrayList<>(Collections.singleton(new BoostingReward()));

    public static class Reward {

        @Comment("The reward id determines if the player and/or user has already been granted the reward.\n"
                + "It should be unique for each reward. Please do not use the same reward ids on multiple servers at once")
        public String rewardId = "firstlink";

        @Comment("Determines on when the reward can be granted.\n"
                + "Valid values are: once_per_player, once_per_user, once_per_both, always")
        public GrantType grantType = GrantType.ONCE_PER_BOTH;

        @Comment("Profile placeholders can be used")
        public List<String> consoleCommandsToRun = new ArrayList<>();
    }

    public static class LinkingReward extends Reward {

        @Comment("When the linking reward will trigger, valid values are:\n"
                + "- is_linked: the reward will be granted to any linked player at any time\n"
                + "- linked: the reward will be granted when the player and user become linked\n"
                + "- unlinked: the reward will be granted when the player and user become unlinked")
        public Type type = Type.IS_LINKED;

        public enum Type {
            IS_LINKED,
            LINKED,
            UNLINKED
        }
    }

    public static class BoostingReward extends Reward {

        @Comment("When the boosting reward will trigger, valid values are:\n"
                + "- is_boosting: the reward will be granted to any boosting player at any time\n"
                + "- boosted: the reward will be granted when a user starts boosting\n"
                + "- unboosted: the reward will be granted when a user stops boosting\n"
                + "This latter two options will not work if the user is not cached")
        public Type type = Type.IS_BOOSTING;
        public long serverId = 0L;

        public enum Type {
            IS_BOOSTING,
            BOOSTED,
            UNBOOSTED
        }
    }

    public enum GrantType {
        ONCE_PER_PLAYER,
        ONCE_PER_USER,
        ONCE_PER_BOTH,
        ALWAYS
    }
}
