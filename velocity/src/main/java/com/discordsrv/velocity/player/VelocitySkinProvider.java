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

package com.discordsrv.velocity.player;

import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.provider.PlayerSkinProvider;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.abstraction.player.provider.model.Textures;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.SkinParts;
import com.velocitypowered.api.util.GameProfile;

public class VelocitySkinProvider extends AbstractModule<VelocityDiscordSRV> implements PlayerSkinProvider.Platform {

    public VelocitySkinProvider(VelocityDiscordSRV discordSRV, Logger logger) {
        super(discordSRV, logger);
    }

    @Override
    public SkinInfo getSkinForPlayer(IOfflinePlayer player) {
        if (!(player instanceof VelocityPlayer)) {
            return null;
        }

        Player proxy = ((VelocityPlayer) player).getProxyPlayer();
        for (GameProfile.Property property : proxy.getGameProfile().getProperties()) {
            if (!Textures.KEY.equals(property.getName())) {
                continue;
            }

            SkinParts skinParts = proxy.getPlayerSettings().getSkinParts();
            SkinInfo.Parts parts = new SkinInfo.Parts(
                    skinParts.hasCape(),
                    skinParts.hasJacket(),
                    skinParts.hasLeftSleeve(),
                    skinParts.hasRightSleeve(),
                    skinParts.hasLeftPants(),
                    skinParts.hasRightPants(),
                    skinParts.hasHat()
            );

            Textures textures = Textures.getFromBase64(player.discordSRV(), property.getValue());
            return textures.getSkinInfo(parts);
        }

        return null;
    }
}

