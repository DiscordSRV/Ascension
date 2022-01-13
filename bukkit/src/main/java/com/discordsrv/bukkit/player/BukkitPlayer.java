/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.bukkit.component.util.PaperComponentUtil;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("NullableProblems") // BukkitOfflinePlayer nullability
public class BukkitPlayer extends BukkitOfflinePlayer implements IPlayer {

    private final Player player;
    private final Audience audience;

    public BukkitPlayer(BukkitDiscordSRV discordSRV, Player player) {
        super(discordSRV, player);
        this.player = player;
        this.audience = discordSRV.audiences().player(player);
    }

    @Override
    public void sendMessage(Identity identity, @NotNull Component message) {
        if (audience != null) {
            audience.sendMessage(
                    identity != null ? identity : Identity.nil(),
                    message);
        } else {
            player.sendMessage(BukkitComponentSerializer.legacy().serialize(message));
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void runCommand(String command) {
        discordSRV.scheduler().runOnMainThread(() ->
                discordSRV.server().dispatchCommand(player, command));
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @SuppressWarnings("deprecation") // Paper
    @Override
    public @NotNull Component displayName() {
        return ComponentUtil.fromAPI(
                PaperComponentUtil.getComponent(discordSRV, player, "displayName", Player::getDisplayName)
        );
    }
}
