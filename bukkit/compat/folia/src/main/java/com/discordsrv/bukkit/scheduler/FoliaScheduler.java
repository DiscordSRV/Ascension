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

package com.discordsrv.bukkit.scheduler;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class FoliaScheduler extends BukkitScheduler {

    public FoliaScheduler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void runOnMainThread(CommandSender sender, Runnable task) {
        if (sender instanceof ProxiedCommandSender) {
            runOnMainThread(((ProxiedCommandSender) sender).getCallee(), task);
            return;
        }

        Plugin plugin = discordSRV.plugin();
        if (sender instanceof Entity) {
            ((Entity) sender).getScheduler().run(
                    plugin,
                    r -> task.run(),
                    null
            );
        } else if (sender instanceof BlockCommandSender) {
            discordSRV.server().getRegionScheduler().run(
                    plugin,
                    ((BlockCommandSender) sender).getBlock().getLocation(),
                    r -> task.run()
            );
        } else {
            runOnMainThread(task);
        }
    }
}
