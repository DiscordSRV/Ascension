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

package com.discordsrv.common.linking.impl;

import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.player.event.PlayerConnectedEvent;
import com.github.benmanes.caffeine.cache.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class CachedLinkProvider implements LinkProvider {

    private static final long UNLINKED_USER = -1L;
    private static final UUID UNLINKED_UUID = new UUID(0, 0);

    protected final DiscordSRV discordSRV;
    private final Cache<Long, UUID> userToPlayer;
    private final AsyncLoadingCache<UUID, Long> playerToUser;

    public CachedLinkProvider(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.userToPlayer = discordSRV.caffeineBuilder().build();
        this.playerToUser = discordSRV.caffeineBuilder()
                .refreshAfterWrite(30, TimeUnit.SECONDS)
                .removalListener((RemovalListener<UUID, Long>) (key, value, cause) -> {
                    if (value != null) {
                        userToPlayer.invalidate(value);
                    }
                })
                .buildAsync(new AsyncCacheLoader<UUID, Long>() {
                    @Override
                    public @NonNull CompletableFuture<Long> asyncLoad(@NonNull UUID key, @NonNull Executor executor) {
                        return queryUserId(key).thenApply(opt -> opt.orElse(UNLINKED_USER));
                    }

                    @Override
                    public @NonNull CompletableFuture<Long> asyncReload(
                            @NonNull UUID key,
                            @NonNull Long oldValue,
                            @NonNull Executor executor
                    ) {
                        if (!discordSRV.playerProvider().player(key).isPresent()) {
                            // Don't keep players that aren't online in cache
                            return CompletableFuture.completedFuture(null);
                        }

                        return asyncLoad(key, executor);
                    }
                });
        discordSRV.eventBus().subscribe(this);
    }

    @Override
    public CompletableFuture<Optional<Long>> getUserId(@NotNull UUID playerUUID) {
        return playerToUser.get(playerUUID).thenApply(value -> {
            if (value == UNLINKED_USER) {
                return Optional.empty();
            }
            return Optional.of(value);
        });
    }

    @Override
    public Optional<Long> getCachedUserId(@NotNull UUID player) {
        Long value = playerToUser.synchronous().getIfPresent(player);
        return Optional.ofNullable(value == null || value == UNLINKED_USER ? null : value);
    }

    @Override
    public CompletableFuture<Optional<UUID>> getPlayerUUID(long userId) {
        UUID player = userToPlayer.getIfPresent(userId);
        if (player != null) {
            return CompletableFuture.completedFuture(player == UNLINKED_UUID ? Optional.empty() : Optional.of(player));
        }

        return queryPlayerUUID(userId).thenApply(optional -> {
            if (!optional.isPresent()) {
                userToPlayer.put(userId, UNLINKED_UUID);
                return optional;
            }

            UUID uuid = optional.get();
            userToPlayer.put(userId, uuid);
            return optional;
        });
    }

    @Override
    public Optional<UUID> getCachedPlayerUUID(long discordId) {
        UUID value = userToPlayer.getIfPresent(discordId);
        return Optional.ofNullable(value == null || value == UNLINKED_UUID ? null : value);
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        // Cache logged in players
        playerToUser.get(event.player().uniqueId());
    }
}
