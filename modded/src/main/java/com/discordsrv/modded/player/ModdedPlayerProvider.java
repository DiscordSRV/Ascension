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

package com.discordsrv.modded.player;

import com.discordsrv.common.abstraction.player.provider.AbstractPlayerProvider;
import com.discordsrv.modded.ModdedDiscordSRV;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ModdedPlayerProvider extends AbstractPlayerProvider<ModdedPlayer, ModdedDiscordSRV> {

    private boolean enabled = false;

    public ModdedPlayerProvider(ModdedDiscordSRV discordSRV) {
        super(discordSRV);

        //? if fabric {
        // Register events here instead of in subscribe() to avoid duplicate registrations. Since there's no unregister method for events in Fabric, we need to make sure we only register once.
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(this::onConnection);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
        //?}

        //? if neoforge {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onConnection);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onDisconnect);
        *///?}
    }

    @Override
    public void subscribe() {
        enabled = true;
        if (discordSRV.getServer() == null || discordSRV.getServer().getPlayerList() == null) {
            return; // Server not started yet, So there's no players to add
        }

        // Add players that are already connected
        for (ServerPlayer player : discordSRV.getServer().getPlayerList().getPlayers()) {
            addPlayer(player, true);
        }
    }

    @Override
    public void unsubscribe() {
        enabled = false;
    }

    //? if fabric {
    private void onConnection(ServerGamePacketListenerImpl serverPlayNetworkHandler, net.fabricmc.fabric.api.networking.v1.PacketSender packetSender, MinecraftServer minecraftServer) {
        addPlayer(serverPlayNetworkHandler.player, false);
    }

    private void onDisconnect(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        removePlayer(serverPlayNetworkHandler.player);
    }
    //?}

    //? if neoforge {
    /*private void onConnection(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        addPlayer((ServerPlayer) event.getEntity(), false);
    }

    private void onDisconnect(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        removePlayer((ServerPlayer) event.getEntity());
    }
    *///?}

    private void addPlayer(ServerPlayer player, boolean initial) {
        if (!enabled) return;
        addPlayer(player.getUUID(), new ModdedPlayer(discordSRV, player), initial);
    }

    private void removePlayer(ServerPlayer player) {
        if (!enabled) return;
        removePlayer(player.getUUID());
    }

    public ModdedPlayer player(ServerPlayer player) {
        ModdedPlayer srvPlayer = player(player.getUUID());
        if (srvPlayer == null) {
            throw new IllegalStateException("Player not available");
        }
        return srvPlayer;
    }
}
