/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.module.type;

import com.discordsrv.api.module.Module;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PermissionModule extends Module {

    boolean supportsOffline();

    interface Basic extends Groups, Permissions, PrefixAndSuffix {}
    interface All extends Basic, Meta, GroupsContext {}

    interface Groups extends PermissionModule {
        List<String> getGroups();
        Task<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited);
        Task<Void> addGroup(@NotNull UUID player, @NotNull String groupName);
        Task<Void> removeGroup(@NotNull UUID player, @NotNull String groupName);
    }

    interface Permissions extends PermissionModule {
        Task<Boolean> hasPermission(@NotNull UUID player, @NotNull String permission);
    }

    interface PrefixAndSuffix extends PermissionModule {
        Task<String> getPrefix(@NotNull UUID player);
        Task<String> getSuffix(@NotNull UUID player);
    }

    interface Meta extends PermissionModule {
        Task<String> getMeta(@NotNull UUID player, @NotNull String key);
    }

    interface GroupsContext extends Groups {

        Set<String> getDefaultServerContext();
        Task<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited, @Nullable Set<String> serverContext);
        Task<Void> addGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Set<String> serverContext);
        Task<Void> removeGroup(@NotNull UUID player, @NotNull String groupName, @Nullable Set<String> serverContext);

        @Override
        default Task<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited) {
            return hasGroup(player, groupName, includeInherited, null);
        }

        @Override
        default Task<Void> addGroup(@NotNull UUID player, @NotNull String groupName) {
            return addGroup(player, groupName, null);
        }

        @Override
        default Task<Void> removeGroup(@NotNull UUID player, @NotNull String groupName) {
            return removeGroup(player, groupName, null);
        }
    }

}
