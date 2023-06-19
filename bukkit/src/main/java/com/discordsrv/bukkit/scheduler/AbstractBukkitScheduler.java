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

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.DiscordSRVBukkitBootstrap;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.scheduler.ServerScheduler;
import com.discordsrv.common.scheduler.StandardScheduler;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;

public abstract class AbstractBukkitScheduler extends StandardScheduler implements ServerScheduler, IBukkitScheduler {

    protected final BukkitDiscordSRV discordSRV;

    public AbstractBukkitScheduler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    protected void checkDisable(Runnable task, BiConsumer<Server, Plugin> runNormal) {
        // Can't run tasks when disabling, so we'll push those to the bootstrap to run after disable
        if (!discordSRV.plugin().isEnabled() && discordSRV.status() == DiscordSRV.Status.SHUTTING_DOWN) {
            ((DiscordSRVBukkitBootstrap) discordSRV.bootstrap()).mainThreadTasksForDisable().add(task);
            return;
        }

        runWithArgs(runNormal);
    }

    @Override
    public void runWithArgs(BiConsumer<Server, Plugin> runNormal) {
        runNormal.accept(discordSRV.server(), discordSRV.plugin());
    }
}
