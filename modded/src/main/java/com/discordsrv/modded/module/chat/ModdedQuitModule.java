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

package com.discordsrv.modded.module.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.LeaveMessagePreProcessEvent;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.module.AbstractModdedModule;
import com.discordsrv.modded.player.ModdedPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ModdedQuitModule extends AbstractModdedModule {

    private final ModdedDiscordSRV discordSRV;

    public ModdedQuitModule(ModdedDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    public void register() {
        //? if fabric
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);

        //? if neoforge
        //net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onDisconnect);
    }

    //? if fabric {
    private void onDisconnect(net.minecraft.server.network.ServerGamePacketListenerImpl trigger, MinecraftServer minecraftServer) {
        if (!enabled) return;
        ServerPlayer player = trigger.player;
    //? }
    //? if neoforge {
    /*private void onDisconnect(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent trigger) {
        if (!enabled) return;

        ServerPlayer player = (ServerPlayer) trigger.getEntity();
    *///? }

        MinecraftComponent component = discordSRV.componentFactory().toAPI(net.minecraft.network.chat.Component.translatable("multiplayer.player.left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW));
        discordSRV.eventBus().publish(
                new LeaveMessagePreProcessEvent(
                        trigger,
                        new ModdedPlayer(discordSRV, player),
                        component,
                        null,
                        false,
                        component == null,
                        false
                )
        );
    }
}
