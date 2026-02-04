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

import com.discordsrv.modded.module.chat.ModdedChatModule;
import com.discordsrv.modded.requiredlinking.ModdedRequiredLinkingModule;
import com.discordsrv.modded.util.MixinUtils;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerList.class)
public class PlayerMessageMixin {

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
        ModdedRequiredLinkingModule.withInstance(module -> {
            if (!module.allowChatMessage(senderUuid)) {
                ci.cancel();
            } else {
//                ModdedChatModule.onChatMessage(message, senderUuid);
            }
        });
    }
    */
    //?} else if minecraft: <1.19.2 {
    /*@Inject(
            method = {"broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Function;Lnet/minecraft/network/message/MessageSender;Lnet/minecraft/util/registry/RegistryKey;)V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void onSendChatMessage(net.minecraft.network.message.SignedMessage message, Function<ServerPlayerEntity, net.minecraft.network.message.SignedMessage> playerMessageFactory, net.minecraft.network.message.MessageSender sender, net.minecraft.util.registry.RegistryKey<net.minecraft.network.message.MessageType> typeKey, CallbackInfo ci) {
        ModdedRequiredLinkingModule.withInstance(module -> {
            if (!module.allowChatMessage(sender.uuid())) {
                ci.cancel();
            } else {
                ModdedChatModule.onChatMessage(message.getContent(), sender.uuid());
            }
        });
    }
    */
    //?} else if minecraft: >=1.19.2 {
    static {
        //? if fabric {
        ModdedRequiredLinkingModule.withInstance(module -> net.fabricmc.fabric.api.message.v1.ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, type) -> module.allowChatMessage(player.getUUID())));
        net.fabricmc.fabric.api.message.v1.ServerMessageEvents.CHAT_MESSAGE.register((message, player, type) -> ModdedChatModule.onChatMessage(message.signedContent(), player.getUUID()));
        //?}

        //? if neoforge {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.ServerChatEvent event) -> {
            boolean allowMessage = MixinUtils.withClass("com.discordsrv.modded.requiredlinking.ModdedRequiredLinkingModule", Boolean.class)
                    .withInstance()
                    .withMethod("allowChatMessage", event.getPlayer().getUUID())
                    .execute().orElse(false);
            if (!allowMessage || event.isCanceled()) event.setCanceled(true);
            else MixinUtils.withClass("com.discordsrv.modded.module.chat.ModdedChatModule")
                        .withInstance()
                        .withMethod("onChatMessage", event.getMessage(), event.getPlayer().getUUID())
                        .execute();
        });
        //?}
    }
    //?}
}
