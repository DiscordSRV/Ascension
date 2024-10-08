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
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class BukkitChatListener implements Listener {

    private final IBukkitChatForwarder forwarder;

    public BukkitChatListener(IBukkitChatForwarder forwarder) {
        this.forwarder = forwarder;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAsyncPlayerChatAnnotate(AsyncPlayerChatEvent event) {
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getMessage()));

        MinecraftComponent annotated = forwarder.renderChatMessage(event, event.getPlayer(), component);
        if (annotated != null) {
            event.setMessage(BukkitComponentSerializer.legacy().serialize(ComponentUtil.fromAPI(annotated)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChatForward(AsyncPlayerChatEvent event) {
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getMessage()));

        forwarder.forwardMessage(event, event.getPlayer(), component, event.isCancelled());
    }
}
