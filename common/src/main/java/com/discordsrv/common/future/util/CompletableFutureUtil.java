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

package com.discordsrv.common.future.util;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.function.CheckedRunnable;
import com.discordsrv.common.function.CheckedSupplier;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

public final class CompletableFutureUtil {

    private CompletableFutureUtil() {}

    /**
     * Same as {@link CompletableFuture#completedFuture(Object)} but for failing.
     */
    @NotNull
    public static <T> CompletableFuture<T> failed(@NotNull Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> CompletableFuture<List<T>> combine(@NotNull Collection<@NotNull CompletableFuture<T>> futures) {
        return combine(futures.toArray(new CompletableFuture[0]));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> CompletableFuture<List<T>> combineGeneric(@NotNull Collection<@NotNull CompletableFuture<? extends T>> futures) {
        return combine(futures.toArray(new CompletableFuture[0]));
    }

    @SafeVarargs
    @NotNull
    public static <T> CompletableFuture<List<T>> combine(@NotNull CompletableFuture<@NotNull T>... futures) {
        return CompletableFuture.allOf(futures).thenApply(v -> {
            List<T> results = new ArrayList<>();
            for (CompletableFuture<T> aFuture : futures) {
                results.add(aFuture.join());
            }
            return results;
        });
    }

    @NotNull
    public static <T> CompletableFuture<T> timeout(@NotNull DiscordSRV discordSRV, @NotNull CompletableFuture<T> future, @NotNull Duration timeout) {
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
    public static <T> CompletableFuture<T> supplyAsync(@NotNull CheckedSupplier<T> supplier, @NotNull Executor executor) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            if (future.isCancelled()) {
                return;
            }
            try {
                future.complete(supplier.get());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @NotNull
    public static CompletableFuture<Void> runAsync(@NotNull CheckedRunnable runnable, @NotNull Executor executor) {
        return supplyAsync(() -> {
            runnable.run();
            return null;
        }, executor);
    }
}
