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

package com.discordsrv.neoforge.player;

import com.discordsrv.common.abstraction.player.provider.AbstractPlayerProvider;
import com.discordsrv.neoforge.NeoforgeDiscordSRV;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class NeoforgePlayerProvider extends AbstractPlayerProvider<NeoforgePlayer, NeoforgeDiscordSRV> {

    private boolean enabled = false;

    public NeoforgePlayerProvider(NeoforgeDiscordSRV discordSRV) {
        super(discordSRV);
        NeoForge.EVENT_BUS.addListener(this::onConnection);
        NeoForge.EVENT_BUS.addListener(this::onDisconnect);
    }

    @Override
    public void subscribe() {
        enabled = true;
        if (discordSRV.getServer() == null || discordSRV.getServer().getPlayerList() == null) {
            return; // Server not started yet, So there's no players to add
        }

        // Add players that are already connected
        for (ServerPlayer player : discordSRV.getServer().getPlayerList().getPlayers()) {
            addPlayer(player, true);
        }
    }

    @Override
    public void unsubscribe() {
        enabled = false;
    }

    private void onConnection(PlayerEvent.PlayerLoggedInEvent event) {
        addPlayer((ServerPlayer) event.getEntity(), false);
    }

    private void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        removePlayer((ServerPlayer) event.getEntity());
    }

    private void addPlayer(ServerPlayer player, boolean initial) {
        if (!enabled) return;
        addPlayer(player.getUUID(), new NeoforgePlayer(discordSRV, player), initial);
    }

    private void removePlayer(ServerPlayer player) {
        if (!enabled) return;
        removePlayer(player.getUUID());
    }

    public NeoforgePlayer player(ServerPlayer player) {
        NeoforgePlayer srvPlayer = player(player.getUUID());
        if (srvPlayer == null) {
            throw new IllegalStateException("Player not available");
        }
        return srvPlayer;
    }
}
