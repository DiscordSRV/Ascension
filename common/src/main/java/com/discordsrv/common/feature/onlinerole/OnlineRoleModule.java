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

import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.OnlineRoleConfig;
import com.discordsrv.common.events.player.PlayerChangedWorldEvent;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.feature.onlinerole.enums.OnlineRoleCause;
import com.discordsrv.common.feature.onlinerole.enums.OnlineRoleResult;
import com.discordsrv.common.feature.profile.Profile;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.Game;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Result;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The long is the user ID of the Discord user, the boolean is if they meet the condition in Minecraft.
 */
public class OnlineRoleModule extends AbstractSyncModule<DiscordSRV, OnlineRoleConfig, Game, Long, Boolean> {

    public OnlineRoleModule(DiscordSRV discordSRV) {
        super(discordSRV, "ONLINE_ROLE");
    }

    @Override
    protected String syncName() {
        return "Online Role";
    }

    @Override
    protected String logFileName() {
        return "onlinerole";
    }

    @Override
    protected String gameTerm() {
        return "online";
    }

    @Override
    protected String discordTerm() {
        return "role";
    }

    @Override
    protected List<OnlineRoleConfig> configs() {
        return discordSRV.config().onlineSync.getEntries();
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(Boolean one, Boolean two) {
        if (one == two) {
            return GenericSyncResults.both(one);
        }
        return null;
    }

    @Override
    protected Task<Boolean> getDiscord(OnlineRoleConfig config, long userId) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
        if (role == null) {
            return Task.failed(new SyncFail(OnlineRoleResult.ROLE_DOESNT_EXIST));
        }

        if (!role.getGuild().getSelfMember().canInteract(role)) {
            return Task.failed(new SyncFail(OnlineRoleResult.ROLE_CANNOT_INTERACT));
        }

        return role.getGuild().retrieveMemberById(userId)
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(OnlineRoleResult.NOT_A_GUILD_MEMBER);
                    }
                    throw t;
                })
                .thenApply(member -> member.hasRole(role) ? true : null);
    }

    @Override
    protected Task<Boolean> getGame(OnlineRoleConfig config, UUID playerUUID) {
        DiscordSRVPlayer player = discordSRV.playerProvider().player(playerUUID);
        
        if (player == null) {
            return Task.failed(new SyncFail(OnlineRoleResult.PLAYER_NOT_ONLINE));
        }

        return Task.completed(!discordSRV.isShutdown());
    }

    @Override
    protected Task<ISyncResult> applyDiscord(OnlineRoleConfig config, long userId, @Nullable Boolean newState) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
        if (role == null) {
            return Task.completed(OnlineRoleResult.ROLE_DOESNT_EXIST);
        }

        if (!role.getGuild().getSelfMember().canInteract(role)) {
            return Task.completed(OnlineRoleResult.ROLE_CANNOT_INTERACT);
        }

        return role.getGuild().retrieveMemberById(userId)
                .then(member -> Boolean.TRUE.equals(newState)
                        ? member.addRole(role).thenApply(v -> (ISyncResult) GenericSyncResults.ADD_DISCORD)
                        : member.removeRole(role).thenApply(v -> GenericSyncResults.REMOVE_DISCORD))
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(OnlineRoleResult.NOT_A_GUILD_MEMBER);
                    }
                    if (t.getErrorCode() == ErrorResponse.MISSING_PERMISSIONS.getCode()) {
                        throw new SyncFail(GenericSyncResults.MEMBER_CANNOT_INTERACT);
                    }
                    throw t;
                });
    }

    @Override
    protected Task<ISyncResult> applyGame(OnlineRoleConfig config, UUID playerUUID, @Nullable Boolean newState) {
        return Task.completed(GenericSyncResults.WRONG_DIRECTION);
    }

    @Override
    public void onPlayerConnected(PlayerConnectedEvent event) {
        resyncAll(OnlineRoleCause.PLAYER_JOINED_SERVER, Someone.of(event.player().uniqueId()));
    }

    @Subscribe
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        resyncAll(OnlineRoleCause.PLAYER_CHANGED_WORLD, Someone.of(event.player().uniqueId()));
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        resyncAll(OnlineRoleCause.PLAYER_LEFT_SERVER, Someone.of(event.player().uniqueId()));
    }

    @Subscribe
    public void onShutdown(DiscordSRVShuttingDownEvent event) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

        HashMap<Member, ArrayList<Role>> pairs = new HashMap<>();
        for (OnlineRoleConfig config : configs()) {
            DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
            if (role == null) {
                return;
            }

            role.getGuild().asJDA().getMembersWithRoles(role.asJDA()).forEach(member ->
                    pairs.computeIfAbsent(member, __ -> new ArrayList<>()).add(role.asJDA())
            );
        }

        CompletableFuture<Result<List<Void>>> future = RestAction.allOf(pairs.entrySet().stream().map(entry ->
                entry.getKey().getGuild().modifyMemberRoles(entry.getKey(), Collections.emptyList(), entry.getValue())
        ).collect(Collectors.toList())).mapToResult().submit();

        try {
            Result<List<Void>> result = future.get();
            if (result.isFailure()) throw Objects.requireNonNull(result.getFailure());
        } catch (Throwable e) {
            discordSRV.logger().error("Failed to remove online sync roles from all users for server shutdown", e);
        } finally {
            discordSRV.logger().info("Removed all online sync roles from all users for server shutdown");
        }
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

        for (OnlineRoleConfig config : configs()) {
            DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
            if (role == null) {
                return;
            }

            // Remove the role from everyone who has it but isn't online
            role.getGuild().asJDA().getMembersWithRoles(role.asJDA()).stream().map(Member::getIdLong).filter(
                    userId -> onlineProfiles.stream().noneMatch(profile -> Objects.equals(profile.userId(), userId))
            ).forEach(userId -> gameChanged(
                    OnlineRoleCause.SERVER_STARTUP,
                    Someone.of(userId),
                    Game.INSTANCE,
                    false
            ));
        }
    }
}
