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

package com.discordsrv.neoforge.mixin.requiredlinking;

import com.discordsrv.neoforge.util.MixinUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;import com.mojang.authlib.GameProfile;import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Optional;

@Mixin(net.minecraft.server.players.PlayerList.class)
public class PlayerManagerMixin {

    @Inject(method = "canPlayerLogin", at = @At("TAIL"), cancellable=true)
    public void checkCanJoin(SocketAddress socketAddress, @Coerce Object entry, CallbackInfoReturnable<Component> cir) {
        Optional<Component> kickReason = MixinUtils.<Component>withClass("com.discordsrv.modded.requiredlinking.ModdedRequiredLinkingModule")
                .withInstance()
                .withMethod("canJoin", entry)
                .execute();

        cir.setReturnValue(kickReason.orElse(null));
    }
}
