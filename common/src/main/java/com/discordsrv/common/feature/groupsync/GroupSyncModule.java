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

package com.discordsrv.common.feature.groupsync;

import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.member.role.DiscordMemberRoleAddEvent;
import com.discordsrv.api.events.discord.member.role.DiscordMemberRoleRemoveEvent;
import com.discordsrv.api.module.type.PermissionModule;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.enums.SyncSide;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.GroupSyncConfig;
import com.discordsrv.common.core.debug.DebugGenerateEvent;
import com.discordsrv.common.core.debug.file.TextDebugFile;
import com.discordsrv.common.feature.groupsync.enums.GroupSyncCause;
import com.discordsrv.common.feature.groupsync.enums.GroupSyncResult;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.DiscordPermissionUtil;
import com.github.benmanes.caffeine.cache.Cache;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The Game id is the group name, the Discord id is the role id and the state indicates if the player or user has the group or role.
 */
public class GroupSyncModule extends AbstractSyncModule<DiscordSRV, GroupSyncConfig.Entry, String, Long, Boolean> {

    private final Cache<Long, Map<Long, Boolean>> expectedDiscordChanges;
    private final Cache<UUID, Map<String, Boolean>> expectedMinecraftChanges;

    public GroupSyncModule(DiscordSRV discordSRV) {
        super(discordSRV, "GROUP_SYNC");

        this.expectedDiscordChanges = discordSRV.caffeineBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
        this.expectedMinecraftChanges = discordSRV.caffeineBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String syncName() {
        return "Group sync";
    }

    @Override
    public String logFileName() {
        return "groupsync";
    }

    @Override
    public String gameTerm() {
        return "group";
    }

    @Override
    public String discordTerm() {
        return "role";
    }

    @Override
    public List<GroupSyncConfig.Entry> configs() {
        return discordSRV.config().groupSync.getEntries();
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(Boolean one, Boolean two) {
        if (one == two) {
            return GenericSyncResults.both(one);
        }
        return null;
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        StringBuilder builder = new StringBuilder("Active pairs:");

        for (GroupSyncConfig.Entry sync : syncs) {
            builder.append("\n- ").append(sync)
                    .append(" (tie-breakers: ").append(sync.tieBreakers)
                    .append(", timer: ").append(sync.timer)
                    .append(", direction: ").append(sync.direction)
                    .append(", context: ").append(sync.contexts).append(")");
        }

        PermissionModule.Groups groups = getPermissionProvider();
        if (groups != null) {
            builder.append("\n\nAvailable groups (").append(groups.getClass().getName()).append("):");

            for (String group : groups.getGroups()) {
                builder.append("\n- ").append(group);
            }
        } else {
            builder.append("\n\nNo permission provider available");
        }
        event.addFile("group-sync.txt", new TextDebugFile(builder));
    }

    // Listeners & methods to indicate something changed

    @Subscribe
    public void onDiscordMemberRoleAdd(DiscordMemberRoleAddEvent event) {
        event.getRoles().forEach(role -> roleChanged(event.getMember().getUser().getId(), role.getId(), true));
    }

    @Subscribe
    public void onDiscordMemberRoleRemove(DiscordMemberRoleRemoveEvent event) {
        event.getRoles().forEach(role -> roleChanged(event.getMember().getUser().getId(), role.getId(), false));
    }

    public void groupAdded(UUID player, String groupName, @Nullable Map<String, Set<String>> contexts, GroupSyncCause cause) {
        groupChanged(player, groupName, contexts, cause, true);
    }

    public void groupRemoved(UUID player, String groupName, @Nullable Map<String, Set<String>> contexts, GroupSyncCause cause) {
        groupChanged(player, groupName, contexts, cause, false);
    }

    public void groupsMaybeChanged(UUID player, Set<String> groupNames, GroupSyncCause cause) {
        Set<GroupSyncConfig.Entry> entries = new LinkedHashSet<>();
        for (GroupSyncConfig.Entry config : configs()) {
            if (!config.includeInherited()) {
                continue;
            }

            if (groupNames.stream().anyMatch(config.groupName::equals)) {
                entries.add(config);
            }
        }
        resync(cause, Someone.of(discordSRV, player), __ -> SyncSide.MINECRAFT, entries);
    }

    private void roleChanged(long userId, long roleId, boolean newState) {
        if (checkExpectation(expectedDiscordChanges, userId, roleId, newState)) {
            return;
        }

        PermissionModule.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider == null) {
            logger().debug("No permission provider");
            return;
        }

        discordChanged(GroupSyncCause.DISCORD_ROLE_CHANGE, Someone.of(discordSRV, userId), roleId, newState);
    }

    private void groupChanged(
            UUID playerUUID,
            String groupName,
            Map<String, Set<String>> contexts,
            GroupSyncCause cause,
            boolean state
    ) {
        if (cause.isDiscordSRVCanCause() && checkExpectation(expectedMinecraftChanges, playerUUID, groupName, state)) {
            return;
        }

        PermissionModule.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider == null) {
            logger().debug("No permission provider");
            return;
        }

        gameChanged(cause, Someone.of(discordSRV, playerUUID), GroupSyncConfig.Entry.makeGameId(groupName, contexts), state);
    }

    @Override
    protected boolean isApplicableForProactiveSync(GroupSyncConfig.Entry config) {
        return !config.includeInherited();
    }

    private PermissionModule.Groups getPermissionProvider() {
        PermissionModule.GroupsContext groupsContext = discordSRV.getModule(PermissionModule.GroupsContext.class);
        return groupsContext != null ? groupsContext : discordSRV.getModule(PermissionModule.Groups.class);
    }

    public boolean noPermissionProvider() {
        return getPermissionProvider() == null;
    }

    private <T, R> boolean checkExpectation(Cache<T, Map<R, Boolean>> expectations, T key, R mapKey, boolean newState) {
        // Check if we were expecting the change (when we add/remove something due to synchronization),
        // if we did expect the change, we won't trigger a synchronization since we just synchronized what was needed
        Map<R, Boolean> expected = expectations.getIfPresent(key);
        if (expected != null && Objects.equals(expected.get(mapKey), newState)) {
            expected.remove(mapKey);
            return true;
        }
        return false;
    }

    // Resync

    @Override
    public Task<Boolean> getDiscord(GroupSyncConfig.Entry config, Someone.Resolved someone) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
        if (role == null) {
            return Task.failed(new SyncFail(GroupSyncResult.ROLE_DOESNT_EXIST));
        }

        if (!role.getGuild().getSelfMember().canInteract(role)) {
            return Task.failed(new SyncFail(GroupSyncResult.ROLE_CANNOT_INTERACT));
        }

        return someone.guildMember(role.getGuild())
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        throw new SyncFail(GroupSyncResult.NOT_A_GUILD_MEMBER);
                    }
                    throw t;
                })
                .thenApply(member -> {
                    logger().trace(someone + " roles: " + member.getRoles());
                    return member.hasRole(role);
                });
    }

    @Override
    public Task<Boolean> getGame(GroupSyncConfig.Entry config, Someone.Resolved someone) {
        PermissionModule.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider == null) {
            return Task.failed(new SyncFail(GenericSyncResults.MODULE_NOT_FOUND));
        }

        UUID playerUUID = someone.playerUUID();
        Task<Boolean> future;
        if (permissionProvider instanceof PermissionModule.GroupsContext) {
            future = ((PermissionModule.GroupsContext) permissionProvider)
                    .hasGroup(playerUUID, config.groupName, config.includeInherited(), config.contexts());
        } else {
            future = permissionProvider.hasGroup(playerUUID, config.groupName, config.includeInherited());
        }

        return future.mapException(t -> {
            throw new SyncFail(GroupSyncResult.PERMISSION_BACKEND_FAILED, t);
        });
    }

    @Override
    public Task<ISyncResult> applyDiscord(GroupSyncConfig.Entry config, Someone.Resolved someone, Boolean newState) {
        boolean stateToApply = newState != null && newState;

        DiscordRole role = discordSRV.discordAPI().getRoleById(config.roleId);
        if (role == null) {
            return Task.failed(new SyncFail(GroupSyncResult.ROLE_DOESNT_EXIST));
        }

        Guild jdaGuild = role.getGuild().asJDA();
        EnumSet<Permission> missingPermissions = DiscordPermissionUtil.getMissingPermissions(jdaGuild, Collections.singleton(Permission.MANAGE_ROLES));
        if (!missingPermissions.isEmpty()) {
            return Task.completed(DiscordPermissionResult.of(jdaGuild, missingPermissions));
        }

        Map<Long, Boolean> expected = Objects.requireNonNull(expectedDiscordChanges.get(someone.userId(), key -> new ConcurrentHashMap<>()));
        expected.put(config.roleId, stateToApply);

        return someone.guildMember(role.getGuild())
                .then(member -> stateToApply
                                ? member.addRole(role).thenApply(v -> (ISyncResult) GenericSyncResults.ADD_DISCORD)
                                : member.removeRole(role).thenApply(v -> GenericSyncResults.REMOVE_DISCORD)
                ).whenFailed((t) -> expected.remove(config.roleId));
    }

    @Override
    public Task<ISyncResult> applyGame(GroupSyncConfig.Entry config, Someone.Resolved someone, Boolean newState) {
        boolean stateToApply = newState != null && newState;
        UUID playerUUID = someone.playerUUID();

        Map<String, Boolean> expected = expectedMinecraftChanges.get(playerUUID, key -> new ConcurrentHashMap<>());
        if (expected != null) {
            expected.put(config.groupName, stateToApply);
        }

        Task<ISyncResult> future =
                stateToApply
                    ? addGroup(playerUUID, config).thenApply(v -> GenericSyncResults.ADD_GAME)
                    : removeGroup(playerUUID, config).thenApply(v -> GenericSyncResults.REMOVE_GAME);
        return future.mapException(t -> {
            //noinspection DataFlowIssue
            expected.remove(config.groupName);
            throw new SyncFail(GroupSyncResult.PERMISSION_BACKEND_FAILED, t);
        });
    }

    private Task<Void> addGroup(UUID player, GroupSyncConfig.Entry config) {
        PermissionModule.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider == null) {
            return Task.failed(new SyncFail(GenericSyncResults.MODULE_NOT_FOUND));
        }

        String groupName = config.groupName;
        if (permissionProvider instanceof PermissionModule.GroupsContext) {
            return ((PermissionModule.GroupsContext) permissionProvider)
                    .addGroup(player, groupName, config.contexts());
        } else {
            return permissionProvider.addGroup(player, groupName);
        }
    }

    private Task<Void> removeGroup(UUID player, GroupSyncConfig.Entry config) {
        PermissionModule.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider == null) {
            return Task.failed(new SyncFail(GenericSyncResults.MODULE_NOT_FOUND));
        }

        String groupName = config.groupName;
        if (permissionProvider instanceof PermissionModule.GroupsContext) {
            return ((PermissionModule.GroupsContext) permissionProvider)
                    .removeGroup(player, groupName, config.contexts());
        } else {
            return permissionProvider.removeGroup(player, groupName);
        }
    }
}
