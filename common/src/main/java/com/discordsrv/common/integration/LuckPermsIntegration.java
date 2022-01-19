/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.module.type.PermissionDataProvider;
import com.discordsrv.common.module.type.PluginIntegration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class LuckPermsIntegration extends PluginIntegration<DiscordSRV> implements PermissionDataProvider.All {

    private LuckPerms luckPerms;

    public LuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
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
    }

    @Override
    public void disable() {
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
    public CompletableFuture<Boolean> hasGroup(UUID player, String groupName) {
        return user(player).thenApply(user -> {
            for (Group inheritedGroup : user.getInheritedGroups(QueryOptions.defaultContextualOptions())) {
                if (inheritedGroup.getName().equalsIgnoreCase(groupName)) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Void> addGroup(UUID player, String groupName) {
        return groupMutate(player, groupName, NodeMap::add);
    }

    @Override
    public CompletableFuture<Void> removeGroup(UUID player, String groupName) {
        return groupMutate(player, groupName, NodeMap::remove);
    }

    private CompletableFuture<Void> groupMutate(UUID player, String groupName, BiFunction<NodeMap, Node, DataMutateResult> function) {
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Group does not exist"));
            return future;
        }

        return user(player).thenApply(user -> {
            DataMutateResult result = function.apply(user.data(), InheritanceNode.builder(group).build());
            if (result != DataMutateResult.SUCCESS) {
                throw new RuntimeException(result.name());
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
        return user(player).thenApply(
                user -> user.getCachedData().getPermissionData().checkPermission(permission).asBoolean());
    }

    @Override
    public CompletableFuture<String> getPrefix(UUID player) {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getPrefix());
    }

    @Override
    public CompletableFuture<String> getSuffix(UUID player) {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getSuffix());
    }

    @Override
    public CompletableFuture<String> getMeta(UUID player, String key) throws UnsupportedOperationException {
        return user(player).thenApply(user -> user.getCachedData().getMetaData().getMetaValue(key));
    }
}
