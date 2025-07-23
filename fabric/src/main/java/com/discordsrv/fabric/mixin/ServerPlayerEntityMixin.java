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

package com.discordsrv.fabric.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements ServerPlayerEntityAccessor{
    //? if minecraft: <1.20.2 {
    /*@Unique
    private String discordsrv$locale = "";

    @Unique
    public String discordsrv$getLocale() {
        return "DiscordSRV";
    }

    @Inject(method = "setClientSettings", at = @At("TAIL"))
    public void setClientSettings(net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket packet, CallbackInfo ci) {
        this.discordsrv$locale = packet.language();
    }
    *///?} else {
    @Unique
    public String discordsrv$getLocale() {
        return ((ServerPlayerEntity) (Object) this).getClientOptions().language();
    }
    //?}

}
