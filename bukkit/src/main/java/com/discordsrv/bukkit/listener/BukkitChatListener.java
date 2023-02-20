/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.event.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.channel.GlobalChannel;
import com.discordsrv.common.component.util.ComponentUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public abstract class BukkitChatListener implements Listener {

    public static BukkitChatListener get(BukkitDiscordSRV discordSRV) {
        // TODO: config option
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (1 == 2 && PaperComponentHandle.IS_PAPER_ADVENTURE) {
            return new Paper(discordSRV);
        }

        return new Bukkit(discordSRV);
    }

    protected final BukkitDiscordSRV discordSRV;

    public BukkitChatListener(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    protected void publishEvent(Player player, MinecraftComponent component, boolean cancelled) {
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(
                        discordSRV.playerProvider().player(player),
                        new GlobalChannel(discordSRV),
                        component,
                        cancelled
                )
        ));
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

        private final PaperComponentHandle<AsyncChatEvent> componentHandle;

        public Paper(BukkitDiscordSRV discordSRV) {
            super(discordSRV);
            this.componentHandle = new PaperComponentHandle<>(
                    AsyncChatEvent.class,
                    "message",
                    null
            );
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onAsyncChat(AsyncChatEvent event) {
            MinecraftComponent component = componentHandle.getComponent(discordSRV, event);
            publishEvent(event.getPlayer(), component, event.isCancelled());
        }
    }

}
