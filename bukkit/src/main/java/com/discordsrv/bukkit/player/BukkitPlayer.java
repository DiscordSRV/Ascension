/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.provider.model.SkinInfo;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class BukkitPlayer extends BukkitCommandSender implements IPlayer {

    private static final PaperComponentHandle<Player> DISPLAY_NAME_HANDLE = makeDisplayNameHandle();

    private static PaperComponentHandle<Player> makeDisplayNameHandle() {
        return new PaperComponentHandle<>(
                Player.class,
                "displayName",
                Player::getDisplayName
        );
    }

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

    @Override
    public CompletableFuture<Void> kick(Component component) {
        return discordSRV.scheduler().executeOnMainThread(player, () -> {
            if (PaperPlayer.isKickAvailable()) {
                PaperPlayer.kick(player, component);
            } else {
                player.kickPlayer(BukkitComponentSerializer.legacy().serialize(component));
            }
        });
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        return SpigotPlayer.getSkinInfo(player);
    }

    @Override
    public Locale locale() {
        return PaperPlayer.getLocale(player);
    }

    @Override
    public @NotNull Component displayName() {
        return ComponentUtil.fromAPI(DISPLAY_NAME_HANDLE.getComponent(player));
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }
}
