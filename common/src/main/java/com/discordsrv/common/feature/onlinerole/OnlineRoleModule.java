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

package com.discordsrv.common.feature.onlinerole;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.vanish.PlayerVanishStatusChangeEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.profile.Profile;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.OnlineRoleConfig;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.feature.groupsync.DiscordPermissionResult;
import com.discordsrv.common.feature.onlinerole.enums.OnlineRoleCause;
import com.discordsrv.common.feature.onlinerole.enums.OnlineRoleResult;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.DiscordPermissionUtil;
import com.discordsrv.common.util.Game;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The long is the user ID of the Discord user, the boolean is if they meet the condition in Minecraft.
 */
public class OnlineRoleModule extends AbstractSyncModule<DiscordSRV, OnlineRoleConfig.SyncConfig, Game, Long, Boolean> {

    private final Map<UUID, Future<?>> roleAddFutures = new ConcurrentHashMap<>();

    public OnlineRoleModule(DiscordSRV discordSRV) {
        super(discordSRV, "ONLINE_ROLE");
    }

    @Override
    protected String syncName() {
        return "Online Role";
    }

    @Override
    protected String logFileName() {
        return null;
    }

    @Override
    protected String gameTerm() {
        return "online";
    }

    @Override
    protected String discordTerm() {
        return "role";
    }

    private OnlineRoleConfig config() {
        return discordSRV.config().onlineRole;
    }

    @Override
    protected List<OnlineRoleConfig.SyncConfig> configs() {
        return Collections.singletonList(config().syncConfig());
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(Boolean one, Boolean two) {
        if (one == two) {
            return GenericSyncResults.both(one);
        }
        return null;
    }

    @Override
    protected Task<Boolean> getDiscord(OnlineRoleConfig.SyncConfig config, Someone.Resolved someone) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
        if (role == null) {
            return Task.failed(new SyncFail(OnlineRoleResult.ROLE_DOESNT_EXIST));
        }

        DiscordGuild guild = role.getGuild();
        if (!guild.getSelfMember().canInteract(role)) {
            return Task.failed(new SyncFail(OnlineRoleResult.ROLE_CANNOT_INTERACT));
        }

        return someone.guildMember(guild)
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(OnlineRoleResult.NOT_A_GUILD_MEMBER);
                    }
                    throw t;
                })
                .thenApply(member -> member.hasRole(role) ? true : null);
    }

    @Override
    protected Task<Boolean> getGame(OnlineRoleConfig.SyncConfig config, Someone.Resolved someone) {
        DiscordSRVPlayer player = discordSRV.playerProvider().player(someone.playerUUID());
        return Task.completed(
                !discordSRV.isShutdown()
                        && player != null
                        && (config().giveRoleToVanishedPlayers || !player.isVanished())
        );
    }

    @Override
    protected Task<ISyncResult> applyDiscord(OnlineRoleConfig.SyncConfig config, Someone.Resolved someone, @Nullable Boolean newState) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
        if (role == null) {
            return Task.completed(OnlineRoleResult.ROLE_DOESNT_EXIST);
        }

        DiscordGuild guild = role.getGuild();
        DiscordGuildMember member = someone.guildMember(guild).join();
        if (member == null) {
            return Task.completed(OnlineRoleResult.NOT_A_GUILD_MEMBER);
        }

        DiscordGuildMember selfMember = guild.getSelfMember();
        if (!selfMember.canInteract(role)) {
            return Task.completed(OnlineRoleResult.ROLE_CANNOT_INTERACT);
        }

        EnumSet<Permission> missingPermissions = DiscordPermissionUtil.getMissingPermissions(guild.asJDA(), Collections.singleton(Permission.MANAGE_ROLES));
        if (!missingPermissions.isEmpty()) {
            return Task.completed(DiscordPermissionResult.of(guild.asJDA(), missingPermissions));
        }

        return (Boolean.TRUE.equals(newState) ? member.addRole(role) : member.removeRole(role))
                .thenApply(v -> Boolean.TRUE.equals(newState) ? (ISyncResult) GenericSyncResults.ADD_DISCORD : GenericSyncResults.REMOVE_DISCORD)
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(OnlineRoleResult.NOT_A_GUILD_MEMBER);
                    }
                    throw t;
                });
    }

    @Override
    protected Task<ISyncResult> applyGame(OnlineRoleConfig.SyncConfig config, Someone.Resolved someone, @Nullable Boolean newState) {
        return Task.completed(GenericSyncResults.WRONG_DIRECTION);
    }

    @Override
    public void onPlayerConnected(PlayerConnectedEvent event) {
        DiscordSRVPlayer player = event.player();
        long delayMs = config().delayAddingRoleByMs;
        if (delayMs <= 0) {
            addRole(player);
            return;
        }

        roleAddFutures.put(
                player.uniqueId(),
                discordSRV.scheduler().runLater(
                        () -> {
                            addRole(player);
                            roleAddFutures.remove(player.uniqueId());
                        },
                        Duration.ofMillis(delayMs)
                )
        );
    }

    private void addRole(DiscordSRVPlayer player) {
        if (!config().giveRoleToVanishedPlayers && player.isVanished()) {
            return;
        }

        gameChanged(OnlineRoleCause.PLAYER_JOINED_SERVER, Someone.of(discordSRV, player), Game.INSTANCE, true);
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        Future<?> addFuture = roleAddFutures.remove(event.player().uniqueId());
        if (addFuture != null && addFuture.cancel(false)) {
            // Don't try to remove the role if it was never added in the first place
            return;
        }

        gameChanged(OnlineRoleCause.PLAYER_LEFT_SERVER, Someone.of(discordSRV, event.player()), Game.INSTANCE, false);
    }

    @Subscribe
    public void onPlayerVanishStatusChange(PlayerVanishStatusChangeEvent event) {
        if (config().giveRoleToVanishedPlayers) {
            return;
        }
        gameChanged(OnlineRoleCause.PLAYER_VANISH_STATUS_CHANGED, Someone.of(discordSRV, event.getPlayer()), Game.INSTANCE, !event.isNewStatus());
    }

    private Task<List<Void>> removeRoleFromList(List<Member> members, Role role, long timeout, TimeUnit unit) {
        List<Task<Void>> futures = members.stream().map(member -> Task.of(member.getGuild().removeRoleFromMember(member, role).timeout(timeout, unit).submit())).collect(Collectors.toList());
        return Task.allOf(futures);
    }

    @Override
    public void serverShuttingDown() {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

        Role role = jda.getRoleById(config().roleId);
        if (role == null) {
            return;
        }

        List<Member> members = role.getGuild().getMembersWithRoles(role);
        removeRoleFromList(members, role, 10, TimeUnit.SECONDS)
                .whenFailed(e -> discordSRV.logger().error("Failed to remove online sync roles from all users for server shutdown", e))
                .whenSuccessful(__ -> discordSRV.logger().info("Removed all online sync roles from all users for server shutdown"));
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        super.reload(resultConsumer);
        List<Profile> onlineProfiles = new ArrayList<>();
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            Profile profile = discordSRV.profileManager().getProfile(player.uniqueId());
            if (profile != null) {
                onlineProfiles.add(profile);
            }
        }

        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

        Role role = jda.getRoleById(config().roleId);
        if (role == null) {
            return;
        }

        List<Member> membersToRemove = role.getGuild().getMembersWithRoles(role).stream().filter(
                member -> onlineProfiles.stream().noneMatch(profile -> Objects.equals(profile.userId(), member.getIdLong()))
        ).collect(Collectors.toList());

        removeRoleFromList(membersToRemove, role, 1, TimeUnit.MINUTES)
                .whenFailed(e -> discordSRV.logger().error("Failed to remove online sync roles from all users for reload", e))
                .whenSuccessful(__ -> discordSRV.logger().debug("Removed all online sync roles from all users for reload"));
    }
}
