/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.common.function.CheckedSupplier;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.scheduler.ServerScheduler;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.CheckReturnValue;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface IBukkitScheduler extends ServerScheduler {

    void runWithArgs(BiConsumer<Server, Plugin> runNormal);

    default void runOnMainThread(CommandSender sender, Runnable task) {
        runOnMainThread(task);
    }

    @CheckReturnValue
    default CompletableFuture<Void> executeOnMainThread(CommandSender sender, Runnable runnable) {
        return CompletableFuture.runAsync(runnable, task -> runOnMainThread(sender, task));
    }

    @CheckReturnValue
    default <T> CompletableFuture<T> supplyOnMainThread(CommandSender sender, CheckedSupplier<T> supplier) {
        return CompletableFutureUtil.supplyAsync(supplier, task -> runOnMainThread(sender, task));
    }

}
