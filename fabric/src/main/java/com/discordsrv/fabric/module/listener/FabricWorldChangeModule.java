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

package com.discordsrv.fabric.module.listener;

import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.events.player.PlayerChangedWorldEvent;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class FabricWorldChangeModule extends AbstractFabricModule {

    private final FabricDiscordSRV discordSRV;

    public FabricWorldChangeModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(this::onWorldChange);
    }

    private void onWorldChange(ServerPlayerEntity playerEntity, ServerWorld from, ServerWorld to) {
        IPlayer player = discordSRV.playerProvider().player(playerEntity);
        discordSRV.eventBus().publish(new PlayerChangedWorldEvent(player));
    }
}
