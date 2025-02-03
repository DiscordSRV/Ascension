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
import com.discordsrv.api.events.message.receive.game.LeaveMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class BukkitQuitListener extends AbstractBukkitListener<PlayerQuitEvent> {

    public BukkitQuitListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "QUIT_LISTENER"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerQuitEvent event, Void __) {
        String message = event.getQuitMessage();
        MinecraftComponent component = message == null ? null : ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(message));

        DiscordSRVPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(
                new LeaveMessageReceiveEvent(
                        event,
                        player,
                        component,
                        null,
                        component == null
                )
        );
    }

    private EventObserver<PlayerQuitEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerQuitEvent.class, event -> event.getQuitMessage() == null, enable);
    }
}
