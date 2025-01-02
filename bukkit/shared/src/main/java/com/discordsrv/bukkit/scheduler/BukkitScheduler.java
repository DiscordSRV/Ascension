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

package com.discordsrv.bukkit.scheduler;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.scheduler.ServerScheduler;
import com.discordsrv.common.core.scheduler.StandardScheduler;
import com.discordsrv.common.util.CompletableFutureUtil;
import com.discordsrv.common.util.function.CheckedSupplier;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class BukkitScheduler extends StandardScheduler implements ServerScheduler {

    protected final BukkitDiscordSRV discordSRV;

    public BukkitScheduler(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    protected void checkDisable(Runnable task, BiConsumer<Server, Plugin> runNormal) {
        // Can't run tasks when disabling, so we'll push those to the bootstrap to run after disable
        if (!discordSRV.plugin().isEnabled() && discordSRV.status() == DiscordSRV.Status.SHUTTING_DOWN) {
            // TODO
            //((DiscordSRVBukkitBootstrap) discordSRV.bootstrap()).mainThreadTasksForDisable().add(task);
            return;
        }

        runNormal.accept(discordSRV.server(), discordSRV.plugin());
    }

    @Override
    public void runOnMainThread(@NotNull Runnable task) {
        checkDisable(task, (server, plugin) -> server.getScheduler().runTask(plugin, task));
    }

    @Override
    public void runOnMainThreadLaterInTicks(@NotNull Runnable task, int ticks) {
        checkDisable(task, (server, plugin) -> server.getScheduler().runTaskLater(plugin, task, ticks));
    }

    @Override
    public void runOnMainThreadAtFixedRateInTicks(@NotNull Runnable task, int initialTicks, int rateTicks) {
        checkDisable(task, (server, plugin) -> server.getScheduler().runTaskTimer(plugin, task, initialTicks, rateTicks));
    }

    public void runOnMainThread(CommandSender sender, Runnable task) {
        runOnMainThread(task);
    }

    @CheckReturnValue
    public CompletableFuture<Void> executeOnMainThread(CommandSender sender, Runnable runnable) {
        return CompletableFuture.runAsync(runnable, task -> runOnMainThread(sender, task));
    }

    @CheckReturnValue
    public <T> CompletableFuture<T> supplyOnMainThread(CommandSender sender, CheckedSupplier<T> supplier) {
        return CompletableFutureUtil.supplyAsync(supplier, task -> runOnMainThread(sender, task));
    }
}
