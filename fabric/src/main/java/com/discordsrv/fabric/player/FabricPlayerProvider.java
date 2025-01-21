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

package com.discordsrv.fabric.player;

import com.discordsrv.common.abstraction.player.provider.AbstractPlayerProvider;
import com.discordsrv.fabric.FabricDiscordSRV;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class FabricPlayerProvider extends AbstractPlayerProvider<FabricPlayer, FabricDiscordSRV> {
    private boolean enabled = false;

    public FabricPlayerProvider(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        // Register events here instead of in subscribe() to avoid duplicate registrations. Since there's no unregister method for events in Fabric, we need to make sure we only register once.
        ServerPlayConnectionEvents.JOIN.register(this::onConnection);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onDisconnect);
    }

    @Override
    public void subscribe() {
        enabled = true;
        if (discordSRV.getServer() == null || discordSRV.getServer().getPlayerManager() == null)
            return; // Server not started yet, So there's no players to add

        // Add players that are already connected
        for (ServerPlayerEntity player : discordSRV.getServer().getPlayerManager().getPlayerList()) {
            addPlayer(player, true);
        }
    }

    @Override
    public void unsubscribe() {
        enabled = false;
    }

    private void onConnection(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        addPlayer(serverPlayNetworkHandler.player, false);
    }

    private void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        removePlayer(serverPlayNetworkHandler.player);
    }

    private void addPlayer(ServerPlayerEntity player, boolean initial) {
        if (!enabled) return;
        addPlayer(player.getUuid(), new FabricPlayer(discordSRV, player), initial);
    }

    private void removePlayer(ServerPlayerEntity player) {
        if (!enabled) return;
        removePlayer(player.getUuid());
    }

    public FabricPlayer player(ServerPlayerEntity player) {
        FabricPlayer srvPlayer = player(player.getUuid());
        if (srvPlayer == null) {
            throw new IllegalStateException("Player not available");
        }
        return srvPlayer;
    }
}
