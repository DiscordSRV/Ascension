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

package com.discordsrv.bungee.player;

import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.common.abstraction.player.provider.AbstractPlayerProvider;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeePlayerProvider extends AbstractPlayerProvider<BungeePlayer, BungeeDiscordSRV> implements Listener {

    public BungeePlayerProvider(BungeeDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void subscribe() {
        discordSRV.proxy().getPluginManager().registerListener(discordSRV.plugin(), this);

        // Add players that are already connected
        for (ProxiedPlayer player : discordSRV.proxy().getPlayers()) {
            addPlayer(player, true);
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE) // Runs first
    public void onPostLogin(PostLoginEvent event) {
        addPlayer(event.getPlayer(), false);
    }

    private void addPlayer(ProxiedPlayer player, boolean initial) {
        addPlayer(player.getUniqueId(), new BungeePlayer(discordSRV, player), initial);
    }

    @EventHandler(priority = Byte.MAX_VALUE) // Runs last
    public void onDisconnect(PlayerDisconnectEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    public BungeePlayer player(ProxiedPlayer player) {
        BungeePlayer srvPlayer = player(player.getUniqueId());
        if (srvPlayer == null) {
            throw new IllegalStateException("Player not available");
        }
        return srvPlayer;
    }

}
