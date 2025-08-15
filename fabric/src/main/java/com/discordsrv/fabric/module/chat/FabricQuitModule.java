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

package com.discordsrv.fabric.module.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.LeaveMessagePreProcessEvent;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import com.discordsrv.fabric.player.FabricPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FabricQuitModule extends AbstractFabricModule {

    private final FabricDiscordSRV discordSRV;

    public FabricQuitModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    public void register() {
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        if (!enabled) return;

        ServerPlayerEntity player = serverPlayNetworkHandler.player;

        MinecraftComponent component = discordSRV.componentFactory().toAPI(Text.translatable("multiplayer.player.left", player.getDisplayName()).formatted(Formatting.YELLOW));
        discordSRV.eventBus().publish(
                new LeaveMessagePreProcessEvent(
                        serverPlayNetworkHandler,
                        new FabricPlayer(discordSRV, player),
                        component,
                        null,
                        false,
                        component == null,
                        false
                )
        );
    }
}
