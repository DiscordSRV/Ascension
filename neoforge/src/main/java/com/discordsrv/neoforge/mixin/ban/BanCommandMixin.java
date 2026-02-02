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

package com.discordsrv.neoforge.mixin.ban;

import com.discordsrv.neoforge.util.MixinUtils;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.players.UserBanListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BanPlayerCommands.class)
public class BanCommandMixin {

//    @Definition(id = "UserBanListEntry", type = UserBanListEntry.class)
//    @Definition(id = "entry", local = @Local(name = "gameProfile"))
//    @Expression("new UserBanListEntry(entry, ?, ?, ?, ?)")
//    @ModifyArg(method = "banPlayers", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
//    private static GameProfile ban(GameProfile entry) {
//        MixinUtils.withClass("com.discordsrv.modded.module.ban.ModdedBanModule")
//                .withMethod("onPardon", entry)
//                .execute();
//        return entry;
//    }
}
