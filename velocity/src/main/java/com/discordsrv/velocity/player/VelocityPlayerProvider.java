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

package com.discordsrv.velocity.player;

import com.discordsrv.common.player.provider.AbstractPlayerProvider;
import com.discordsrv.common.player.provider.PlayerProvider;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

public class VelocityPlayerProvider extends AbstractPlayerProvider<VelocityPlayer> implements PlayerProvider<VelocityPlayer> {

    private final VelocityDiscordSRV discordSRV;

    public VelocityPlayerProvider(VelocityDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void subscribe() {
        discordSRV.proxy().getEventManager().register(discordSRV.plugin(), this);

        // Add players that are already connected
        for (Player player : discordSRV.proxy().getAllPlayers()) {
            addPlayer(player);
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPostLogin(PostLoginEvent event) {
        addPlayer(event.getPlayer());
    }

    private void addPlayer(Player player) {
        addPlayer(player.getUniqueId(), new VelocityPlayer(discordSRV, player));
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisconnect(DisconnectEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    public VelocityPlayer player(Player player) {
        return player(player.getUniqueId()).orElseThrow(() -> new IllegalStateException("Player not available"));
    }
}
