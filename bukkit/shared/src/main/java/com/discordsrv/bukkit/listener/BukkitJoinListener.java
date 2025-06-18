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
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class BukkitJoinListener extends AbstractBukkitListener<PlayerJoinEvent> {

    public BukkitJoinListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "JOIN_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerJoinEvent event, Void __) {
        String message = event.getJoinMessage();
        MinecraftComponent component = message == null ? null : ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(message));
        boolean firstJoin = !event.getPlayer().hasPlayedBefore();

        DiscordSRVPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(
                new JoinMessagePreProcessEvent(
                        event,
                        player,
                        component,
                        null,
                        firstJoin,
                        false,
                        component == null,
                        false
                )
        );
    }

    private EventObserver<PlayerJoinEvent, Boolean> observer;

    @Override
    protected void observeEvents(boolean enable) {
        observer = observeEvent(observer, PlayerJoinEvent.class, event -> event.getJoinMessage() == null, enable);
    }
}
