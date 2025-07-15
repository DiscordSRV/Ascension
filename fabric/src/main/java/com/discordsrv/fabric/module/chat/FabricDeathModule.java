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
import com.discordsrv.api.events.message.preprocess.game.DeathMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;

public class FabricDeathModule extends AbstractFabricModule {

    private static FabricDeathModule instance;
    private final FabricDiscordSRV discordSRV;

    public FabricDeathModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
        instance = this;
    }

    public static void onDeath(LivingEntity livingEntity, DamageSource damageSource) {
        if (instance == null || !instance.enabled) return;
        if (!(livingEntity instanceof ServerPlayerEntity playerEntity)) {
            return;
        }
        FabricDiscordSRV discordSRV = instance.discordSRV;

//        if (!playerEntity.getServerWorld().getGameRules().get(GameRules.SHOW_DEATH_MESSAGES).get()) {
//            logger().debug("Skipping displaying death message, disabled by gamerule");
//            return;
//        }

        Text message = damageSource.getDeathMessage(livingEntity);
        //? if adventure: <6 {
            /*@SuppressWarnings("removal")
            Component component = discordSRV.getAdventure().toAdventure(message);
            *///?} else {
        Component component = discordSRV.getAdventure().asAdventure(message);
        //?}
        MinecraftComponent minecraftComponent = ComponentUtil.toAPI(component);

        DiscordSRVPlayer player = discordSRV.playerProvider().player(playerEntity);
        discordSRV.eventBus().publish(
                new DeathMessagePreProcessEvent(
                        damageSource,
                        player,
                        minecraftComponent,
                        null,
                        false
                )
        );
    }
}
