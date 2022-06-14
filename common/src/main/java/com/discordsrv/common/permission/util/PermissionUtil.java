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

package com.discordsrv.common.permission.util;

import com.discordsrv.api.module.type.PermissionDataProvider;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class PermissionUtil {

    public static final String PREFIX_META_KEY = "discordsrv_prefix";
    public static final String SUFFIX_META_KEY = "discordsrv_suffix";

    private PermissionUtil() {}

    public static Component getMetaPrefix(DiscordSRV discordSRV, UUID uuid) {
        return getMeta(discordSRV, uuid, PREFIX_META_KEY);
    }
    public static Component getMetaSuffix(DiscordSRV discordSRV, UUID uuid) {
        return getMeta(discordSRV, uuid, SUFFIX_META_KEY);
    }

    public static Component getPrefix(DiscordSRV discordSRV, UUID uuid) {
        return getLegacy(discordSRV, perm -> perm.getPrefix(uuid));
    }

    public static Component getSuffix(DiscordSRV discordSRV, UUID uuid) {
        return getLegacy(discordSRV, perm -> perm.getSuffix(uuid));
    }

    private static Component getMeta(DiscordSRV discordSRV, UUID uuid, String metaKey) {
        PermissionDataProvider.Meta meta = discordSRV.getModule(PermissionDataProvider.Meta.class);
        if (meta == null) {
            return null;
        }

        String data = meta.getMeta(uuid, metaKey).join();
        return translate(discordSRV, data);
    }

    private static Component getLegacy(
            DiscordSRV discordSRV,
            Function<PermissionDataProvider.PrefixAndSuffix, CompletableFuture<String>> legacy
    ) {
        PermissionDataProvider.PrefixAndSuffix permission = discordSRV.getModule(PermissionDataProvider.PrefixAndSuffix.class);
        if (permission == null) {
            return null;
        }

        String data = legacy.apply(permission).join();
        return translate(discordSRV, data);
    }

    private static Component translate(DiscordSRV discordSRV, String data) {
        return data != null ? ComponentUtil.fromAPI(discordSRV.componentFactory().textBuilder(data).build()) : null;
    }
}
