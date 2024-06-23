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

package com.discordsrv.common.player.provider;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.event.events.player.PlayerConnectedEvent;
import com.discordsrv.common.event.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.http.util.HttpUtil;
import com.discordsrv.common.player.IOfflinePlayer;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.OfflinePlayer;
import com.discordsrv.common.player.provider.model.GameProfileResponse;
import com.discordsrv.common.player.provider.model.SkinInfo;
import com.discordsrv.common.player.provider.model.Textures;
import com.discordsrv.common.player.provider.model.UUIDResponse;
import com.discordsrv.common.uuid.util.UUIDUtil;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractPlayerProvider<T extends IPlayer, DT extends DiscordSRV> implements PlayerProvider<T> {

    private static final String MOJANG_API_URL = "https://api.mojang.com";
    private static final String USERNAME_TO_UUID_URL = MOJANG_API_URL + "/users/profiles/minecraft/%s";
    private static final String UUID_TO_PROFILE_URL = MOJANG_API_URL + "/session/minecraft/profile/%s";

    private final Map<UUID, T> players = new ConcurrentHashMap<>();
    private final List<T> allPlayers = new CopyOnWriteArrayList<>();
    protected final DT discordSRV;
    private final AtomicBoolean anyOffline = new AtomicBoolean(false);

    public AbstractPlayerProvider(DT discordSRV) {
        this.discordSRV = discordSRV;
    }

    public boolean isAnyOffline() {
        return anyOffline.get();
    }

    public abstract void subscribe();

    protected void addPlayer(UUID uuid, T player, boolean initial) {
        this.players.put(uuid, player);
        this.allPlayers.add(player);
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(new PlayerConnectedEvent(player, initial)));

        if (uuid.getLeastSignificantBits() != 0 /* Not Geyser */
                && uuid.version() == 3 /* Offline */) {
            anyOffline.set(true);
        }
    }

    protected void removePlayer(UUID uuid) {
        T player = this.players.remove(uuid);
        if (player != null) {
            allPlayers.remove(player);
            discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(new PlayerDisconnectedEvent(player)));
        }
    }

    @Override
    public final @Nullable T player(@NotNull UUID uuid) {
        return players.get(uuid);
    }

    @Override
    public final @Nullable T player(@NotNull String username) {
        for (T value : allPlayers) {
            if (value.username().equalsIgnoreCase(username)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public @NotNull Collection<T> allPlayers() {
        return allPlayers;
    }

    @Override
    public CompletableFuture<UUID> lookupUUIDForUsername(String username) {
        IPlayer player = player(username);
        if (player != null) {
            return CompletableFuture.completedFuture(player.uniqueId());
        }

        Request request = new Request.Builder()
                .url(String.format(USERNAME_TO_UUID_URL, username))
                .get()
                .build();

        return HttpUtil.readJson(discordSRV, request, UUIDResponse.class)
                .thenApply(response -> UUIDUtil.fromShort(response.id));
    }

    @Override
    public CompletableFuture<IOfflinePlayer> lookupOfflinePlayer(UUID uuid) {
        IPlayer player = player(uuid);
        if (player != null) {
            return CompletableFuture.completedFuture(player);
        }

        Request request = new Request.Builder()
                .url(String.format(UUID_TO_PROFILE_URL, uuid))
                .get()
                .build();

        return HttpUtil.readJson(discordSRV, request, GameProfileResponse.class).thenApply(response -> {
            SkinInfo skinInfo = null;
            for (GameProfileResponse.Property property : response.properties) {
                if (!Textures.KEY.equals(property.name)) {
                    continue;
                }

                Textures textures = Textures.getFromBase64(discordSRV, property.value);
                skinInfo = textures.getSkinInfo();
            }

            return new OfflinePlayer(discordSRV, response.name, uuid, skinInfo);
        });
    }
}
