/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.event.events.message.receive.game.ChatMessageProcessingEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.channel.DefaultGlobalChannel;
import com.discordsrv.common.component.util.ComponentUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class BukkitChatListener implements Listener {

    public static BukkitChatListener get(BukkitDiscordSRV discordSRV) {

        // TODO: config option
        //noinspection ConstantConditions
        if (1 == 2) {
            try {
                Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
                return new Paper(discordSRV);
            } catch (ClassNotFoundException ignored) {}
        }

        return new Bukkit(discordSRV);
    }

    protected final BukkitDiscordSRV discordSRV;

    public BukkitChatListener(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    protected void publishEvent(Player player, MinecraftComponent component, boolean cancelled) {
        discordSRV.eventBus().publish(
                new ChatMessageProcessingEvent(
                        discordSRV.playerProvider().player(player),
                        component,
                        new DefaultGlobalChannel(discordSRV),
                        cancelled
                )
        );
    }

    static class Bukkit extends BukkitChatListener {

        public Bukkit(BukkitDiscordSRV discordSRV) {
            super(discordSRV);
        }

        @SuppressWarnings("deprecation") // Paper
        @EventHandler(priority = EventPriority.MONITOR)
        public void onAsyncPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
            MinecraftComponent component = ComponentUtil.toAPI(
                    BukkitComponentSerializer.legacy().deserialize(event.getMessage()));

            publishEvent(event.getPlayer(), component, event.isCancelled());
        }
    }

    static class Paper extends BukkitChatListener {

        private static final Method MESSAGE_METHOD;

        static {
            try {
                MESSAGE_METHOD = AsyncChatEvent.class.getMethod("message");
            } catch (NoSuchMethodException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        public Paper(BukkitDiscordSRV discordSRV) {
            super(discordSRV);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onAsyncChat(AsyncChatEvent event) {
            MinecraftComponent component = discordSRV.componentFactory().empty();

            Object unrelocated;
            try {
                unrelocated = MESSAGE_METHOD.invoke(event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                discordSRV.logger().error("Failed to get message from Paper AsyncChatEvent", e);
                return;
            }

            MinecraftComponent.Adapter adapter = component.unrelocatedAdapter().orElse(null);
            if (adapter == null) {
                discordSRV.logger().error("Failed to get unrelocated adventure adapter for Paper AsyncChatEvent listener");
                return;
            }

            adapter.setComponent(unrelocated);
            publishEvent(
                    event.getPlayer(),
                    component,
                    event.isCancelled()
            );
        }
    }

}
