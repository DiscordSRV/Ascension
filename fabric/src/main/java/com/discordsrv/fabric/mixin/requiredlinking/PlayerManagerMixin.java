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

package com.discordsrv.fabric.mixin.requiredlinking;

import com.discordsrv.fabric.requiredlinking.FabricRequiredLinkingModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(net.minecraft.server.PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "checkCanJoin", at = @At("TAIL"), cancellable = true)
    //? if minecraft: >= 1.21.9 {
    public void checkCanJoin(SocketAddress address, net.minecraft.server.PlayerConfigEntry entry, CallbackInfoReturnable<Text> cir) {
    //?} else {
    /*public void checkCanJoin(SocketAddress address, GameProfile entry, CallbackInfoReturnable<Text> cir) {
    *///?}
        Text kickReason = FabricRequiredLinkingModule.canJoin(entry);

        cir.setReturnValue(kickReason);
    }
}
