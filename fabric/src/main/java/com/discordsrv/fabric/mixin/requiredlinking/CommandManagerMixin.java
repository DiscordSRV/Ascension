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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//?if minecraft: <1.19.2 {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.brigadier.ParseResults;
//?}

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    //?if minecraft: <1.19.2 {
    /*@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void execute(ServerCommandSource commandSource, String command, CallbackInfoReturnable<Integer> cir) {
        FabricRequiredLinkingModule.onCommandExecute(commandSource, command, cir);
        if(cir.isCancelled()) cir.setReturnValue(0);
    }
    *///?} else {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void execute(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        FabricRequiredLinkingModule.onCommandExecute(parseResults, command, ci);
    }
    //?}
}
