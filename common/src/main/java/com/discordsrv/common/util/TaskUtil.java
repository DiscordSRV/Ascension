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

package com.discordsrv.common.util;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.util.function.CheckedRunnable;
import com.discordsrv.common.util.function.CheckedSupplier;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

public final class TaskUtil {

    @NotNull
    public static <T> Task<T> timeout(@NotNull DiscordSRV discordSRV, @NotNull Task<T> future, @NotNull Duration timeout) {
        ScheduledFuture<?> scheduledFuture = discordSRV.scheduler().runLater(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new TimeoutException());
            }
        }, timeout);
        return future.whenComplete((__, t) -> {
            if (t == null) {
                scheduledFuture.cancel(false);
            }
        });
    }

    @NotNull
    public static <T> Task<T> supplyAsync(@NotNull CheckedSupplier<T> supplier, @NotNull Executor executor) {
        Task<T> task = new Task<>();
        executor.execute(() -> {
            if (task.isCancelled()) {
                return;
            }
            try {
                task.complete(supplier.get());
            } catch (Throwable t) {
                task.completeExceptionally(t);
            }
        });
        return task;
    }

    @NotNull
    public static Task<Void> runAsync(@NotNull CheckedRunnable runnable, @NotNull Executor executor) {
        return supplyAsync(() -> {
            runnable.run();
            return null;
        }, executor);
    }
}
