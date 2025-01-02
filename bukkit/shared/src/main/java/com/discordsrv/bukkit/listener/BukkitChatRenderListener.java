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
import com.discordsrv.api.events.message.render.GameChatRenderEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class BukkitChatRenderListener extends AbstractBukkitListener<AsyncPlayerChatEvent> {

    public BukkitChatRenderListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHAT_RENDER_LISTENER"));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        handleEvent(event);
    }

    @Override
    protected void handleEvent(AsyncPlayerChatEvent event) {
        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        MinecraftComponent component = ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(event.getMessage()));

        GameChatRenderEvent annotateEvent = new GameChatRenderEvent(
                event,
                player,
                new GlobalChannel(discordSRV),
                component
        );

        discordSRV.eventBus().publish(annotateEvent);
        MinecraftComponent message = annotateEvent.getAnnotatedMessage();
        if (message != null) {
            event.setMessage(BukkitComponentSerializer.legacy().serialize(ComponentUtil.fromAPI(message)));
        }
    }

    // Not important
    @Override
    protected void observeEvents(boolean enable) {}
}
