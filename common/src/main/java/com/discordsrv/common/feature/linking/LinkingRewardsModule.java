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

package com.discordsrv.common.feature.linking;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.linking.AccountLinkedEvent;
import com.discordsrv.api.events.linking.AccountUnlinkedEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.RewardsConfig;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.core.profile.PlayerRewardData;
import com.discordsrv.common.core.profile.ProfileImpl;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkingRewardsModule extends AbstractModule<DiscordSRV> {

    public LinkingRewardsModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        discordSRV.profileManager().queryProfile(event.player().uniqueId())
                .whenSuccessful(profile -> {
                    Set<RewardsConfig.Reward> pendingRewards = new HashSet<>();
                    for (RewardsConfig.Reward reward : Stream.concat(discordSRV.config().rewards.linkingRewards.stream(), discordSRV.config().rewards.boostingRewards.stream()).collect(Collectors.toSet())) {
                        if (doesProfileAlreadyHave(profile, reward)) {
                            continue;
                        }

                        if (profile.getGameRewards() != null && profile.getGameRewards().stream().filter(PlayerRewardData::isPending).anyMatch(r -> r.getName().equals(reward.rewardId)) ||
                                profile.getDiscordRewards() != null && profile.getDiscordRewards().stream().filter(PlayerRewardData::isPending).anyMatch(r -> r.getName().equals(reward.rewardId))) {
                            pendingRewards.add(reward);
                    }

                    if (!pendingRewards.isEmpty()) {
                        triggerRewards(profile, new ArrayList<>(pendingRewards));
                    }

                    Long userId = profile.userId();
                    if (!profile.isLinked() || userId == null) {
                        return;
                    }
                    triggerRewards(profile, RewardsConfig.LinkingReward.Type.IS_LINKED);

                    Set<Long> uniqueGuildIds = new HashSet<>();
                    for (RewardsConfig.BoostingReward reward : discordSRV.config().rewards.boostingRewards) {
                        uniqueGuildIds.add(reward.serverId);
                    }
                    for (Long guildId : uniqueGuildIds) {
                        if (guildId == 0L) {
                            continue;
                        }

                        DiscordGuild guild = discordSRV.discordAPI().getGuildById(guildId);
                        if (guild == null) {
                            continue;
                        }

                        guild.retrieveMemberById(userId).whenSuccessful(member -> {
                            if (member.isBoosting()) {
                                triggerRewards(profile, RewardsConfig.BoostingReward.Type.IS_BOOSTING, guildId);
                            }
                        });
                    }
                });
    }

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        discordSRV.profileManager().queryProfile(event.getPlayerUUID())
                .whenSuccessful(profile -> triggerRewards(profile, RewardsConfig.LinkingReward.Type.LINKED));
    }

    @Subscribe
    public void onAccountUnlinked(AccountUnlinkedEvent event) {
        discordSRV.profileManager().queryProfile(event.getPlayerUUID())
                .whenSuccessful(profile -> triggerRewards(profile, RewardsConfig.LinkingReward.Type.UNLINKED));
    }

    @Subscribe
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent event) {
        boolean wasBoosting = event.getOldTimeBoosted() != null;
        boolean isBoosting = event.getNewTimeBoosted() != null;
        if (wasBoosting == isBoosting) {
            return;
        }

        discordSRV.profileManager().queryProfile(event.getMember().getIdLong())
                .whenSuccessful(profile -> triggerRewards(
                        profile,
                        isBoosting ? RewardsConfig.BoostingReward.Type.BOOSTED : RewardsConfig.BoostingReward.Type.UNBOOSTED,
                        event.getGuild().getIdLong()
                ));
    }

    private boolean doesProfileAlreadyHave(ProfileImpl profile, RewardsConfig.Reward reward) {
        Set<PlayerRewardData> gameRewards = profile.getGameRewards() != null ? profile.getGameRewards().stream().filter(r -> !r.isPending()).collect(Collectors.toSet()) : Collections.emptySet();
        Set<PlayerRewardData> discordRewards = profile.getDiscordRewards() != null ? profile.getDiscordRewards().stream().filter(r -> !r.isPending()).collect(Collectors.toSet()) : Collections.emptySet();
        switch (reward.grantType) {
            case ONCE_PER_BOTH:
                return gameRewards.stream().anyMatch(r -> r.getName().equals(reward.rewardId)) ||
                        discordRewards.stream().anyMatch(r -> r.getName().equals(reward.rewardId));
            case ONCE_PER_PLAYER:
                return gameRewards.stream().anyMatch(r -> r.getName().equals(reward.rewardId));
            case ONCE_PER_USER:
                return discordRewards.stream().anyMatch(r -> r.getName().equals(reward.rewardId));
            case ALWAYS:
            default:
                return false;
        }
    }

    /**
     * Adds a reward to the profile's granted or pending rewards by updating the reward's pending state.
     *
     * @param profile The profile to add the reward to.
     * @param game    Whether the reward is for the game or Discord. True for game, false for Discord.
     * @param reward  The reward to add.
     * @param pending Whether the reward is pending or already granted.
     */
    private void addRewardToProfile(ProfileImpl profile, boolean game, RewardsConfig.Reward reward, boolean pending) {
        Set<PlayerRewardData> rewards = game ? profile.getGameRewards() : profile.getDiscordRewards();

        if (rewards == null) {
            throw new IllegalStateException((game ? "Game" : "Discord") + " profile not available");
        }

        rewards.stream()
                .filter(r -> r.getName().equals(reward.rewardId))
                .findFirst()
                .ifPresentOrElse(
                        r -> r.setPending(pending),
                        () -> rewards.add(new PlayerRewardData(reward.rewardId, pending))
                );
    }

    private void triggerRewards(ProfileImpl profile, com.discordsrv.common.config.main.RewardsConfig.LinkingReward.Type type) {
        List<RewardsConfig.LinkingReward.Type> types = new ArrayList<>(2);
        types.add(type);
        if (type == com.discordsrv.common.config.main.RewardsConfig.LinkingReward.Type.LINKED) {
            types.add(RewardsConfig.LinkingReward.Type.IS_LINKED);
        }

        List<RewardsConfig.Reward> rewards = new ArrayList<>();
        for (RewardsConfig.LinkingReward reward : discordSRV.config().rewards.linkingRewards) {
            if (!types.contains(reward.type) || doesProfileAlreadyHave(profile, reward)) {
                continue;
            }

            rewards.add(reward);
        }
        triggerRewards(profile, rewards);
    }

    private void triggerRewards(ProfileImpl profile, com.discordsrv.common.config.main.RewardsConfig.BoostingReward.Type type, long guildId) {
        List<RewardsConfig.BoostingReward.Type> types = new ArrayList<>(2);
        types.add(type);
        if (type == com.discordsrv.common.config.main.RewardsConfig.BoostingReward.Type.BOOSTED) {
            types.add(RewardsConfig.BoostingReward.Type.IS_BOOSTING);
        }

        List<RewardsConfig.Reward> rewards = new ArrayList<>();
        for (RewardsConfig.BoostingReward reward : discordSRV.config().rewards.boostingRewards) {
            if (!types.contains(reward.type) || reward.serverId != guildId || doesProfileAlreadyHave(profile, reward)) {
                continue;
            }

            rewards.add(reward);
        }
        triggerRewards(profile, rewards);
    }

    private void triggerRewards(ProfileImpl profile, List< com.discordsrv.common.config.main.RewardsConfig.Reward> rewards) {
        if (rewards.isEmpty()) {
            return;
        }

        List<String> commands = new ArrayList<>();
        boolean gameRewards = false, discordRewards = false;
        for (RewardsConfig.Reward reward : rewards) {
            List<String> commandsToRun = reward.consoleCommandsToRun;
            if (commandsToRun.isEmpty()) {
                continue;
            }

            RewardsConfig.GrantType grantType = reward.grantType;
            boolean both = grantType == ONCE_PER_BOTH;
            boolean isPending = reward.needsOnline && !profile.isOnline();
            if (isPending) {
                addRewardToProfile(profile, true, reward, true);
                gameRewards = true;
                continue;
            } else {
                if (both || grantType == ONCE_PER_PLAYER) {
                    addRewardToProfile(profile, true, reward, false);
                    gameRewards = true;
                }
                if (both || grantType == ONCE_PER_USER) {
                    addRewardToProfile(profile, false, reward, false);
                    discordRewards = true;
                }
            }

            commands.addAll(commandsToRun);
        }

        if (gameRewards) {
            discordSRV.storage().saveGameProfileData(profile.getGameData());
        }
        if (discordRewards) {
            discordSRV.storage().saveDiscordProfileData(profile.getDiscordData());
        }

        for (String command : commands) {
            String finalCommand = discordSRV.placeholderService().replacePlaceholders(command, profile);
            discordSRV.console().runCommand(finalCommand);
        }
    }
}
