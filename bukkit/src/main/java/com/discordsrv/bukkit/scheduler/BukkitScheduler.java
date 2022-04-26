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

package com.discordsrv.bukkit.scheduler;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.DiscordSRVBukkitBootstrap;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.scheduler.StandardScheduler;
import com.discordsrv.common.server.scheduler.ServerScheduler;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;

public class BukkitScheduler extends StandardScheduler implements ServerScheduler {

    private final BukkitDiscordSRV discordSRV;

    public BukkitScheduler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    private void checkDisable(Runnable task, BiConsumer<org.bukkit.scheduler.BukkitScheduler, Plugin> runNormal) {
        // Can't run tasks when disabling, so we'll push those to the bootstrap to run after disable
        if (!discordSRV.plugin().isEnabled() && discordSRV.status() == DiscordSRV.Status.SHUTTING_DOWN) {
            ((DiscordSRVBukkitBootstrap) discordSRV.bootstrap()).mainThreadTasksForDisable().add(task);
            return;
        }

        runNormal.accept(discordSRV.server().getScheduler(), discordSRV.plugin());
    }

    @Override
    public void runOnMainThread(Runnable task) {
        checkDisable(task, (scheduler, plugin) -> scheduler.runTask(plugin, task));
    }

    @Override
    public void runOnMainThreadLaterInTicks(Runnable task, int ticks) {
        checkDisable(task, (scheduler, plugin) -> scheduler.runTaskLater(plugin, task, ticks));
    }

    @Override
    public void runOnMainThreadAtFixedRateInTicks(Runnable task, int initialTicks, int rateTicks) {
        checkDisable(task, (scheduler, plugin) -> scheduler.runTaskTimer(plugin, task, initialTicks, rateTicks));
    }
}
