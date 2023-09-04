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

package com.discordsrv.common.groupsync;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.events.member.role.DiscordMemberRoleAddEvent;
import com.discordsrv.api.discord.events.member.role.DiscordMemberRoleRemoveEvent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.module.type.PermissionDataProvider;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.GroupSyncConfig;
import com.discordsrv.common.debug.DebugGenerateEvent;
import com.discordsrv.common.debug.file.TextDebugFile;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.groupsync.enums.GroupSyncCause;
import com.discordsrv.common.groupsync.enums.GroupSyncDirection;
import com.discordsrv.common.groupsync.enums.GroupSyncResult;
import com.discordsrv.common.groupsync.enums.GroupSyncSide;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.event.PlayerConnectedEvent;
import com.discordsrv.common.profile.Profile;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GroupSyncModule extends AbstractModule<DiscordSRV> {

    private final Map<GroupSyncConfig.PairConfig, Future<?>> pairs = new LinkedHashMap<>();
    private final Map<String, List<GroupSyncConfig.PairConfig>> groupsToPairs = new ConcurrentHashMap<>();
    private final Map<Long, List<GroupSyncConfig.PairConfig>> rolesToPairs = new ConcurrentHashMap<>();

    private final Cache<Long, Map<Long, Boolean>> expectedDiscordChanges;
    private final Cache<UUID, Map<String, Boolean>> expectedMinecraftChanges;

    public GroupSyncModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "GROUP_SYNC"));
        this.expectedDiscordChanges = discordSRV.caffeineBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();

        this.expectedMinecraftChanges = discordSRV.caffeineBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public boolean isEnabled() {
        boolean any = false;
        for (GroupSyncConfig.PairConfig pair : discordSRV.config().groupSync.pairs) {
            if (pair.roleId != 0 && StringUtils.isNotEmpty(pair.groupName)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return false;
        }

        return super.isEnabled();
    }

    @Override
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        synchronized (pairs) {
            pairs.values().forEach(future -> {
                if (future != null) {
                    future.cancel(false);
                }
            });
            pairs.clear();
            groupsToPairs.clear();
            rolesToPairs.clear();

            GroupSyncConfig config = discordSRV.config().groupSync;
            for (GroupSyncConfig.PairConfig pair : config.pairs) {
                String groupName = pair.groupName;
                long roleId = pair.roleId;
                if (StringUtils.isEmpty(groupName) || roleId == 0) {
                    continue;
                }

                if (!pair.validate(discordSRV)) {
                    continue;
                }

                boolean failed = false;
                for (GroupSyncConfig.PairConfig pairConfig : config.pairs) {
                    if (pairConfig != pair && pair.isTheSameAs(pairConfig)) {
                        failed = true;
                        break;
                    }
                }
                if (failed) {
                    discordSRV.logger().error("Duplicate group synchronization pair: " + groupName + " to " + roleId);
                    continue;
                }

                Future<?> future = null;
                GroupSyncConfig.PairConfig.TimerConfig timer = pair.timer;
                if (timer != null && timer.enabled) {
                    int cycleTime = timer.cycleTime;
                    future = discordSRV.scheduler().runAtFixedRate(
                            () -> resyncPair(pair, GroupSyncCause.TIMER),
                            cycleTime,
                            cycleTime,
                            TimeUnit.MINUTES
                    );
                }

                pairs.put(pair, future);
                groupsToPairs.computeIfAbsent(groupName, key -> new ArrayList<>()).add(pair);
                rolesToPairs.computeIfAbsent(roleId, key -> new ArrayList<>()).add(pair);
            }
        }
    }

    // Debug

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        StringBuilder builder = new StringBuilder("Active pairs:");

        for (Map.Entry<GroupSyncConfig.PairConfig, Future<?>> entry : pairs.entrySet()) {
            GroupSyncConfig.PairConfig pair = entry.getKey();
            builder.append("\n- ").append(pair)
                    .append(" (tie-breaker: ").append(pair.tieBreaker())
                    .append(", direction: ").append(pair.direction())
                    .append(", server context: ").append(pair.serverContext).append(")");
            if (entry.getValue() != null) {
                builder.append(" [Timed]");
            }
        }

        PermissionDataProvider.Groups groups = getPermissionProvider();
        if (groups != null) {
            builder.append("\n\nAvailable groups (").append(groups.getClass().getName()).append("):");

            for (String group : groups.getGroups()) {
                builder.append("\n- ").append(group);
            }
        } else {
            builder.append("\n\nNo permission provider available");
        }
        event.addFile(new TextDebugFile("group-sync.txt", builder));
    }

    private void logSummary(
            UUID player,
            GroupSyncCause cause,
            Map<GroupSyncConfig.PairConfig, CompletableFuture<GroupSyncResult>> pairs
    ) {
        CompletableFutureUtil.combine(pairs.values()).whenComplete((v, t) -> {
            SynchronizationSummary summary = new SynchronizationSummary(player, cause);
            for (Map.Entry<GroupSyncConfig.PairConfig, CompletableFuture<GroupSyncResult>> entry : pairs.entrySet()) {
                summary.add(entry.getKey(), entry.getValue().join());
            }
            logger().debug(summary.toString());
        });
    }

    // Linked account helper methods

    private CompletableFuture<Long> lookupLinkedAccount(UUID player) {
        return discordSRV.profileManager().lookupProfile(player)
                .thenApply(Profile::userId);
    }

    private CompletableFuture<UUID> lookupLinkedAccount(long userId) {
        return discordSRV.profileManager().lookupProfile(userId)
                .thenApply(Profile::playerUUID);
    }

    // Permission data helper methods

    private PermissionDataProvider.Groups getPermissionProvider() {
        PermissionDataProvider.GroupsContext groupsContext = discordSRV.getModule(PermissionDataProvider.GroupsContext.class);
        return groupsContext == null ? discordSRV.getModule(PermissionDataProvider.Groups.class) : groupsContext;
    }

    public boolean noPermissionProvider() {
        PermissionDataProvider.Groups groups = getPermissionProvider();
        return groups == null || !groups.isEnabled();
    }

    private boolean supportsOffline() {
        return getPermissionProvider().supportsOffline();
    }

    private CompletableFuture<Boolean> hasGroup(
            UUID player,
            String groupName,
            @Nullable String serverContext
    ) {
        PermissionDataProvider.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider instanceof PermissionDataProvider.GroupsContext) {
            return ((PermissionDataProvider.GroupsContext) permissionProvider)
                    .hasGroup(player, groupName, false, serverContext != null ? Collections.singleton(serverContext) : null);
        } else {
            return permissionProvider.hasGroup(player, groupName, false);
        }
    }

    private CompletableFuture<Void> addGroup(
            UUID player,
            String groupName,
            @Nullable String serverContext
    ) {
        PermissionDataProvider.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider instanceof PermissionDataProvider.GroupsContext) {
            return ((PermissionDataProvider.GroupsContext) permissionProvider)
                    .addGroup(player, groupName, Collections.singleton(serverContext));
        } else {
            return permissionProvider.addGroup(player, groupName);
        }
    }

    private CompletableFuture<Void> removeGroup(
            UUID player,
            String groupName,
            @Nullable String serverContext
    ) {
        PermissionDataProvider.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider instanceof PermissionDataProvider.GroupsContext) {
            return ((PermissionDataProvider.GroupsContext) permissionProvider)
                    .removeGroup(player, groupName, Collections.singleton(serverContext));
        } else {
            return permissionProvider.removeGroup(player, groupName);
        }
    }

    // Resync user

    public CompletableFuture<List<GroupSyncResult>> resync(UUID player, GroupSyncCause cause) {
        return lookupLinkedAccount(player).thenCompose(userId -> {
            if (userId == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            return CompletableFutureUtil.combine(resync(player, userId, cause));
        });
    }

    public CompletableFuture<List<GroupSyncResult>> resync(long userId, GroupSyncCause cause) {
        return lookupLinkedAccount(userId).thenCompose(player -> {
            if (player == null) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            return CompletableFutureUtil.combine(resync(player, userId, cause));
        });
    }

    public Collection<CompletableFuture<GroupSyncResult>> resync(UUID player, long userId, GroupSyncCause cause) {
        if (noPermissionProvider()) {
            return Collections.singletonList(CompletableFuture.completedFuture(GroupSyncResult.NO_PERMISSION_PROVIDER));
        } else if (discordSRV.playerProvider().player(player) == null && !supportsOffline()) {
            return Collections.singletonList(CompletableFuture.completedFuture(GroupSyncResult.PERMISSION_PROVIDER_NO_OFFLINE_SUPPORT));
        }

        Map<GroupSyncConfig.PairConfig, CompletableFuture<GroupSyncResult>> futures = new LinkedHashMap<>();
        for (GroupSyncConfig.PairConfig pair : pairs.keySet()) {
            futures.put(pair, resyncPair(pair, player, userId));
        }

        logSummary(player, cause, futures);
        return futures.values();
    }

    private void resyncPair(GroupSyncConfig.PairConfig pair, GroupSyncCause cause) {
        if (noPermissionProvider()) {
            return;
        }

        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            UUID uuid = player.uniqueId();
            lookupLinkedAccount(uuid).whenComplete((userId, t) -> {
                if (userId == null) {
                    return;
                }

                resyncPair(pair, uuid, userId).whenComplete((result, t2) -> logger().debug(
                        new SynchronizationSummary(uuid, cause, pair, result).toString()
                ));
            });
        }
    }

    private CompletableFuture<GroupSyncResult> resyncPair(GroupSyncConfig.PairConfig pair, UUID player, long userId) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(pair.roleId);
        if (role == null) {
            return CompletableFuture.completedFuture(GroupSyncResult.ROLE_DOESNT_EXIST);
        }

        if (!role.getGuild().getSelfMember().canInteract(role)) {
            return CompletableFuture.completedFuture(GroupSyncResult.ROLE_CANNOT_INTERACT);
        }

        return role.getGuild().retrieveMemberById(userId).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(GroupSyncResult.NOT_A_GUILD_MEMBER);
            }

            boolean hasRole = member.hasRole(role);
            String groupName = pair.groupName;
            CompletableFuture<GroupSyncResult> resultFuture = new CompletableFuture<>();

            hasGroup(player, groupName, pair.serverContext).whenComplete((hasGroup, t) -> {
                if (t != null) {
                    discordSRV.logger().error("Failed to check if player " + player + " has group " + groupName, t);
                    resultFuture.complete(GroupSyncResult.PERMISSION_BACKEND_FAIL_CHECK);
                    return;
                }

                if (hasRole == hasGroup) {
                    resultFuture.complete(hasRole ? GroupSyncResult.BOTH_TRUE : GroupSyncResult.BOTH_FALSE);
                    // We're all good
                    return;
                }

                GroupSyncSide side = pair.tieBreaker();
                GroupSyncDirection direction = pair.direction();
                CompletableFuture<Void> future;
                GroupSyncResult result;
                if (hasRole) {
                    if (side == GroupSyncSide.DISCORD) {
                        // Has role, add group
                        if (direction == GroupSyncDirection.MINECRAFT_TO_DISCORD) {
                            resultFuture.complete(GroupSyncResult.WRONG_DIRECTION);
                            return;
                        }

                        result = GroupSyncResult.ADD_GROUP;
                        future = addGroup(player, groupName, pair.serverContext);
                    } else {
                        // Doesn't have group, remove role
                        if (direction == GroupSyncDirection.DISCORD_TO_MINECRAFT) {
                            resultFuture.complete(GroupSyncResult.WRONG_DIRECTION);
                            return;
                        }

                        result = GroupSyncResult.REMOVE_ROLE;
                        future = member.removeRole(role);
                    }
                } else {
                    if (side == GroupSyncSide.DISCORD) {
                        // Doesn't have role, remove group
                        if (direction == GroupSyncDirection.MINECRAFT_TO_DISCORD) {
                            resultFuture.complete(GroupSyncResult.WRONG_DIRECTION);
                            return;
                        }

                        result = GroupSyncResult.REMOVE_GROUP;
                        future = removeGroup(player, groupName, pair.serverContext);
                    } else {
                        // Has group, add role
                        if (direction == GroupSyncDirection.DISCORD_TO_MINECRAFT) {
                            resultFuture.complete(GroupSyncResult.WRONG_DIRECTION);
                            return;
                        }

                        result = GroupSyncResult.ADD_ROLE;
                        future = member.addRole(role);
                    }
                }
                future.whenComplete((v, t2) -> {
                    if (t2 != null) {
                        discordSRV.logger().error("Failed to " + result + " to " + player + "/" + Long.toUnsignedString(userId), t2);
                        resultFuture.complete(GroupSyncResult.UPDATE_FAILED);
                        return;
                    }

                    resultFuture.complete(result);
                });
            });

            return resultFuture;
        });
    }

    // Listeners & methods to indicate something changed

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        resync(event.player().uniqueId(), GroupSyncCause.GAME_JOIN);
    }

    @Subscribe
    public void onDiscordMemberRoleAdd(DiscordMemberRoleAddEvent event) {
        event.getRoles().forEach(role -> roleChanged(event.getMember().getUser().getId(), role.getId(), false));
    }

    @Subscribe
    public void onDiscordMemberRoleRemove(DiscordMemberRoleRemoveEvent event) {
        event.getRoles().forEach(role -> roleChanged(event.getMember().getUser().getId(), role.getId(), true));
    }

    public void groupAdded(UUID player, String groupName, @Nullable Set<String> serverContext, GroupSyncCause cause) {
        groupChanged(player, groupName, serverContext, cause, false);
    }

    public void groupRemoved(UUID player, String groupName, @Nullable Set<String> serverContext, GroupSyncCause cause) {
        groupChanged(player, groupName, serverContext, cause, true);
    }

    // Internal handling of changes

    private <T, R> boolean checkExpectation(Cache<T, Map<R, Boolean>> expectations, T key, R mapKey, boolean remove) {
        // Check if we were expecting the change (when we add/remove something due to synchronization),
        // if we did expect the change, we won't trigger a synchronization since we just synchronized what was needed
        Map<R, Boolean> expected = expectations.getIfPresent(key);
        if (expected != null && Objects.equals(expected.get(mapKey), remove)) {
            expected.remove(mapKey);
            return true;
        }
        return false;
    }

    private void roleChanged(long userId, long roleId, boolean remove) {
        if (noPermissionProvider()) {
            return;
        }

        if (checkExpectation(expectedDiscordChanges, userId, roleId, remove)) {
            return;
        }

        lookupLinkedAccount(userId).whenComplete((player, t) -> {
            if (player == null) {
                return;
            }

            roleChanged(userId, player, roleId, remove);
        });
    }

    private void roleChanged(long userId, UUID player, long roleId, boolean remove) {
        List<GroupSyncConfig.PairConfig> pairs = rolesToPairs.get(roleId);
        if (pairs == null) {
            return;
        }

        PermissionDataProvider.Groups permissionProvider = getPermissionProvider();
        if (permissionProvider == null) {
            discordSRV.logger().warning("No supported permission plugin available to perform group sync");
            return;
        }

        Map<GroupSyncConfig.PairConfig, CompletableFuture<GroupSyncResult>> futures = new LinkedHashMap<>();
        for (GroupSyncConfig.PairConfig pair : pairs) {
            GroupSyncDirection direction = pair.direction();
            if (direction == GroupSyncDirection.MINECRAFT_TO_DISCORD) {
                // Not going Discord -> Minecraft
                futures.put(pair, CompletableFuture.completedFuture(GroupSyncResult.WRONG_DIRECTION));
                continue;
            }

            futures.put(pair, modifyGroupState(player, pair, remove));

            // If the sync is bidirectional, also add/remove any other roles that are linked to this group
            if (direction == GroupSyncDirection.DISCORD_TO_MINECRAFT) {
                continue;
            }

            List<GroupSyncConfig.PairConfig> groupPairs = groupsToPairs.get(pair.groupName);
            if (groupPairs == null) {
                continue;
            }

            for (GroupSyncConfig.PairConfig groupPair : groupPairs) {
                if (groupPair.roleId == roleId) {
                    continue;
                }

                futures.put(groupPair, modifyRoleState(userId, groupPair, remove));
            }
        }
        logSummary(player, GroupSyncCause.DISCORD_ROLE_CHANGE, futures);
    }

    private void groupChanged(
            UUID player,
            String groupName,
            @Nullable Set<String> serverContext,
            GroupSyncCause cause,
            boolean remove
    ) {
        if (noPermissionProvider()) {
            return;
        }

        if (cause.isDiscordSRVCanCause() && checkExpectation(expectedMinecraftChanges, player, groupName, remove)) {
            return;
        }

        lookupLinkedAccount(player).whenComplete((userId, t) -> {
            if (userId == null) {
                return;
            }

            groupChanged(player, userId, groupName, serverContext, cause, remove);
        });
    }

    private void groupChanged(
            UUID player,
            long userId,
            String groupName,
            @Nullable Set<String> serverContext,
            GroupSyncCause cause,
            boolean remove
    ) {
        List<GroupSyncConfig.PairConfig> pairs = groupsToPairs.get(groupName);
        if (pairs == null) {
            return;
        }

        PermissionDataProvider.Groups permissionProvider = getPermissionProvider();
        Map<GroupSyncConfig.PairConfig, CompletableFuture<GroupSyncResult>> futures = new LinkedHashMap<>();
        for (GroupSyncConfig.PairConfig pair : pairs) {
            GroupSyncDirection direction = pair.direction();
            if (direction == GroupSyncDirection.DISCORD_TO_MINECRAFT) {
                // Not going Minecraft -> Discord
                futures.put(pair, CompletableFuture.completedFuture(GroupSyncResult.WRONG_DIRECTION));
                continue;
            }

            // Check if we're in the right context
            String context = pair.serverContext;
            if (permissionProvider instanceof PermissionDataProvider.GroupsContext) {
                if (StringUtils.isEmpty(context)) {
                    // Use the default server context of the server
                    Set<String> defaultValues = ((PermissionDataProvider.GroupsContext) permissionProvider)
                            .getDefaultServerContext();
                    if (!Objects.equals(serverContext, defaultValues)) {
                        continue;
                    }
                } else if (context.equals("global")) {
                    // No server context
                    if (serverContext != null && !serverContext.isEmpty()) {
                        continue;
                    }
                } else {
                    // Server context has to match the specified
                    if (serverContext == null
                            || serverContext.size() != 1
                            || !serverContext.iterator().next().equals(context)) {
                        continue;
                    }
                }
            }

            futures.put(pair, modifyRoleState(userId, pair, remove));

            // If the sync is bidirectional, also add/remove any other groups that are linked to this role
            if (direction == GroupSyncDirection.MINECRAFT_TO_DISCORD) {
                continue;
            }

            long roleId = pair.roleId;
            List<GroupSyncConfig.PairConfig> rolePairs = rolesToPairs.get(roleId);
            if (rolePairs == null || rolePairs.isEmpty()) {
                continue;
            }

            for (GroupSyncConfig.PairConfig rolePair : rolePairs) {
                if (rolePair.groupName.equals(groupName)) {
                    continue;
                }

                futures.put(rolePair, modifyGroupState(player, rolePair, remove));
            }
        }
        logSummary(player, cause, futures);
    }

    private CompletableFuture<GroupSyncResult> modifyGroupState(UUID player, GroupSyncConfig.PairConfig config, boolean remove) {
        String groupName = config.groupName;

        Map<String, Boolean> expected = expectedMinecraftChanges.get(player, key -> new ConcurrentHashMap<>());
        if (expected != null) {
            expected.put(groupName, remove);
        }

        CompletableFuture<GroupSyncResult> future = new CompletableFuture<>();
        String serverContext = config.serverContext;
        hasGroup(player, groupName, serverContext).thenCompose(hasGroup -> {
            if (remove && hasGroup) {
                return removeGroup(player, groupName, serverContext).thenApply(v -> GroupSyncResult.REMOVE_GROUP);
            } else if (!remove && !hasGroup) {
                return addGroup(player, groupName, serverContext).thenApply(v -> GroupSyncResult.ADD_GROUP);
            } else {
                // Nothing to do
                return CompletableFuture.completedFuture(GroupSyncResult.ALREADY_IN_SYNC);
            }
        }).whenComplete((result, t) -> {
            if (t != null) {
                if (expected != null) {
                    // Failed, remove expectation
                    expected.remove(groupName);
                }

                future.complete(GroupSyncResult.UPDATE_FAILED);
                discordSRV.logger().error("Failed to add group " + groupName + " to " + player, t);
                return;
            }

            future.complete(result);
        });
        return future;
    }

    private CompletableFuture<GroupSyncResult> modifyRoleState(long userId, GroupSyncConfig.PairConfig config, boolean remove) {
        long roleId = config.roleId;
        DiscordRole role = discordSRV.discordAPI().getRoleById(roleId);
        if (role == null) {
            return CompletableFuture.completedFuture(GroupSyncResult.ROLE_DOESNT_EXIST);
        }

        if (!role.getGuild().getSelfMember().canInteract(role)) {
            return CompletableFuture.completedFuture(GroupSyncResult.ROLE_CANNOT_INTERACT);
        }

        return role.getGuild().retrieveMemberById(userId).thenCompose(member -> {
            if (member == null) {
                return CompletableFuture.completedFuture(GroupSyncResult.NOT_A_GUILD_MEMBER);
            }

            Map<Long, Boolean> expected = expectedDiscordChanges.get(userId, key -> new ConcurrentHashMap<>());
            if (expected != null) {
                expected.put(roleId, remove);
            }

            boolean hasRole = member.hasRole(role);
            CompletableFuture<GroupSyncResult> future;
            if (remove && hasRole) {
                future = member.removeRole(role).thenApply(v -> GroupSyncResult.REMOVE_ROLE);
            } else if (!remove && !hasRole) {
                future = member.addRole(role).thenApply(v -> GroupSyncResult.ADD_ROLE);
            } else {
                if (expected != null) {
                    // Nothing needed to be changed, remove expectation
                    expected.remove(roleId);
                }
                return CompletableFuture.completedFuture(GroupSyncResult.ALREADY_IN_SYNC);
            }

            CompletableFuture<GroupSyncResult> resultFuture = new CompletableFuture<>();
            future.whenComplete((result, t) -> {
                if (t != null) {
                    if (expected != null) {
                        // Failed, remove expectation
                        expected.remove(roleId);
                    }

                    resultFuture.complete(GroupSyncResult.UPDATE_FAILED);
                    discordSRV.logger().error("Failed to give/take role " + role + " to/from " + member, t);
                    return;
                }
                resultFuture.complete(result);
            });
            return resultFuture;
        });
    }
}
