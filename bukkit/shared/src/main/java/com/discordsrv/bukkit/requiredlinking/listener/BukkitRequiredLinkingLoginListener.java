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

package com.discordsrv.bukkit.requiredlinking.listener;

import com.discordsrv.bukkit.requiredlinking.BukkitRequiredLinkingModule;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BukkitRequiredLinkingLoginListener extends BukkitRequiredLinkingListener<PlayerLoginEvent> {

    public BukkitRequiredLinkingLoginListener(BukkitRequiredLinkingModule module, EventPriority priority) {
        super(module, PlayerLoginEvent.class, priority);
    }

    @Override
    public @Nullable String getAlreadyBlockedReason(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            return null;
        }
        return event.getKickMessage();
    }

    @Override
    public @NotNull Pair<UUID, String> getPlayer(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        return Pair.of(player.getUniqueId(), player.getName());
    }

    @Override
    public void blockLogin(PlayerLoginEvent event, Component reason) {
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, BukkitComponentSerializer.legacy().serialize(reason));
    }
}
