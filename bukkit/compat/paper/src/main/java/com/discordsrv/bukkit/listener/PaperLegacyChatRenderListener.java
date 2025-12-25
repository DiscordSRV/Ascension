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
import com.discordsrv.api.events.message.render.game.GameChatRenderEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PaperLegacyChatRenderListener extends AbstractBukkitListener<AsyncChatEvent> {

    private static final PaperComponentHandle.Get<AsyncChatEvent> GET_MESSAGE_HANDLE
            = PaperComponentHandle.get(AsyncChatEvent.class, "message");
    private static final PaperComponentHandle.Set<AsyncChatEvent> SET_MESSAGE_HANDLE
            = PaperComponentHandle.set(AsyncChatEvent.class, "message");

    public PaperLegacyChatRenderListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHAT_RENDER_LISTENER"));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull AsyncChatEvent event, Void __) {
        Player bukkitPlayer = event.getPlayer();

        IPlayer player = discordSRV.playerProvider().player(bukkitPlayer);
        MinecraftComponent message = GET_MESSAGE_HANDLE.getAPI(event);

        GameChatRenderEvent annotateEvent = new GameChatRenderEvent(
                event,
                player,
                new GlobalChannel(discordSRV),
                message
        );

        discordSRV.eventBus().publish(annotateEvent);

        MinecraftComponent annotatedMessage = annotateEvent.getAnnotatedMessage();
        if (annotatedMessage != null) {
            SET_MESSAGE_HANDLE.call(event, annotatedMessage);
        }
    }

    // Already observed via normal chat listener
    @Override
    protected void observeEvents(boolean enable) {}

    @Override
    protected void collectRelevantHandlerLists(Consumer<Class<?>> eventClassConsumer) {
        eventClassConsumer.accept(AsyncChatEvent.class);
    }
}
