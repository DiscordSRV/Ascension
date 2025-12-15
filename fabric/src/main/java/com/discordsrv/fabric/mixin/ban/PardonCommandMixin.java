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

package com.discordsrv.fabric.mixin.ban;

import com.discordsrv.fabric.module.ban.FabricBanModule;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PardonCommand;

@Mixin(PardonCommand.class)
public class PardonCommandMixin {

    //? if minecraft: >= 1.21.9 {
    @Inject(method = "pardonPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/UserBanList;remove(Lnet/minecraft/server/players/NameAndId;)Z"))
    private static void pardon(CommandSourceStack source, Collection<net.minecraft.server.players.NameAndId> targets, CallbackInfoReturnable<Integer> cir, @Local net.minecraft.server.players.NameAndId entry) {
    //?} else {
    /*@Inject(method = "pardonPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/UserBanList;remove(Ljava/lang/Object;)V"))
    private static void pardon(CommandSourceStack source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir, @Local GameProfile entry) {
    *///?}
        FabricBanModule.onPardon(entry);
    }
}
