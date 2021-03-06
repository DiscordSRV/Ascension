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

package com.discordsrv.bukkit.player;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.player.IOfflinePlayer;
import com.discordsrv.common.server.player.ServerPlayerProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BukkitPlayerProvider extends ServerPlayerProvider<BukkitPlayer, BukkitDiscordSRV> implements Listener {

    public BukkitPlayerProvider(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    // IPlayer

    @Override
    public void subscribe() {
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());

        // Add players that are already connected
        for (Player player : discordSRV.server().getOnlinePlayers()) {
            addPlayer(player, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        addPlayer(event.getPlayer(), false);
    }

    private void addPlayer(Player player, boolean initial) {
        addPlayer(player.getUniqueId(), new BukkitPlayer(discordSRV, player), initial);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer().getUniqueId());
    }

    public BukkitPlayer player(Player player) {
        return player(player.getUniqueId()).orElseThrow(() -> new IllegalStateException("Player not available"));
    }

    // IOfflinePlayer

    private CompletableFuture<Optional<IOfflinePlayer>> getFuture(Supplier<OfflinePlayer> provider) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer offlinePlayer = provider.get();
            if (offlinePlayer == null) {
                return Optional.empty();
            }

            return Optional.of(new BukkitOfflinePlayer(discordSRV, offlinePlayer));
        }, discordSRV.scheduler().executor());
    }

    @Override
    public CompletableFuture<Optional<IOfflinePlayer>> offlinePlayer(UUID uuid) {
        return getFuture(() -> discordSRV.server().getOfflinePlayer(uuid));
    }

    @SuppressWarnings("deprecation") // Shut up, I know
    @Override
    public CompletableFuture<Optional<IOfflinePlayer>> offlinePlayer(String username) {
        return getFuture(() -> discordSRV.server().getOfflinePlayer(username));
    }

    public IOfflinePlayer offlinePlayer(OfflinePlayer offlinePlayer) {
        return new BukkitOfflinePlayer(discordSRV, offlinePlayer);
    }
}
