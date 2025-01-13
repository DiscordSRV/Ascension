package com.discordsrv.fabric.mixin.ban;

import com.discordsrv.fabric.module.ban.FabricBanModule;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.dedicated.command.BanCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BanCommand.class)
public class BanCommandMixin {

    @Inject(method = "ban", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/BannedPlayerEntry;<init>(Lcom/mojang/authlib/GameProfile;Ljava/util/Date;Ljava/lang/String;Ljava/util/Date;Ljava/lang/String;)V"))
    private static void ban(CallbackInfoReturnable<Integer> cir, @Local GameProfile gameProfile) {
        FabricBanModule.onBan(gameProfile);
    }
}
