/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.debug;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.listener.AbstractBukkitListener;
import com.discordsrv.common.core.logging.NamedLogger;
import io.papermc.paper.event.player.AbstractChatEvent;
import io.papermc.paper.event.player.ChatEvent;
import org.bukkit.event.Event;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class PaperLegacyChatDebugModule extends AbstractBukkitListener<Event> {

    public PaperLegacyChatDebugModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "PAPER_LEGACY_CHAT_EVENT_OBSERVATION"));
    }

    // Dummy "listener", not actually listening to anything
    @Override
    public void enable() {}

    @Override
    public void disable() {}

    @Override
    protected void handleEvent(@NonNull Event event, Void __) {}

    private EventObserver<io.papermc.paper.event.player.ChatEvent, Boolean> modernSyncObserver;
    private EventObserver<org.bukkit.event.player.AsyncPlayerChatEvent, Boolean> legacyAsyncObserver;
    private EventObserver<org.bukkit.event.player.PlayerChatEvent, Boolean> legacySyncObserver;

    @Override
    protected void observeEvents(boolean enable) {
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

    @Override
    protected void collectRelevantHandlerLists(Consumer<Class<?>> eventClassConsumer) {
        eventClassConsumer.accept(ChatEvent.class);
        eventClassConsumer.accept(AsyncPlayerChatEvent.class);
        eventClassConsumer.accept(PlayerChatEvent.class);
    }
}
