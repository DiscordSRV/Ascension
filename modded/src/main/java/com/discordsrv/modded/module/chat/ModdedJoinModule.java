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
import com.discordsrv.api.events.message.preprocess.game.JoinMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.module.AbstractModdedModule;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

public class ModdedJoinModule extends AbstractModdedModule {

    private final ModdedDiscordSRV discordSRV;

    public ModdedJoinModule(ModdedDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    public void register() {
        //? if fabric
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(this::onJoin);

        //? if neoforge
        //net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onJoin);
    }

    //? if fabric {
    private void onJoin(net.minecraft.server.network.ServerGamePacketListenerImpl trigger, net.fabricmc.fabric.api.networking.v1.PacketSender packetSender, MinecraftServer minecraftServer) {
        if (!enabled) return;
        ServerPlayer playerEntity = trigger.player;
    //? }
    //? if neoforge {
    /*private void onJoin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent trigger) {
        if (!enabled) return;

        ServerPlayer playerEntity = (ServerPlayer) trigger.getEntity();
    *///? }
        boolean firstJoin = playerEntity.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) == 0;

        MinecraftComponent component;
        if (discordSRV.getNameFromGameProfile(playerEntity.getGameProfile()).equalsIgnoreCase(playerEntity.getName().getString())) {
            component = discordSRV.componentFactory().toAPI(Component.translatable("multiplayer.player.joined", discordSRV.componentFactory().fromNative(playerEntity.getDisplayName())));
        } else {
            component = discordSRV.componentFactory().toAPI(Component.translatable(
                    "multiplayer.player.joined.renamed",
                    discordSRV.componentFactory().fromNative(playerEntity.getDisplayName()),
                    Component.text(discordSRV.getNameFromGameProfile(playerEntity.getGameProfile()))
            ));
        }

        DiscordSRVPlayer player = discordSRV.playerProvider().player(playerEntity);
        discordSRV.eventBus().publish(
                new JoinMessagePreProcessEvent(
                        trigger,
                        player,
                        component,
                        null,
                        firstJoin,
                        false,
                        component == null,
                        false
                )
        );
    }
}
