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

package com.discordsrv.bungee.module;

import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.bungee.listener.AbstractBungeeListener;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.common.util.ComponentUtil;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.event.EventHandler;
import org.jspecify.annotations.NonNull;

public class BungeeChatModule extends AbstractBungeeListener<ChatEvent> {

    public BungeeChatModule(BungeeDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHAT_MODULE"));
    }

    @EventHandler()
    public void onPostLogin(ChatEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NonNull ChatEvent event, Void __) {
        if (event.isCommand()) return;

        discordSRV.eventBus().publish(new GameChatMessagePreProcessEvent(
                event,
                discordSRV.playerProvider().player(event.getSender()),
                ComponentUtil.fromPlain(event.getMessage()),
                new GlobalChannel(discordSRV),
                event.isCancelled()
        ));
    }
}
