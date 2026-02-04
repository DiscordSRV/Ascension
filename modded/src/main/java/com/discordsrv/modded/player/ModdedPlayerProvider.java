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

import java.util.function.Consumer;

public class ModdedPlayerProvider extends AbstractPlayerProvider<ModdedPlayer, ModdedDiscordSRV> {

    private boolean enabled = false;
    private static ModdedPlayerProvider INSTANCE;

    public static void withInstance(Consumer<ModdedPlayerProvider> consumer) {
        if (INSTANCE != null && INSTANCE.enabled) {
            consumer.accept(INSTANCE);
        }
    }

    public ModdedPlayerProvider(ModdedDiscordSRV discordSRV) {
        super(discordSRV);
        INSTANCE = this;
    }

    @Override
    public void subscribe() {
        enabled = true;
    }

    @Override
    public void unsubscribe() {
        enabled = false;
    }

    public void addPlayer(ServerPlayer player, boolean initial) {
        if (!enabled) return;
        addPlayer(player.getUUID(), new ModdedPlayer(discordSRV, player), initial);
    }

    @SuppressWarnings("unused") // Used by Mixin
    public void removePlayer(ServerPlayer player) {
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
