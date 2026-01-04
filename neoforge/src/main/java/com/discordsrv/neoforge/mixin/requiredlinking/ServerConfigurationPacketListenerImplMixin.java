package com.discordsrv.neoforge.mixin.requiredlinking;

import com.discordsrv.neoforge.requiredlinking.NeoforgeRequiredLinkingModule;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerConfigurationPacketListenerImpl.class, priority = 900)
public class ServerConfigurationPacketListenerImplMixin {

    @Inject(method = "startConfiguration", at = @At("HEAD"))
    private void onClientReady(CallbackInfo ci) {
        NeoforgeRequiredLinkingModule.withInstance(module -> module.onPlayerPreLogin((ServerConfigurationPacketListenerImpl) (Object) this));
    }
}
