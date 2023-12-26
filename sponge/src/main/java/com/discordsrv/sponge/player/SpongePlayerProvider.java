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

package com.discordsrv.sponge.player;

import com.discordsrv.common.player.IOfflinePlayer;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.ServerPlayerProvider;
import com.discordsrv.sponge.SpongeDiscordSRV;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SpongePlayerProvider extends ServerPlayerProvider<SpongePlayer, SpongeDiscordSRV> {

    public SpongePlayerProvider(SpongeDiscordSRV discordSRV) {
        super(discordSRV);
    }

    // IPlayer

    @Override
    public void subscribe() {
        discordSRV.game().eventManager().registerListeners(discordSRV.container(), this);

        // Add players that are already connected
        for (ServerPlayer player : discordSRV.game().server().onlinePlayers()) {
            addPlayer(player, true);
        }
    }

    @Listener(order = Order.PRE)
    public void onPlayerJoin(ServerSideConnectionEvent.Join event) {
        addPlayer(event.player(), false);
    }

    private void addPlayer(ServerPlayer player, boolean initial) {
        addPlayer(player.uniqueId(), new SpongePlayer(discordSRV, player), initial);
    }

    @Listener(order = Order.POST)
    public void onPlayerDisconnect(ServerSideConnectionEvent.Disconnect event) {
        removePlayer(event.player().uniqueId());
    }

    public SpongePlayer player(ServerPlayer player) {
        SpongePlayer srvPlayer = player(player.uniqueId());
        if (srvPlayer == null) {
            throw new IllegalStateException("Player not available");
        }
        return srvPlayer;
    }

    // IOfflinePlayer

    private IOfflinePlayer convert(User user) {
        return new SpongeOfflinePlayer(discordSRV, user);
    }

    @Override
    public CompletableFuture<IOfflinePlayer> lookupOfflinePlayer(UUID uuid) {
        IPlayer player = player(uuid);
        if (player != null) {
            return CompletableFuture.completedFuture(player);
        }

        return discordSRV.game().server().userManager()
                .load(uuid)
                .thenApply(optional -> optional.map(this::convert).orElse(null));
    }

    @Override
    public CompletableFuture<IOfflinePlayer> lookupOfflinePlayer(String username) {
        IPlayer player = player(username);
        if (player != null) {
            return CompletableFuture.completedFuture(player);
        }

        return discordSRV.game().server().userManager()
                .load(username)
                .thenApply(optional -> optional.map(this::convert).orElse(null));
    }

    public IOfflinePlayer offlinePlayer(User user) {
        return convert(user);
    }
}
