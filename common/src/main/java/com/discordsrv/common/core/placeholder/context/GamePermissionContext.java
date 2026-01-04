/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.module.type.PermissionModule;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.function.Function;

public class GamePermissionContext {

    public static final String PREFIX_META_KEY = "discordsrv_prefix";
    public static final String SUFFIX_META_KEY = "discordsrv_suffix";

    private final DiscordSRV discordSRV;

    public GamePermissionContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Placeholder("player_prefix")
    public Task<?> getPrefix(IOfflinePlayer player) {
        return getMetaPrefix(player)
                .then(metaValue -> (Task<?>) (metaValue != null ? Task.completed(metaValue) : getPermissionPrefix(player)));
    }

    @Placeholder("player_suffix")
    public Task<?> getSuffix(IOfflinePlayer player) {
        return getMetaSuffix(player)
                .then(metaValue -> (Task<?>) (metaValue != null ? Task.completed(metaValue) : getPermissionSuffix(player)));
    }

    @Placeholder("player_primary_group")
    public Task<String> getPrimaryGroup(IOfflinePlayer player) {
        PermissionModule.Groups permission = discordSRV.getModule(PermissionModule.Groups.class);
        return permission != null ? permission.getPrimaryGroup(player.uniqueId()) : null;
    }

    private Task<FormattedText> getMeta(UUID uuid, String metaKey) {
        PermissionModule.Meta meta = discordSRV.getModule(PermissionModule.Meta.class);
        if (meta == null) {
            return Task.completed(null);
        }

        return meta.getMeta(uuid, metaKey).thenApply(FormattedText::of);
    }

    @Placeholder("player_meta_prefix")
    public Task<FormattedText> getMetaPrefix(IOfflinePlayer player) {
        return getMeta(player.uniqueId(), PREFIX_META_KEY);
    }

    @Placeholder("player_meta_suffix")
    public Task<FormattedText> getMetaSuffix(IOfflinePlayer player) {
        return getMeta(player.uniqueId(), SUFFIX_META_KEY);
    }

    private Task<Component> getPermissionMeta(
            String what,
            Function<PermissionModule.PrefixAndSuffix, Task<String>> function
    ) {
        PermissionModule.PrefixAndSuffix permission = discordSRV.getModule(PermissionModule.PrefixAndSuffix.class);
        if (permission == null) {
            return Task.completed(null);
        }

        return function.apply(permission)
                .mapException(Throwable.class, t -> {
                    discordSRV.logger().debug("Failed to lookup " + what, t);
                    return null;
                })
                .thenApply(data -> {
                    if (data == null) {
                        return null;
                    }

                    return discordSRV.componentFactory().parse(data);
                });
    }

    @Placeholder("player_permission_prefix")
    public Task<Component> getPermissionPrefix(IOfflinePlayer player) {
        return getPermissionMeta("prefix", permissions -> permissions.getPrefix(player.uniqueId()));
    }

    @Placeholder("player_permission_suffix")
    public Task<Component> getPermissionSuffix(IOfflinePlayer player) {
        return getPermissionMeta("suffix", permissions -> permissions.getSuffix(player.uniqueId()));
    }
}
