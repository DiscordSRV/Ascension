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

package com.discordsrv.velocity.player;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.velocity.VelocityDiscordSRV;
import com.discordsrv.velocity.command.game.sender.VelocityCommandSender;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class VelocityPlayer extends VelocityCommandSender implements IPlayer {

    private final Player player;

    public VelocityPlayer(VelocityDiscordSRV discordSRV, Player player) {
        super(discordSRV, player);
        this.player = player;
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @NotNull String username() {
        return player.getUsername();
    }

    @Override
    public @Nullable Locale locale() {
        return player.getPlayerSettings().getLocale();
    }

    @Override
    public @NotNull Identity identity() {
        return player.identity();
    }

    @Override
    public @NotNull Component displayName() {
        // Use Adventure's Pointer, otherwise username
        return player.getOrDefaultFrom(
                Identity.DISPLAY_NAME,
                () -> Component.text(player.getUsername())
        );
    }
}
