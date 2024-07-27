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

package com.discordsrv.bukkit.listener;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.receive.game.JoinMessageReceiveEvent;
import com.discordsrv.api.events.message.receive.game.LeaveMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitStatusMessageListener implements Listener {

    private static final PaperComponentHandle<PlayerJoinEvent> JOIN_HANDLE;
    private static final PaperComponentHandle<PlayerQuitEvent> QUIT_HANDLE;

    static {
        JOIN_HANDLE = new PaperComponentHandle<>(
                PlayerJoinEvent.class,
                "joinMessage",
                PlayerJoinEvent::getJoinMessage
        );
        QUIT_HANDLE = new PaperComponentHandle<>(
                PlayerQuitEvent.class,
                "quitMessage",
                PlayerQuitEvent::getQuitMessage
        );
    }

    private final BukkitDiscordSRV discordSRV;

    public BukkitStatusMessageListener(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        DiscordSRVPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        MinecraftComponent component = JOIN_HANDLE.getComponent(event);
        boolean firstJoin = !event.getPlayer().hasPlayedBefore();

        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new JoinMessageReceiveEvent(event, player, component, null, firstJoin, false)
        ));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        DiscordSRVPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        MinecraftComponent component = QUIT_HANDLE.getComponent(event);

        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new LeaveMessageReceiveEvent(event, player, component, null, false)
        ));
    }
}
