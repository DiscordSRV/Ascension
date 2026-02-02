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
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.players.UserBanList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PardonCommand.class)
public class PardonCommandMixin {

    @Definition(id = "userBanList", local = @Local(type = UserBanList.class))
    @Expression("userBanList.?(?)")
    @ModifyArg(method = "pardonPlayers", at = @At(value = "MIXINEXTRAS:EXPRESSION", ordinal = 1))
    private static Object pardon(Object o) {
        MixinUtils.withClass("com.discordsrv.modded.module.ban.ModdedBanModule")
                .withMethod("onPardon", o)
                .execute();
        return o;
    }
}
