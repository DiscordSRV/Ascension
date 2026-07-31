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

package com.discordsrv.bukkit.player;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

/**
 * Super-duper legacy audience with only sending messages in legacy supported.
 */
public class BukkitAudience implements Audience {

    private final BukkitDiscordSRV discordSRV;
    private final CommandSender commandSender;

    public BukkitAudience(BukkitDiscordSRV discordSRV, CommandSender commandSender) {
        this.discordSRV = discordSRV;
        this.commandSender = commandSender;
    }

    @Override
    public void sendMessage(@NonNull Component message) {
        commandSender.sendMessage(discordSRV.componentFactory().legacySerializer().serialize(message));
    }
}
