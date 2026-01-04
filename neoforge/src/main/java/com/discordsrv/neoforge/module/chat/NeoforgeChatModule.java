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

package com.discordsrv.neoforge.module.chat;

import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.neoforge.NeoforgeDiscordSRV;
import com.discordsrv.neoforge.module.AbstractNeoforgeModule;
import net.kyori.adventure.text.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;

public class NeoforgeChatModule extends AbstractNeoforgeModule {

    private static NeoforgeChatModule instance;
    private final NeoforgeDiscordSRV discordSRV;

    public NeoforgeChatModule(NeoforgeDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
        instance = this;
    }

    @Override
    public void register() {
        NeoForge.EVENT_BUS.addListener(this::onChatMessage);

    }

    public void onChatMessage(ServerChatEvent event) {
        NeoforgeDiscordSRV discordSRV = instance.discordSRV;

        Component component = discordSRV.componentFactory().fromNative(event.getMessage());
        discordSRV.eventBus().publish(new GameChatMessagePreProcessEvent(
                null,
                discordSRV.playerProvider().player(event.getPlayer()),
                ComponentUtil.toAPI(component),
                new GlobalChannel(discordSRV),
                false
        ));
    }
}
