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

package com.discordsrv.common.integration;

import com.discordsrv.api.module.type.PermissionDataProvider;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.MessageException;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.groupsync.GroupSyncModule;
import com.discordsrv.common.groupsync.enums.GroupSyncCause;
import com.discordsrv.common.module.type.PluginIntegration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;
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
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LuckPermsIntegration extends PluginIntegration<DiscordSRV> implements PermissionDataProvider.All {

    private LuckPerms luckPerms;
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();

    public LuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public @NotNull String getIntegrationName() {
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

    private CompletableFuture<User> user(UUID player) {
        return luckPerms.getUserManager().loadUser(player);
    }

    @Override
    public boolean supportsOffline() {
        return true;
    }

    @Override
    public Set<String> getDefaultServerContext() {
        return luckPerms.getContextManager().getStaticContext().getValues(DefaultContextKeys.SERVER_KEY);
    }

    @Override
    public CompletableFuture<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited, @Nullable Set<String> serverContext) {
        return user(player).thenApply(user -> {
            MutableContextSet context = luckPerms.getContextManager().getStaticContext().mutableCopy();
            if (serverContext != null) {
                context.removeAll(DefaultContextKeys.SERVER_KEY);
                if (isNotGlobalOnly(serverContext)) {
                    for (String ctx : serverContext) {
                        context.add(DefaultContextKeys.SERVER_KEY, ctx);
                    }
                }
            }

            return (
                includeInherited
                    ? user.getInheritedGroups(QueryOptions.builder(QueryMode.CONTEXTUAL).context(context).build())
                        .stream()
                        .map(Group::getName)
                    : user.getNodes(NodeType.INHERITANCE)
                        .stream()
                        .filter(node -> context.isSatisfiedBy(node.getContexts()))
                        .map(InheritanceNode::getGroupName)
            ).anyMatch(name -> name.equalsIgnoreCase(groupName));
        });
    }

    @Override
    public CompletableFuture<Void> addGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Set<String> serverContext) {
        return groupMutate(player, groupName, serverContext, NodeMap::add);
    }

    @Override
    public CompletableFuture<Void> removeGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Set<String> serverContext) {
        return groupMutate(player, groupName, serverContext, NodeMap::remove);
    }

    private CompletableFuture<Void> groupMutate(UUID player, String groupName, Set<String> serverContext, BiFunction<NodeMap, Node, DataMutateResult> function) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return CompletableFutureUtil.failed(new MessageException("Group does not exist"));
        }
        return user(player).thenCompose(user -> {
            ContextSet contexts;
            if (serverContext != null) {
                if (isNotGlobalOnly(serverContext)) {
                    ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
                    for (String ctx : serverContext) {
                        builder.add(DefaultContextKeys.SERVER_KEY, ctx);
                    }
                    contexts = builder.build();
                } else {
                    contexts = ImmutableContextSet.empty();
                }
            } else {
                MutableContextSet contextSet = MutableContextSet.create();
                for (String value : getDefaultServerContext()) {
                    contextSet.add(DefaultContextKeys.SERVER_KEY, value);
                }
                contexts = contextSet;
            }

            InheritanceNode node = InheritanceNode.builder(group).context(contexts).build();
            DataMutateResult result = function.apply(user.data(), node);
            if (result != DataMutateResult.SUCCESS) {
                return CompletableFutureUtil.failed(new MessageException(result.name()));
            }

            return luckPerms.getUserManager().saveUser(user);
        });
    }

    private boolean isNotGlobalOnly(Set<String> context) {
        return context.size() != 1 || !context.iterator().next().equals("global");
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(@NotNull UUID player, @NotNull String permission) {
        return user(player).thenApply(
                user -> user.getCachedData().getPermissionData().checkPermission(permission).asBoolean());
    }

    @Override
    public CompletableFuture<String> getPrefix(@NotNull UUID player) {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getPrefix());
    }

    @Override
    public CompletableFuture<String> getSuffix(@NotNull UUID player) {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getSuffix());
    }

    @Override
    public CompletableFuture<String> getMeta(@NotNull UUID player, @NotNull String key) throws UnsupportedOperationException {
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
        event.getGroupFrom().ifPresent(group -> groupUpdate(user, group, Collections.emptySet(), true, true));
        event.getGroupTo().ifPresent(group -> groupUpdate(user, group, Collections.emptySet(), false, true));
    }

    private void nodeUpdate(PermissionHolder holder, Node node, boolean remove) {
        if (!(holder instanceof User) || node.getType() != NodeType.INHERITANCE) {
            return;
        }

        InheritanceNode inheritanceNode = NodeType.INHERITANCE.cast(node);
        String groupName = inheritanceNode.getGroupName();
        Set<String> serverContext = inheritanceNode.getContexts().getValues(DefaultContextKeys.SERVER_KEY);

        groupUpdate((User) holder, groupName, serverContext, remove, false);
    }

    private void groupUpdate(User user, String groupName, Set<String> serverContext, boolean remove, boolean track) {
        GroupSyncModule module = discordSRV.getModule(GroupSyncModule.class);
        if (module == null || !module.isEnabled()) {
            return;
        }

        GroupSyncCause cause = track ? GroupSyncCause.LUCKPERMS_TRACK : GroupSyncCause.LUCKPERMS_NODE_CHANGE;
        UUID uuid = user.getUniqueId();
        if (remove) {
            module.groupRemoved(uuid, groupName, serverContext, cause);
        } else {
            module.groupAdded(uuid, groupName, serverContext, cause);
        }
    }

    @Override
    public List<String> getGroups() {
        return luckPerms.getGroupManager().getLoadedGroups().stream()
                .map(Group::getName)
                .collect(Collectors.toList());
    }
}
