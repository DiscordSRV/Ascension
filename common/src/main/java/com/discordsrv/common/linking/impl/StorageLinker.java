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

package com.discordsrv.common.linking.impl;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StorageLinker extends CachedLinkProvider implements LinkProvider, LinkStore {

    public StorageLinker(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Long value = discordSRV.storage().getUserId(playerUUID);
            return Optional.ofNullable(value);
        }, discordSRV.scheduler().executor());
    }

    @Override
    public CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            UUID value = discordSRV.storage().getPlayerUUID(userId);
            return Optional.ofNullable(value);
        }, discordSRV.scheduler().executor());
    }

    @Override
    public CompletableFuture<Void> createLink(@NotNull UUID playerUUID, long userId) {
        return CompletableFuture.runAsync(
                () -> discordSRV.storage().createLink(playerUUID, userId),
                discordSRV.scheduler().executor()
        );
    }

    @Override
    public CompletableFuture<Void> removeLink(@NotNull UUID playerUUID, long userId) {
        return CompletableFuture.runAsync(
                () -> discordSRV.storage().removeLink(playerUUID, userId),
                discordSRV.scheduler().executor()
        );
    }

    @Override
    public CompletableFuture<UUID> getCodeLinking(long userId, @NotNull String code) {
        return CompletableFuture.supplyAsync(
                () -> discordSRV.storage().getLinkingCode(code),
                discordSRV.scheduler().executor()
        );
    }

    @Override
    public CompletableFuture<Void> removeLinkingCode(@NotNull String code) {
        return CompletableFuture.runAsync(
                () -> {
                    UUID player = discordSRV.storage().getLinkingCode(code);
                    discordSRV.storage().removeLinkingCode(player);
                },
                discordSRV.scheduler().executor()
        );
    }

    @Override
    public CompletableFuture<Void> removeLinkingCode(@NotNull UUID playerUUID) {
        return CompletableFuture.runAsync(
                () -> discordSRV.storage().removeLinkingCode(playerUUID),
                discordSRV.scheduler().executor()
        );
    }

    @Override
    public CompletableFuture<Integer> getLinkedAccountCount() {
        return CompletableFuture.supplyAsync(
                () -> discordSRV.storage().getLinkedAccountCount(),
                discordSRV.scheduler().executor()
        );
    }

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public CompletableFuture<MinecraftComponent> getLinkingInstructions(String username, UUID playerUUID, @Nullable Locale locale, @Nullable String requestReason) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String code = null;
                    while (code == null || discordSRV.storage().getLinkingCode(code) != null) {
                        code = String.valueOf(secureRandom.nextInt(1000000));
                        while (code.length() != 6) {
                            code = "0" + code;
                        }
                    }

                    discordSRV.storage().storeLinkingCode(playerUUID, code);
                    return ComponentUtil.toAPI(Component.text(code));
                },
                discordSRV.scheduler().executor()
        );
    }
}
