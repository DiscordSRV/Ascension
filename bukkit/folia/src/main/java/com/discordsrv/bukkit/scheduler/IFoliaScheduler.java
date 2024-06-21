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

package com.discordsrv.bukkit.scheduler;

import com.discordsrv.common.scheduler.ServerScheduler;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface IFoliaScheduler extends ServerScheduler, IBukkitScheduler {

    @Override
    default void runOnMainThread(CommandSender sender, Runnable task) {
        if (sender instanceof ProxiedCommandSender) {
            runOnMainThread(((ProxiedCommandSender) sender).getCallee(), task);
            return;
        }

        if (sender instanceof Entity) {
            runWithArgs((server, plugin) -> ((Entity) sender).getScheduler().run(
                    plugin,
                    r -> task.run(),
                    null
            ));
        } else if (sender instanceof BlockCommandSender) {
            runWithArgs((server, plugin) -> server.getRegionScheduler().run(
                    plugin,
                    ((BlockCommandSender) sender).getBlock().getLocation(),
                    r -> task.run()
            ));
        } else {
            runOnMainThread(task);
        }
    }

    @Override
    default void runOnMainThread(@NotNull Runnable task) {
        runWithArgs((server, plugin) -> server.getGlobalRegionScheduler().execute(plugin, task));
    }

    @Override
    default void runOnMainThreadLaterInTicks(@NotNull Runnable task, int ticks) {
        runWithArgs((server, plugin) -> server.getGlobalRegionScheduler().runDelayed(plugin, r -> task.run(), ticks));
    }

    @Override
    default void runOnMainThreadAtFixedRateInTicks(@NotNull Runnable task, int initialTicks, int rateTicks) {
        runWithArgs((server, plugin) -> server.getGlobalRegionScheduler().execute(plugin, task));
    }
}
