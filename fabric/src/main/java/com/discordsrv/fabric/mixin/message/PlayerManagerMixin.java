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

package com.discordsrv.fabric.mixin.message;

import com.discordsrv.fabric.module.chat.FabricChatModule;
import com.discordsrv.fabric.requiredlinking.FabricRequiredLinkingModule;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Function;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    //? if minecraft: <1.19 {
    /*@Inject(
            //? if minecraft: <1.18 {
            /^method = "Lnet/minecraft/server/PlayerManager;broadcastChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V",
            ^///?} else {
            method = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V",
            //?}
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSendChatMessage(Text message, net.minecraft.network.MessageType type, UUID senderUuid, CallbackInfo ci) {
        if (!(FabricRequiredLinkingModule.allowChatMessage(senderUuid))) {
            ci.cancel();
        } else {
            FabricChatModule.onChatMessage(message, senderUuid);
        }
    }
    *///?} else if minecraft: <1.19.2 {
    /*@Inject(
            method = {"broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Function;Lnet/minecraft/network/message/MessageSender;Lnet/minecraft/util/registry/RegistryKey;)V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void onSendChatMessage(net.minecraft.network.message.SignedMessage message, Function<ServerPlayerEntity, net.minecraft.network.message.SignedMessage> playerMessageFactory, net.minecraft.network.message.MessageSender sender, net.minecraft.util.registry.RegistryKey<net.minecraft.network.message.MessageType> typeKey, CallbackInfo ci) {
        if (!(FabricRequiredLinkingModule.allowChatMessage(sender.uuid()))) {
            ci.cancel();
        } else {
            FabricChatModule.onChatMessage(message.getContent(), sender.uuid());
        }
    }
    *///?} else if minecraft: >=1.19.2 {
    // Use fabric message api
    static {
        net.fabricmc.fabric.api.message.v1.ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, type) -> FabricRequiredLinkingModule.allowChatMessage(player.getUuid()));
        net.fabricmc.fabric.api.message.v1.ServerMessageEvents.CHAT_MESSAGE.register((message, player, type) -> FabricChatModule.onChatMessage(message.getContent(), player.getUuid()));
    }
    //?}
}
