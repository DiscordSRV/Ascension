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

package com.discordsrv.bungee.player;

import com.discordsrv.bungee.BungeeDiscordSRV;
import com.discordsrv.bungee.command.game.sender.BungeeCommandSender;
import com.discordsrv.bungee.component.util.BungeeComponentUtil;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class BungeePlayer extends BungeeCommandSender implements IPlayer {

    private final ProxiedPlayer player;
    private final Identity identity;

    public BungeePlayer(BungeeDiscordSRV discordSRV, ProxiedPlayer player) {
        super(discordSRV, player, () -> discordSRV.audiences().player(player));
        this.player = player;
        this.identity = Identity.identity(player.getUniqueId());
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @NotNull String username() {
        return commandSender.getName();
    }

    @Override
    public @Nullable Locale locale() {
        return player.getLocale();
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }

    @Override
    public @NotNull Component displayName() {
        return BungeeComponentUtil.fromLegacy(player.getDisplayName());
    }

}
