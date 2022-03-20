/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.player;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.command.game.sender.BukkitCommandSender;
import com.discordsrv.bukkit.component.util.PaperComponentUtil;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitPlayer extends BukkitCommandSender implements IPlayer {

    private final Player player;
    private final Identity identity;

    public BukkitPlayer(BukkitDiscordSRV discordSRV, Player player) {
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
        return player.getName();
    }

    @SuppressWarnings("deprecation") // Paper
    @Override
    public @NotNull Component displayName() {
        return ComponentUtil.fromAPI(
                PaperComponentUtil.getComponent(discordSRV, player, "displayName", Player::getDisplayName)
        );
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }
}
