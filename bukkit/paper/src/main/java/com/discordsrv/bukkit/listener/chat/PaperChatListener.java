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
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.unrelocate.net.kyori.adventure.text.Component;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class PaperChatListener implements Listener {

    private static final PaperComponentHandle<AsyncChatEvent> GET_MESSAGE_HANDLE = makeGet();
    private static final MethodHandle SET_MESSAGE_HANDLE = makeSet();

    private static PaperComponentHandle<AsyncChatEvent> makeGet() {
        return new PaperComponentHandle<>(
                AsyncChatEvent.class,
                "message",
                null
        );
    }
    @SuppressWarnings("JavaLangInvokeHandleSignature") // Unrelocate
    private static MethodHandle makeSet() {
        try {
            return MethodHandles.lookup().findVirtual(
                    AsyncChatEvent.class,
                    "message",
                    MethodType.methodType(void.class, Component.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException ignored) {}
        return null;
    }

    private final IBukkitChatForwarder listener;
    private final Logger logger;

    public PaperChatListener(IBukkitChatForwarder listener, Logger logger) {
        this.listener = listener;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncChatRender(AsyncChatEvent event) {
        if (SET_MESSAGE_HANDLE == null) {
            return;
        }

        MinecraftComponent component = GET_MESSAGE_HANDLE.getComponent(event);
        MinecraftComponent annotated = listener.annotateChatMessage(event, event.getPlayer(), component);
        if (annotated != null) {
            try {
                SET_MESSAGE_HANDLE.invoke(event, annotated.asAdventure());
            } catch (Throwable t) {
                logger.debug("Failed to render Minecraft message", t);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncChatForward(AsyncChatEvent event) {
        MinecraftComponent component = GET_MESSAGE_HANDLE.getComponent(event);
        listener.forwardMessage(event, event.getPlayer(), component, event.isCancelled());
    }
}
