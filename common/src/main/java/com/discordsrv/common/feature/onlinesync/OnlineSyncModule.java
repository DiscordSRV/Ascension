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

package com.discordsrv.common.feature.onlinesync;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.player.OfflinePlayer;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.OnlineSyncConfig;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.feature.onlinesync.enums.OnlineSyncCause;
import com.discordsrv.common.feature.onlinesync.enums.OnlineSyncResult;
import com.discordsrv.common.feature.profile.Profile;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.Game;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * The long is the user ID of the Discord user, the boolean is if they are online in Minecraft.
 */
public class OnlineSyncModule extends AbstractSyncModule<DiscordSRV, OnlineSyncConfig, Game, Long, Boolean> {

    public OnlineSyncModule(DiscordSRV discordSRV) {
        super(discordSRV, "ONLINE_SYNC");
    }

    @Override
    protected String syncName() {
        return "Online Sync";
    }

    @Override
    protected String logFileName() {
        return "onlinesync";
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
    protected List<OnlineSyncConfig> configs() {
        return Collections.singletonList(discordSRV.config().onlineSync);
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(Boolean one, Boolean two) {
        if (one == two) {
            return GenericSyncResults.both(one);
        }
        return null;
    }

    @Override
    protected Task<Boolean> getDiscord(OnlineSyncConfig config, long userId) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
        if (role == null) {
            return Task.failed(new SyncFail(OnlineSyncResult.ROLE_DOESNT_EXIST));
        }

        if (!role.getGuild().getSelfMember().canInteract(role)) {
            return Task.failed(new SyncFail(OnlineSyncResult.ROLE_CANNOT_INTERACT));
        }

        return role.getGuild().retrieveMemberById(userId)
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(OnlineSyncResult.NOT_A_GUILD_MEMBER);
                    }
                    throw t;
                })
                .thenApply(member -> member.hasRole(role) ? true : null);
    }

    @Override
    protected Task<Boolean> getGame(OnlineSyncConfig config, UUID playerUUID) {
        DiscordSRVPlayer player = discordSRV.playerProvider().player(playerUUID);
        
        if (player == null || player instanceof OfflinePlayer) {
            return Task.failed(new SyncFail(OnlineSyncResult.PLAYER_NOT_ONLINE));
        }

        return Task.completed(!discordSRV.isShutdown());
    }

    @Override
    protected Task<ISyncResult> applyDiscord(OnlineSyncConfig config, long userId, @Nullable Boolean newState) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            return Task.completed(GenericSyncResults.GUILD_NOT_FOUND);
        }

        DiscordRole role = guild.getRoleById(config.roleId);
        if (role == null) {
            return Task.completed(OnlineSyncResult.ROLE_DOESNT_EXIST);
        }

        if (!guild.getSelfMember().canInteract(role)) {
            return Task.completed(OnlineSyncResult.ROLE_CANNOT_INTERACT);
        }

        return guild.retrieveMemberById(userId)
                .then(member -> Boolean.TRUE.equals(newState) ? member.addRole(role).thenApply(v -> (ISyncResult) GenericSyncResults.ADD_DISCORD) : member.removeRole(role).thenApply(v -> GenericSyncResults.REMOVE_DISCORD))
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(OnlineSyncResult.NOT_A_GUILD_MEMBER);
                    }
                    if (t.getErrorCode() == ErrorResponse.MISSING_PERMISSIONS.getCode()) {
                        throw new SyncFail(GenericSyncResults.MEMBER_CANNOT_INTERACT);
                    }
                    throw t;
                });
    }

    @Override
    protected Task<ISyncResult> applyGame(OnlineSyncConfig config, UUID playerUUID, @Nullable Boolean newState) {
        return Task.completed(GenericSyncResults.WRONG_DIRECTION);
    }

    @Override
    public void onPlayerConnected(PlayerConnectedEvent event) {
        gameChanged(
                OnlineSyncCause.PLAYER_JOINED_SERVER,
                Someone.of(event.player().uniqueId()),
                Game.INSTANCE,
                true
        );
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        gameChanged(
                OnlineSyncCause.PLAYER_LEFT_SERVER,
                Someone.of(event.player().uniqueId()),
                Game.INSTANCE,
                false
        );
    }

    @Subscribe
    public void onShutdown(DiscordSRVShuttingDownEvent event) {
        serverChanged(OnlineSyncCause.SERVER_SHUTDOWN, true);
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        super.reload(resultConsumer);
        serverChanged(OnlineSyncCause.SERVER_STARTUP, false);
    }

    protected void serverChanged(OnlineSyncCause cause, boolean skipOnlineCheck) {
        if (!discordSRV.config().onlineSync.isSet()) {
            return;
        }

        DiscordGuild guild = discordSRV.discordAPI().getGuildById(discordSRV.config().onlineSync.serverId);
        if (guild == null) {
            return;
        }

        DiscordRole role = guild.getRoleById(discordSRV.config().onlineSync.roleId);
        if (role == null) {
            return;
        }

        List<Profile> onlineProfiles = new ArrayList<>();
        if (!skipOnlineCheck) {
            for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
                Profile profile = discordSRV.profileManager().getProfile(player.uniqueId());
                if (profile != null) {
                    onlineProfiles.add(profile);
                }
            }
        }

        // Remove the role from everyone who has it but isn't online
        guild.asJDA().getMembersWithRoles(role.asJDA()).stream().map(Member::getIdLong).filter(
                userId -> onlineProfiles.stream().noneMatch(profile -> Objects.equals(profile.userId(), userId))
        ).forEach(userId -> gameChanged(
                cause,
                Someone.of(userId),
                Game.INSTANCE,
                false
        ));
    }
}
