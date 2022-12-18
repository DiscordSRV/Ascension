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

package com.discordsrv.sponge.scheduler;

import com.discordsrv.common.scheduler.ServerScheduler;
import com.discordsrv.common.scheduler.StandardScheduler;
import com.discordsrv.sponge.SpongeDiscordSRV;
import org.spongepowered.api.scheduler.TaskExecutorService;

import java.util.concurrent.TimeUnit;

public class SpongeScheduler extends StandardScheduler implements ServerScheduler {

    private final TaskExecutorService service;

    public SpongeScheduler(SpongeDiscordSRV discordSRV) {
        super(discordSRV);
        this.service = discordSRV.game().server().scheduler().executor(discordSRV.container());
    }

    @Override
    public void runOnMainThread(Runnable task) {
        service.submit(task);
    }

    @Override
    public void runOnMainThreadLaterInTicks(Runnable task, int ticks) {
        service.schedule(task, ticksToMillis(ticks), TimeUnit.MILLISECONDS);
    }

    @Override
    public void runOnMainThreadAtFixedRateInTicks(Runnable task, int initialTicks, int rateTicks) {
        service.scheduleAtFixedRate(task, ticksToMillis(initialTicks), ticksToMillis(rateTicks), TimeUnit.MILLISECONDS);
    }
}
