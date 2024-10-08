/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.util;

import com.discordsrv.api.module.type.PermissionModule;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.MessageException;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public final class GamePermissionUtil {

    public static final String PREFIX_META_KEY = "discordsrv_prefix";
    public static final String SUFFIX_META_KEY = "discordsrv_suffix";

    private GamePermissionUtil() {}

    public static FormattedText getMetaPrefix(DiscordSRV discordSRV, UUID uuid) {
        return getMeta(discordSRV, uuid, PREFIX_META_KEY);
    }
    public static FormattedText getMetaSuffix(DiscordSRV discordSRV, UUID uuid) {
        return getMeta(discordSRV, uuid, SUFFIX_META_KEY);
    }

    private static FormattedText getMeta(DiscordSRV discordSRV, UUID uuid, String metaKey) {
        PermissionModule.Meta meta = discordSRV.getModule(PermissionModule.Meta.class);
        if (meta == null) {
            return null;
        }

        String data = meta.getMeta(uuid, metaKey).join();
        return FormattedText.of(data);
    }

    public static Component getPrefix(DiscordSRV discordSRV, UUID uuid) {
        return getLegacy(discordSRV, "prefix", perm -> perm.getPrefix(uuid));
    }

    public static Component getSuffix(DiscordSRV discordSRV, UUID uuid) {
        return getLegacy(discordSRV, "suffix", perm -> perm.getSuffix(uuid));
    }

    private static Component getLegacy(
            DiscordSRV discordSRV,
            String what,
            Function<PermissionModule.PrefixAndSuffix, CompletableFuture<String>> legacy
    ) {
        PermissionModule.PrefixAndSuffix permission = discordSRV.getModule(PermissionModule.PrefixAndSuffix.class);
        if (permission == null) {
            return null;
        }

        String data = null;
        try {
            data = legacy.apply(permission).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MessageException) {
                discordSRV.logger().debug("Failed to lookup " + what + ": " + cause.getMessage());
            } else {
                discordSRV.logger().debug("Failed to lookup " + what, cause);
            }
        }
        return translate(discordSRV, data);
    }

    private static Component translate(DiscordSRV discordSRV, String data) {
        return data != null ? discordSRV.componentFactory().parse(data) : null;
    }
}
