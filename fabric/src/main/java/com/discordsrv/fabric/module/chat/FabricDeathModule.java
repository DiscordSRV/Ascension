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
import com.discordsrv.api.events.message.receive.game.DeathMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class FabricDeathModule extends AbstractFabricModule {

    private final FabricDiscordSRV discordSRV;

    public FabricDeathModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    public void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onDeath);
    }

    private void onDeath(LivingEntity livingEntity, DamageSource damageSource) {
        if (!enabled) return;
        if (livingEntity instanceof ServerPlayerEntity) {
            Text message = damageSource.getDeathMessage(livingEntity);
            MinecraftComponent minecraftComponent = ComponentUtil.toAPI(discordSRV.getAdventure().asAdventure(message));

            DiscordSRVPlayer player = discordSRV.playerProvider().player((ServerPlayerEntity) livingEntity);
            discordSRV.eventBus().publish(
                    new DeathMessageReceiveEvent(
                            damageSource,
                            player,
                            minecraftComponent,
                            null,
                            false
                    )
            );
        }
    }
}
