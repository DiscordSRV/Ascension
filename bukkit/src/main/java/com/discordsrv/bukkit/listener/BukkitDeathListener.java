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
import com.discordsrv.api.events.message.receive.game.DeathMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class BukkitDeathListener implements Listener {

    private static final PaperComponentHandle<PlayerDeathEvent> COMPONENT_HANDLE;
    private static final MethodHandle CANCELLED_HANDLE;

    static {
        COMPONENT_HANDLE = new PaperComponentHandle<>(
                PlayerDeathEvent.class,
                "deathMessage",
                PlayerDeathEvent::getDeathMessage
        );

        MethodHandle handle = null;
        try {
            handle = MethodHandles.lookup().findVirtual(
                    Cancellable.class,
                    "isCancelled",
                    MethodType.methodType(boolean.class)
            );
        } catch (ReflectiveOperationException ignored) {}
        CANCELLED_HANDLE = handle;
    }

    private final BukkitDiscordSRV discordSRV;

    public BukkitDeathListener(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        DiscordSRVPlayer player = discordSRV.playerProvider().player(event.getEntity());
        MinecraftComponent component = COMPONENT_HANDLE.getComponent(event);

        boolean cancelled = false;
        if (CANCELLED_HANDLE != null) {
            try {
                cancelled = (boolean) CANCELLED_HANDLE.invokeExact(event);
            } catch (Throwable ignored) {}
        }

        boolean wasCancelled = cancelled;
        discordSRV.eventBus().publish(new DeathMessageReceiveEvent(
                event,
                player,
                component,
                null,
                wasCancelled
        ));
    }
}
