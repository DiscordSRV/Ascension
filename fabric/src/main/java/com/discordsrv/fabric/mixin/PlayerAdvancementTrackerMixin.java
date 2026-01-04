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

package com.discordsrv.fabric.mixin;

import com.discordsrv.fabric.module.chat.FabricAdvancementModule;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayer player;

    //? if minecraft: <=1.19.3 {
    /*@Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerAdvancements;ensureVisibility(Lnet/minecraft/advancements/Advancement;)V"))
    public void onGrant(net.minecraft.advancements.Advancement advancementEntry, String criterionName, CallbackInfoReturnable<Boolean> cir) {
    *///?} else if minecraft: <=1.20.1 {
    /*@Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerAdvancements;markForVisibilityUpdate(Lnet/minecraft/advancements/Advancement;)V"))
    public void onGrant(net.minecraft.advancements.Advancement advancementEntry, String criterionName, CallbackInfoReturnable<Boolean> cir) {
    *///?} else {
    @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerAdvancements;markForVisibilityUpdate(Lnet/minecraft/advancements/AdvancementHolder;)V"))
    public void onGrant(net.minecraft.advancements.AdvancementHolder advancementEntry, String criterionName, CallbackInfoReturnable<Boolean> cir) {
    //?}
        FabricAdvancementModule.onGrant(advancementEntry, player);
    }
}
