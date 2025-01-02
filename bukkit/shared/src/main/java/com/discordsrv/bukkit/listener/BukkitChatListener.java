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
import com.discordsrv.api.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class BukkitChatListener extends AbstractBukkitListener<AsyncPlayerChatEvent> {

    public BukkitChatListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHAT_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        receiveEvent(event);
    }

    @Override
    protected void handleEvent(AsyncPlayerChatEvent event) {
        MinecraftComponent component = ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(event.getMessage()));

        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(
                        event,
                        player,
                        component,
                        new GlobalChannel(discordSRV),
                        event.isCancelled()
                )
        );
    }

    private EventObserver<AsyncPlayerChatEvent, Boolean> legacyAsyncObserver;
    @SuppressWarnings("deprecation")
    private EventObserver<org.bukkit.event.player.PlayerChatEvent, Boolean> legacySyncObserver;

    @SuppressWarnings("deprecation")
    @Override
    protected void observeEvents(boolean enable) {
        legacyAsyncObserver = observeEvent(legacyAsyncObserver, AsyncPlayerChatEvent.class, AsyncPlayerChatEvent::isCancelled, enable);
        legacySyncObserver = observeEvent(
                legacySyncObserver,
                org.bukkit.event.player.PlayerChatEvent.class,
                org.bukkit.event.player.PlayerChatEvent::isCancelled,
                enable
        );
    }
}
