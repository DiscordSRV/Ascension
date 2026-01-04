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

package com.discordsrv.neoforge.mixin;

import com.discordsrv.neoforge.accessor.ServerPlayerEntityAccessor;
import com.discordsrv.neoforge.module.chat.NeoforgeDeathModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin implements ServerPlayerEntityAccessor {
    @Unique
    public String discordsrv$getLocale() {
        return ((ServerPlayer) (Object) this).clientInformation().language();
    }

    @Unique
    public int discordsrv$getPlayerModelParts() {
        return ((ServerPlayer) (Object) this).clientInformation().modelCustomisation();
    }

    @WrapOperation(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recheckStatus()V"))
    public void onDeath(CombatTracker instance, Operation<Void> original) {
        NeoforgeDeathModule.withInstance(module -> module.onDeath(((ServerPlayer) (Object) this), instance));
        original.call(instance);
    }
}
