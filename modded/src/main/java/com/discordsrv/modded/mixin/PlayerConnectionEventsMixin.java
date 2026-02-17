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

package com.discordsrv.modded.mixin;

import com.discordsrv.modded.util.MixinUtils;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.server.players.PlayerList.class)
public class PlayerConnectionEventsMixin {

    @Definition(id = "serverPlayer", local = @Local(type = ServerPlayer.class))
    @Expression("serverPlayer")
    @ModifyExpressionValue(method = "placeNewPlayer", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 0)) // Target the first usage of the player inside the method.
    private ServerPlayer handlePlayerJoin(ServerPlayer player) {
        MixinUtils.withClass("com.discordsrv.modded.player.ModdedPlayerProvider")
                .withInstance()
                .withMethod("addPlayer", player, false)
                .execute();
        return player;
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void handlePlayerLeave(ServerPlayer player, CallbackInfo ci) {
        MixinUtils.withClass("com.discordsrv.modded.player.ModdedPlayerProvider")
                .withInstance()
                .withMethod("removePlayer", player, false)
                .execute();
    }
}
