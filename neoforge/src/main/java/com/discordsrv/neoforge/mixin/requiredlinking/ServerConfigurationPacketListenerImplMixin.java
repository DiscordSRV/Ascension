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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public class ServerConfigurationPacketListenerImplMixin {

    @Inject(method = "startConfiguration", at = @At("HEAD"))
    private void onClientReady(CallbackInfo ci) {
        MixinUtils.withClass("com.discordsrv.modded.requiredlinking.ModdedRequiredLinkingModule")
                .withInstance()
                .withMethod("onPlayerPreLogin", (ServerConfigurationPacketListenerImpl) (Object) this)
                .execute();
    }

    @ModifyArg(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V"))
    private ServerPlayer onPlayerCreate(ServerPlayer serverPlayer) {
        MixinUtils
                .withClass("com.discordsrv.modded.player.ModdedPlayerProvider")
                .withInstance()
                .withMethod("addPlayer", serverPlayer)
                .execute();

        return serverPlayer;
    }
}
