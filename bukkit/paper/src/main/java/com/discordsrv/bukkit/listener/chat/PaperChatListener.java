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

package com.discordsrv.bukkit.listener.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PaperChatListener implements Listener {

    private static final PaperComponentHandle<AsyncChatEvent> COMPONENT_HANDLE;

    static {
        COMPONENT_HANDLE = new PaperComponentHandle<>(
                AsyncChatEvent.class,
                "message",
                null
        );
    }

    private final IBukkitChatForwarder listener;

    public PaperChatListener(IBukkitChatForwarder listener) {
        this.listener = listener;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncChat(AsyncChatEvent event) {
        MinecraftComponent component = COMPONENT_HANDLE.getComponent(event);
        listener.publishEvent(event, event.getPlayer(), component, event.isCancelled());
    }
}
