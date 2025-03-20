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

package com.discordsrv.common.integration;

import com.discordsrv.api.module.type.PermissionModule;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.exception.MessageException;
import com.discordsrv.common.feature.groupsync.GroupSyncModule;
import com.discordsrv.common.feature.groupsync.enums.GroupSyncCause;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeClearEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.track.UserTrackEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LuckPermsIntegration extends PluginIntegration<DiscordSRV> implements PermissionModule.All {

    private LuckPerms luckPerms;
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();
    private final Map<UUID, Task<User>> userLoads = new HashMap<>();
    private final Map<UUID, Future<?>> userCleanup = new HashMap<>();

    public LuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "LUCKPERMS"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "LuckPerms";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("net.luckperms.api.LuckPerms");
        } catch (ClassNotFoundException e) {
            return false;
        }

        return super.isEnabled();
    }

    @Override
    public void enable() {
        luckPerms = LuckPermsProvider.get();
        subscribe(NodeAddEvent.class, this::onNodeAdd);
        subscribe(NodeRemoveEvent.class, this::onNodeRemove);
        subscribe(NodeClearEvent.class, this::onNodeClear);
        subscribe(UserTrackEvent.class, this::onUserTrack);
    }

    private <E extends LuckPermsEvent> void subscribe(Class<E> clazz, Consumer<E> method) {
        subscriptions.add(luckPerms.getEventBus().subscribe(clazz, method));
    }

    @Override
    public void disable() {
        subscriptions.forEach(EventSubscription::close);
        subscriptions.clear();
        luckPerms = null;
    }

    private Task<User> user(UUID player) {
        UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(player);
        if (user != null) {
            logger().trace("User in cache: " + player);
            return Task.completed(user);
        }

        synchronized (userLoads) {
            Task<User> task = userLoads.get(player);
            if (task != null) {
                logger().trace("Re-using load future: " + player);
                return task;
            }

            task = Task.of(userManager.loadUser(player));
            task.whenComplete((loadedUser, ___) -> {
                synchronized (userLoads) {
                    userLoads.remove(player);
                }

                synchronized (userCleanup) {
                    Future<?> future = userCleanup.put(player, discordSRV.scheduler().runLater(
                            () -> {
                                logger().trace("Cleaning up " + player);
                                userManager.cleanupUser(loadedUser);
                                synchronized (userCleanup) {
                                    userCleanup.remove(player);
                                }
                            },
                            Duration.ofMinutes(1))
                    );
                    if (future != null) {
                        future.cancel(false);
                    }
                }
            });
            logger().trace("Loading " + player);
            userLoads.put(player, task);
            return task;
        }
    }

    @Override
    public boolean supportsOffline() {
        return true;
    }

    private ContextSet contextSet(Map<String, Set<String>> contexts) {
        if (contexts == null) {
            return ImmutableContextSet.empty();
        }

        ImmutableContextSet.Builder contextSetBuilder = ImmutableContextSet.builder();
        for (Map.Entry<String, Set<String>> entry : contexts.entrySet()) {
            for (String value : entry.getValue()) {
                contextSetBuilder.add(entry.getKey(), value);
            }
        }

        return contextSetBuilder.build();
    }

    @Override
    public Task<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited, @Nullable Map<String, Set<String>> contexts) {
        return user(player).thenApply(user -> {
            QueryOptions options = QueryOptions.builder(QueryMode.CONTEXTUAL)
                    .flag(Flag.RESOLVE_INHERITANCE, includeInherited)
                    .context(contextSet(contexts))
                    .build();

            Set<String> groupNames = user.getInheritedGroups(options).stream().map(Group::getName).collect(Collectors.toSet());
            logger().trace(player + " groups in context " + contexts + ": " + groupNames);
            return groupNames.stream().anyMatch(group -> group.equalsIgnoreCase(groupName));
        });
    }

    @Override
    public Task<Void> addGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Map<String, Set<String>> contexts) {
        return groupMutate(player, groupName, contexts, NodeMap::add);
    }

    @Override
    public Task<Void> removeGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Map<String, Set<String>> contexts) {
        return groupMutate(player, groupName, contexts, NodeMap::remove);
    }

    private Task<Void> groupMutate(UUID player, String groupName, Map<String, Set<String>> contexts, BiFunction<NodeMap, Node, DataMutateResult> function) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return Task.failed(new MessageException("Group does not exist"));
        }
        return user(player).then(user -> {
            InheritanceNode node = InheritanceNode.builder(group)
                    .context(contextSet(contexts))
                    .build();

            DataMutateResult result = function.apply(user.data(), node);
            if (!result.wasSuccessful()) {
                return Task.failed(new MessageException("Group mutate failed: " + result.name()));
            }

            return Task.of(luckPerms.getUserManager().saveUser(user));
        });
    }

    @Override
    public Task<Boolean> hasPermission(@NotNull UUID player, @NotNull String permission) {
        return user(player).thenApply(
                user -> user.getCachedData().getPermissionData().checkPermission(permission).asBoolean());
    }

    @Override
    public Task<String> getPrefix(@NotNull UUID player) {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getPrefix());
    }

    @Override
    public Task<String> getSuffix(@NotNull UUID player) {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getSuffix());
    }

    @Override
    public Task<String> getMeta(@NotNull UUID player, @NotNull String key) throws UnsupportedOperationException {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getMetaValue(key));
    }

    private void onNodeAdd(NodeAddEvent event) {
        nodeUpdate(event.getTarget(), event.getNode(), false);
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        nodeUpdate(event.getTarget(), event.getNode(), true);
    }

    private void onNodeClear(NodeClearEvent event) {
        PermissionHolder target = event.getTarget();
        for (Node node : event.getNodes()) {
            nodeUpdate(target, node, true);
        }
    }

    private void onUserTrack(UserTrackEvent event) {
        User user = event.getUser();
        event.getGroupFrom().ifPresent(group -> groupUpdate(user, group, Collections.emptyMap(), true, true));
        event.getGroupTo().ifPresent(group -> groupUpdate(user, group, Collections.emptyMap(), false, true));
    }

    private void nodeUpdate(PermissionHolder holder, Node node, boolean remove) {
        if (!(holder instanceof User) || node.getType() != NodeType.INHERITANCE) {
            return;
        }

        InheritanceNode inheritanceNode = NodeType.INHERITANCE.cast(node);
        String groupName = inheritanceNode.getGroupName();
        groupUpdate((User) holder, groupName, inheritanceNode.getContexts().toMap(), remove, false);
    }

    private void groupUpdate(User user, String groupName, Map<String, Set<String>> contexts, boolean remove, boolean track) {
        GroupSyncModule module = discordSRV.getModule(GroupSyncModule.class);
        if (module == null || !module.isEnabled()) {
            return;
        }

        GroupSyncCause cause = track ? GroupSyncCause.LUCKPERMS_TRACK : GroupSyncCause.LUCKPERMS_NODE_CHANGE;
        UUID uuid = user.getUniqueId();
        if (remove) {
            module.groupRemoved(uuid, groupName, contexts, cause);
        } else {
            module.groupAdded(uuid, groupName, contexts, cause);
        }
    }

    @Override
    public List<String> getGroups() {
        return luckPerms.getGroupManager().getLoadedGroups().stream()
                .map(Group::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Task<String> getPrimaryGroup(@NotNull UUID player) {
        return user(player).thenApply(User::getPrimaryGroup);
    }
}
