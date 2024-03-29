/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.sponge.player;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.provider.model.SkinInfo;
import com.discordsrv.common.player.provider.model.Textures;
import com.discordsrv.sponge.SpongeDiscordSRV;
import com.discordsrv.sponge.command.game.sender.SpongeCommandSender;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Locale;

public class SpongePlayer extends SpongeCommandSender implements IPlayer {

    private final ServerPlayer player;
    private final Identity identity;

    public SpongePlayer(SpongeDiscordSRV discordSRV, ServerPlayer player) {
        super(discordSRV, () -> player, () -> player);
        this.player = player;
        this.identity = Identity.identity(player.uniqueId());
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @NotNull String username() {
        return player.name();
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        String texturesRaw = player.skinProfile().get().value();
        Textures textures = Textures.getFromBase64(discordSRV, texturesRaw);
        return textures.getSkinInfo();
    }

    @Override
    public @Nullable Locale locale() {
        return player.locale();
    }

    @Override
    public @NotNull Component displayName() {
        return player.displayName().get();
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }
}
