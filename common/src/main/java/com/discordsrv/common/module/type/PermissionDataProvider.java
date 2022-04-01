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

package com.discordsrv.common.module.type;

import com.discordsrv.api.module.type.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PermissionDataProvider extends Module {

    boolean supportsOffline();

    interface Basic extends Groups, Permissions, PrefixAndSuffix {}
    interface All extends Basic, Meta, GroupsContext {}

    interface Groups extends PermissionDataProvider {
        List<String> getGroups();
        CompletableFuture<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited);
        CompletableFuture<Void> addGroup(@NotNull UUID player, @NotNull String groupName);
        CompletableFuture<Void> removeGroup(@NotNull UUID player, @NotNull String groupName);
    }

    interface Permissions extends PermissionDataProvider {
        CompletableFuture<Boolean> hasPermission(@NotNull UUID player, @NotNull String permission);
    }

    interface PrefixAndSuffix extends PermissionDataProvider {
        CompletableFuture<String> getPrefix(@NotNull UUID player);
        CompletableFuture<String> getSuffix(@NotNull UUID player);
    }

    interface Meta extends PermissionDataProvider {
        CompletableFuture<String> getMeta(@NotNull UUID player, @NotNull String key);
    }

    interface GroupsContext extends Groups {

        Set<String> getDefaultServerContext();
        CompletableFuture<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited, @Nullable Set<String> serverContext);
        CompletableFuture<Void> addGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Set<String> serverContext);
        CompletableFuture<Void> removeGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Set<String> serverContext);

        @Override
        default CompletableFuture<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited) {
            return hasGroup(player, groupName, includeInherited, null);
        }

        @Override
        default CompletableFuture<Void> addGroup(@NotNull UUID player, @NotNull String groupName) {
            return addGroup(player, groupName, null);
        }

        @Override
        default CompletableFuture<Void> removeGroup(@NotNull UUID player, @NotNull String groupName) {
            return removeGroup(player, groupName, null);
        }
    }

}
