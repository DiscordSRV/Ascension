package com.discordsrv.fabric.mixin;

import com.discordsrv.fabric.module.chat.FabricDeathModule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "net/minecraft/world/World.sendEntityStatus(Lnet/minecraft/entity/Entity;B)V"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        FabricDeathModule.onDeath((LivingEntity) (Object) this, source);
    }
}
