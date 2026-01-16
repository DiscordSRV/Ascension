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

package com.discordsrv.fabric.module.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.DeathMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

public class FabricDeathModule extends AbstractFabricModule {

    private static FabricDeathModule INSTANCE;

    public static void withInstance(Consumer<FabricDeathModule> consumer) {
        if (INSTANCE != null && INSTANCE.enabled) {
            consumer.accept(INSTANCE);
        }
    }

    private final FabricDiscordSRV discordSRV;

    public FabricDeathModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;

        INSTANCE = this;
    }

    public void onDeath(LivingEntity livingEntity, CombatTracker damageTracker) {
        if (!enabled || !(livingEntity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        //? if minecraft: >=1.21.9 {
        ServerLevel level = serverPlayer.level();
        //?} else if minecraft: >1.19.4 {
        /*ServerLevel level = (ServerLevel) serverPlayer.level();
        *///?} else {
        /*ServerLevel level = (ServerLevel) serverPlayer.getLevel();
        *///?}

        //? if minecraft: >=1.21.11 {
        boolean showDeathMessages = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SHOW_DEATH_MESSAGES);
        //?} else {
        /*boolean showDeathMessages = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES).get();
        *///?}

        if (!showDeathMessages) {
            logger().debug("Skipping displaying death message, disabled by gamerule");
            return;
        }

        MinecraftComponent minecraftComponent = discordSRV.componentFactory().toAPI(damageTracker.getDeathMessage());
        DiscordSRVPlayer player = discordSRV.playerProvider().player(serverPlayer);

        discordSRV.eventBus().publish(
                new DeathMessagePreProcessEvent(
                        damageTracker,
                        player,
                        minecraftComponent,
                        null,
                        false
                )
        );
    }
}
