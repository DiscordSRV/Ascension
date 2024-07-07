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

package com.discordsrv.common.linking.impl;

import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.event.events.player.PlayerConnectedEvent;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Expiry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public abstract class CachedLinkProvider implements LinkProvider {

    private static final long UNLINKED_USER = -1L;
    private static final UUID UNLINKED_UUID = new UUID(0, 0);

    protected final DiscordSRV discordSRV;
    private final Cache<Long, UUID> userToPlayer;
    private final AsyncLoadingCache<UUID, Long> playerToUser;
    private final Set<UUID> linkingAllowed = new CopyOnWriteArraySet<>();

    public CachedLinkProvider(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.userToPlayer = discordSRV.caffeineBuilder().build();
        this.playerToUser = discordSRV.caffeineBuilder()
                .expireAfter(new Expiry<UUID, Long>() {
                    @Override
                    public long expireAfterCreate(@NotNull UUID key, @NotNull Long value, long currentTime) {
                        return TimeUnit.MINUTES.toNanos(5);
                    }

                    @Override
                    public long expireAfterUpdate(
                            @NotNull UUID key,
                            @NotNull Long value,
                            long currentTime,
                            long currentDuration
                    ) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(
                            @NotNull UUID key,
                            @NotNull Long value,
                            long currentTime,
                            long currentDuration
                    ) {
                        return currentDuration;
                    }
                })
                .removalListener((key, value, cause) -> {
                    if (value != null) {
                        userToPlayer.invalidate(value);
                    }
                })
                .buildAsync(new AsyncCacheLoader<UUID, Long>() {
                    @Override
                    public @NotNull CompletableFuture<Long> asyncLoad(@NotNull UUID key, @NotNull Executor executor) {
                        return queryUserId(key, linkingAllowed.remove(key)).thenApply(opt -> opt.orElse(UNLINKED_USER));
                    }

                    @Override
                    public @NotNull CompletableFuture<Long> asyncReload(
                            @NotNull UUID key,
                            @NotNull Long oldValue,
                            @NotNull Executor executor
                    ) {
                        if (discordSRV.playerProvider().player(key) == null) {
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
        UUID uuid = event.player().uniqueId();
        linkingAllowed.add(uuid);
        playerToUser.get(uuid);
        linkingAllowed.remove(uuid);
    }

    protected void addToCache(UUID playerUUID, long userId) {
        playerToUser.put(playerUUID, CompletableFuture.completedFuture(userId));
    }

    protected void evictFromCache(UUID playerUUID) {
        playerToUser.synchronous().invalidate(playerUUID);
    }

    public static abstract class Store extends CachedLinkProvider implements LinkStore {

        public Store(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        public abstract CompletableFuture<Void> link(@NotNull UUID playerUUID, long userId);
        public abstract CompletableFuture<Void> unlink(@NotNull UUID playerUUID, long userId);

        @Override
        public final CompletableFuture<Void> createLink(@NotNull UUID playerUUID, long userId) {
            return link(playerUUID, userId).thenApply(v -> {
                addToCache(playerUUID, userId);
                return null;
            });
        }

        @Override
        public CompletableFuture<Void> removeLink(@NotNull UUID playerUUID, long userId) {
            return unlink(playerUUID, userId).thenApply(v -> {
                evictFromCache(playerUUID);
                return null;
            });
        }
    }
}
