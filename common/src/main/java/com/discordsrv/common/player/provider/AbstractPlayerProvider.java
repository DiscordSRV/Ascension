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

package com.discordsrv.common.player.provider;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.event.PlayerConnectedEvent;
import com.discordsrv.common.player.event.PlayerDisconnectedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractPlayerProvider<T extends IPlayer, DT extends DiscordSRV> implements PlayerProvider<T> {

    private final Map<UUID, T> players = new ConcurrentHashMap<>();
    private final List<T> allPlayers = new CopyOnWriteArrayList<>();
    protected final DT discordSRV;

    public AbstractPlayerProvider(DT discordSRV) {
        this.discordSRV = discordSRV;
    }

    public abstract void subscribe();

    protected void addPlayer(UUID uuid, T player, boolean initial) {
        this.players.put(uuid, player);
        this.allPlayers.add(player);
        discordSRV.scheduler().runFork(() -> discordSRV.eventBus().publish(new PlayerConnectedEvent(player, initial)));
    }

    protected void removePlayer(UUID uuid) {
        T player = this.players.remove(uuid);
        if (player != null) {
            allPlayers.remove(player);
            discordSRV.scheduler().runFork(() -> discordSRV.eventBus().publish(new PlayerDisconnectedEvent(player)));
        }
    }

    @Override
    public final @NotNull Optional<T> player(@NotNull UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    @Override
    public final @NotNull Optional<T> player(@NotNull String username) {
        for (T value : allPlayers) {
            if (value.username().equalsIgnoreCase(username)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    @Override
    public @NotNull Collection<T> allPlayers() {
        return allPlayers;
    }
}
