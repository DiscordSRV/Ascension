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

public class FoliaScheduler extends AbstractBukkitScheduler implements IFoliaScheduler {

    public FoliaScheduler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void runOnMainThread(Runnable task) {
        checkDisable(task, (server, plugin) -> IFoliaScheduler.super.runOnMainThread(task));
    }

    @Override
    public void runOnMainThreadLaterInTicks(Runnable task, int ticks) {
        checkDisable(task, (server, plugin) -> IFoliaScheduler.super.runOnMainThreadLaterInTicks(task, ticks));
    }

    @Override
    public void runOnMainThreadAtFixedRateInTicks(Runnable task, int initialTicks, int rateTicks) {
        checkDisable(task, (server, plugin) -> IFoliaScheduler.super.runOnMainThreadAtFixedRateInTicks(task, initialTicks, rateTicks));
    }
}
