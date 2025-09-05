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

package com.discordsrv.bukkit.listener;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.JoinMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.core.logging.NamedLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class PaperJoinListener extends AbstractBukkitListener<PlayerJoinEvent> {

    private static final PaperComponentHandle.Get<PlayerJoinEvent> MESSAGE_HANDLE
            = PaperComponentHandle.get(PlayerJoinEvent.class, "joinMessage");

    public PaperJoinListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "JOIN_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerJoinEvent event, Void __) {
        if (!event.getPlayer().isOnline()) {
            return;
        }

        MinecraftComponent message = MESSAGE_HANDLE.getAPI(event);
        boolean firstJoin = !event.getPlayer().hasPlayedBefore();

        DiscordSRVPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(
                new JoinMessagePreProcessEvent(
                        event,
                        player,
                        message,
                        null,
                        firstJoin,
                        false,
                        message == null,
                        false
                )
        );
    }

    private EventObserver<PlayerJoinEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerJoinEvent.class, event -> MESSAGE_HANDLE.getRaw(event) == null, enable);
    }
}