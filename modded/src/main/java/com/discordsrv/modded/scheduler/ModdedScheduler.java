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

package com.discordsrv.modded.scheduler;

import com.discordsrv.common.core.scheduler.ServerScheduler;
import com.discordsrv.common.core.scheduler.StandardScheduler;
import com.discordsrv.modded.ModdedDiscordSRV;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class ModdedScheduler extends StandardScheduler implements ServerScheduler {

    private final ModdedDiscordSRV discordSRV;

    public ModdedScheduler(ModdedDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    @Override
    public void runOnMainThread(@NotNull Runnable task) {
        discordSRV.getServer().execute(task);
    }

    @Override
    public void runOnMainThreadLaterInTicks(@NotNull Runnable task, int ticks) {
        runLater(() -> runOnMainThread(task), Duration.ofMillis(ServerScheduler.ticksToMillis(ticks)));
    }

    @Override
    public void runOnMainThreadAtFixedRateInTicks(@NotNull Runnable task, int initialTicks, int rateTicks) {
        Duration initialDelay = Duration.ofMillis(ServerScheduler.ticksToMillis(initialTicks));
        Duration rate = Duration.ofMillis(ServerScheduler.ticksToMillis(rateTicks));
        runAtFixedRate(() -> runOnMainThread(task), initialDelay, rate);
    }

    @Override
    public boolean isServerThread() {
        return Thread.currentThread().getName().equals("Server thread");
    }
}
