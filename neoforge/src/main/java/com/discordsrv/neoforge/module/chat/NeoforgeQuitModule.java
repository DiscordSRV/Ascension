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

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.LeaveMessagePreProcessEvent;
import com.discordsrv.neoforge.NeoforgeDiscordSRV;
import com.discordsrv.neoforge.module.AbstractNeoforgeModule;
import com.discordsrv.neoforge.player.NeoforgePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class NeoforgeQuitModule extends AbstractNeoforgeModule {

    private final NeoforgeDiscordSRV discordSRV;

    public NeoforgeQuitModule(NeoforgeDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    public void register() {
        NeoForge.EVENT_BUS.addListener(this::onDisconnect);
    }

    private void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!enabled) return;

        MinecraftComponent component = discordSRV.componentFactory().toAPI(net.minecraft.network.chat.Component.translatable("multiplayer.player.left", event.getEntity().getDisplayName()).withStyle(ChatFormatting.YELLOW));
        discordSRV.eventBus().publish(
                new LeaveMessagePreProcessEvent(
                        event,
                        new NeoforgePlayer(discordSRV, (ServerPlayer) event.getEntity()),
                        component,
                        null,
                        false,
                        component == null,
                        false
                )
        );
    }
}
