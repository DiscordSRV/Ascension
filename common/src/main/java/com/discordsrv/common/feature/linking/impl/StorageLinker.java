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

package com.discordsrv.common.feature.linking.impl;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StorageLinker extends CachedLinkProvider.Store {

    public StorageLinker(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID, boolean canCauseLink) {
        return discordSRV.scheduler().supply(() -> {
            Long value = discordSRV.storage().getUserId(playerUUID);
            return Optional.ofNullable(value);
        });
    }

    @Override
    public CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId, boolean canCauseLink) {
        return discordSRV.scheduler().supply(() -> {
            UUID value = discordSRV.storage().getPlayerUUID(userId);
            return Optional.ofNullable(value);
        });
    }

    @Override
    public CompletableFuture<Void> link(@NotNull UUID playerUUID, long userId) {
        return discordSRV.scheduler().execute(() -> discordSRV.storage().createLink(playerUUID, userId));
    }

    @Override
    public CompletableFuture<Void> unlink(@NotNull UUID playerUUID, long userId) {
        return discordSRV.scheduler().execute(() -> discordSRV.storage().removeLink(playerUUID, userId));
    }

    @Override
    public CompletableFuture<Pair<UUID, String>> getCodeLinking(long userId, @NotNull String code) {
        return discordSRV.scheduler().supply(() -> discordSRV.storage().getLinkingCode(code));
    }

    @Override
    public CompletableFuture<Void> removeLinkingCode(@NotNull UUID playerUUID) {
        return discordSRV.scheduler().execute(() -> discordSRV.storage().removeLinkingCode(playerUUID));
    }

    @Override
    public CompletableFuture<Integer> getLinkedAccountCount() {
        return discordSRV.scheduler().supply(() -> discordSRV.storage().getLinkedAccountCount());
    }

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public CompletableFuture<MinecraftComponent> getLinkingInstructions(
            String username,
            UUID playerUUID,
            @Nullable Locale locale,
            @Nullable String requestReason,
            Object... additionalContext
    ) {
        return discordSRV.scheduler().supply(() -> {
            String code = null;
            while (code == null || discordSRV.storage().getLinkingCode(code) != null) {
                code = String.valueOf(secureRandom.nextInt(1000000));
                while (code.length() != 6) {
                    code = "0" + code;
                }
            }

            discordSRV.storage().storeLinkingCode(playerUUID, username, code);
            return discordSRV.messagesConfig(locale).minecraft.storageLinking.textBuilder()
                    .addContext(additionalContext)
                    .addPlaceholder("%code%", code)
                    .addPlaceholder("%player_name%", username)
                    .addPlaceholder("%player_uuid%", playerUUID)
                    .applyPlaceholderService()
                    .build();
        });
    }

    @Override
    public boolean isValidCode(@NotNull String code) {
        return code.matches("[0-9]{6}");
    }
}
