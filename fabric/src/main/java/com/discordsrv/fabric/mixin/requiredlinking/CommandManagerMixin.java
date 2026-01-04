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

package com.discordsrv.fabric.mixin.requiredlinking;

import com.discordsrv.fabric.requiredlinking.FabricRequiredLinkingModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(Commands.class)
public class CommandManagerMixin {

    //? if minecraft: <1.20.3 && >=1.19.1 {
    /*@Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void execute(com.mojang.brigadier.ParseResults<CommandSourceStack> parseResults, String command, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        FabricRequiredLinkingModule.withInstance(module -> module.onCommandExecute(parseResults, command, cir));
        if(cir.isCancelled()) cir.setReturnValue(0);
    }
    *///?} else {
    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void execute(com.mojang.brigadier.ParseResults<CommandSourceStack> parseResults, String command, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        FabricRequiredLinkingModule.withInstance(module -> module.onCommandExecute(parseResults, command, ci));
    }
    //?}
}
