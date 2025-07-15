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
import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.bukkit.debug.EventObserver;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import io.papermc.paper.event.player.AbstractChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

public class PaperChatListener extends AbstractBukkitListener<AsyncChatEvent> {

    private static final PaperComponentHandle.Get<AsyncChatEvent> MESSAGE_HANDLE
            = PaperComponentHandle.get(AsyncChatEvent.class, "message");

    public PaperChatListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHAT_LISTENER"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncChat(AsyncChatEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull AsyncChatEvent event, Void __) {
        MinecraftComponent component = MESSAGE_HANDLE.getAPI(event);

        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        discordSRV.eventBus().publish(
                new GameChatMessagePreProcessEvent(
                        event,
                        player,
                        component,
                        new GlobalChannel(discordSRV),
                        event.isCancelled()
                )
        );
    }

    private EventObserver<AsyncChatEvent, Boolean> modernAsyncObserver;
    @SuppressWarnings("deprecation")
    private EventObserver<io.papermc.paper.event.player.ChatEvent, Boolean> modernSyncObserver;
    @SuppressWarnings("deprecation")
    private EventObserver<org.bukkit.event.player.AsyncPlayerChatEvent, Boolean> legacyAsyncObserver;
    @SuppressWarnings("deprecation")
    private EventObserver<org.bukkit.event.player.PlayerChatEvent, Boolean> legacySyncObserver;

    @SuppressWarnings("deprecation")
    @Override
    public void observeEvents(boolean enable) {
        modernAsyncObserver = observeEvent(modernAsyncObserver, AsyncChatEvent.class, AbstractChatEvent::isCancelled, enable);
        modernSyncObserver = observeEvent(
                modernSyncObserver,
                io.papermc.paper.event.player.ChatEvent.class,
                AbstractChatEvent::isCancelled,
                enable
        );
        legacyAsyncObserver = observeEvent(
                legacyAsyncObserver,
                org.bukkit.event.player.AsyncPlayerChatEvent.class,
                org.bukkit.event.player.AsyncPlayerChatEvent::isCancelled,
                enable
        );
        legacySyncObserver = observeEvent(
                legacySyncObserver,
                org.bukkit.event.player.PlayerChatEvent.class,
                org.bukkit.event.player.PlayerChatEvent::isCancelled,
                enable
        );
    }
}
