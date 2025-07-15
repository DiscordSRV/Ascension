/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.task.Task;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.command.game.sender.BukkitCommandSender;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;

public abstract class BukkitPlayer extends BukkitCommandSender implements IPlayer {

    protected Player player;
    private final Identity identity;

    public BukkitPlayer(BukkitDiscordSRV discordSRV, Player player) {
        super(discordSRV, player, () -> discordSRV.audiences().player(player));
        this.player = player;
        this.identity = Identity.identity(player.getUniqueId());
    }

    protected void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public BukkitDiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @NotNull String username() {
        return player.getName();
    }

    @Override
    public Task<Void> kick(Component component) {
        String legacy = BukkitComponentSerializer.legacy().serialize(component);
        return discordSRV.scheduler().executeOnMainThread(player, () -> player.kickPlayer(legacy));
    }

    @Override
    public abstract void addChatSuggestions(Collection<String> suggestions);

    @Override
    public abstract void removeChatSuggestions(Collection<String> suggestions);

    @Override
    public abstract @Nullable SkinInfo skinInfo();

    @Override
    public abstract Locale locale();

    @Override
    public @NotNull String world() {
        return player.getWorld().getName();
    }

    @Override
    public boolean isVanished() {
        for (MetadataValue metadata : player.getMetadata("vanished")) {
            if (metadata.asBoolean()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Component displayName() {
        return BukkitComponentSerializer.legacy().deserialize(player.getDisplayName());
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }

    @Override
    public String toString() {
        return "BukkitPlayer{" + username() + "}";
    }
}
