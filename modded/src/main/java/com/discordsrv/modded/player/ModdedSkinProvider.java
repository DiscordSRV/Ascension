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

package com.discordsrv.modded.player;

import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.provider.PlayerSkinProvider;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.accessor.ServerPlayerEntityAccessor;
import com.discordsrv.modded.module.AbstractModdedModule;

public class ModdedSkinProvider extends AbstractModdedModule implements PlayerSkinProvider.Platform {

    public ModdedSkinProvider(ModdedDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public SkinInfo getSkinForPlayer(IOfflinePlayer player) {
        if (!(player instanceof ModdedPlayer)) {
            return null;
        }

        int playerModelParts = ((ServerPlayerEntityAccessor) player).discordsrv$getPlayerModelParts();
        //? if minecraft: >1.20.2 {
        //? if minecraft: >=1.21.9 {
        com.mojang.authlib.minecraft.MinecraftProfileTextures textures = discordSRV.getServer().services().sessionService().getTextures(((ModdedPlayer) player).getPlayer().getGameProfile());
        //?} else {
        /*com.mojang.authlib.minecraft.MinecraftProfileTextures textures = discordSRV.getServer().getSessionService().getTextures(((ModdedPlayer) player).getPlayer().getGameProfile());
         *///?}
        if (!textures.equals(com.mojang.authlib.minecraft.MinecraftProfileTextures.EMPTY) && textures.skin() != null) {
            String model = textures.skin().getMetadata("model");
            if (model == null) model = "classic";

            return new SkinInfo(textures.skin().getHash(), model, new SkinInfo.Parts(playerModelParts));
        }
        //?} else {
        /*java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, com.mojang.authlib.minecraft.MinecraftProfileTexture> texturesMap = discordSRV.getServer().getSessionService().getTextures(((ModdedPlayer) player).getPlayer().getGameProfile(), false);
        com.mojang.authlib.minecraft.MinecraftProfileTexture skinTexture = texturesMap.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
        String model;
        if (skinTexture != null) {
            model = skinTexture.getMetadata("model");
            if (model == null) model = "classic";

            return new SkinInfo(skinTexture.getHash(), model, new SkinInfo.Parts(playerModelParts));
        }
        *///?}
        return null;
    }
}

