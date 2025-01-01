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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.module.type.PermissionModule;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class GamePermissionContext {

    public static final String PREFIX_META_KEY = "discordsrv_prefix";
    public static final String SUFFIX_META_KEY = "discordsrv_suffix";

    private final DiscordSRV discordSRV;

    public GamePermissionContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Placeholder("player_prefix")
    public Object getPrefix(IOfflinePlayer player) {
        FormattedText meta = getMetaPrefix(player);
        return meta != null ? meta : getPermissionPrefix(player);
    }

    @Placeholder("player_suffix")
    public Object getSuffix(IOfflinePlayer player) {
        FormattedText meta = getMetaSuffix(player);
        return meta != null ? meta : getPermissionSuffix(player);
    }

    private FormattedText getMeta(UUID uuid, String metaKey) {
        PermissionModule.Meta meta = discordSRV.getModule(PermissionModule.Meta.class);
        if (meta == null) {
            return null;
        }

        String data = meta.getMeta(uuid, metaKey).join();
        return FormattedText.of(data);
    }

    @Placeholder("player_meta_prefix")
    public FormattedText getMetaPrefix(IOfflinePlayer player) {
        return getMeta(player.uniqueId(), PREFIX_META_KEY);
    }

    @Placeholder("player_meta_suffix")
    public FormattedText getMetaSuffix(IOfflinePlayer player) {
        return getMeta(player.uniqueId(), SUFFIX_META_KEY);
    }

    private Component getPermissionMeta(
            String what,
            Function<PermissionModule.PrefixAndSuffix, CompletableFuture<String>> function
    ) {
        PermissionModule.PrefixAndSuffix permission = discordSRV.getModule(PermissionModule.PrefixAndSuffix.class);
        if (permission == null) {
            return null;
        }

        String data = null;
        try {
            data = function.apply(permission).get();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            discordSRV.logger().debug("Failed to lookup " + what, e.getCause());
        }

        if (data == null) {
            return null;
        }

        return discordSRV.componentFactory().parse(data);
    }

    @Placeholder("player_permission_prefix")
    public Component getPermissionPrefix(IOfflinePlayer player) {
        return getPermissionMeta("prefix", permissions -> permissions.getPrefix(player.uniqueId()));
    }

    @Placeholder("player_permission_suffix")
    public Component getPermissionSuffix(IOfflinePlayer player) {
        return getPermissionMeta("suffix", permissions -> permissions.getSuffix(player.uniqueId()));
    }
}
