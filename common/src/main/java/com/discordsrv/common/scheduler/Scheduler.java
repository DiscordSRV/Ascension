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

package com.discordsrv.common.scheduler;

import com.discordsrv.common.function.CheckedRunnable;
import com.discordsrv.common.function.CheckedSupplier;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.*;

@SuppressWarnings({"UnusedReturnValue", "unused"}) // API
public interface Scheduler {

    /**
     * The thread name prefix for all DiscordSRV executors.
     */
    String THREAD_NAME_PREFIX = "DiscordSRV Async ";

    /**
     * An executor that will actually catch exceptions.
     * @return the {@link Executor}
     */
    Executor executor();

    /**
     * Returns the {@link ExecutorService} being used.
     * @return the {@link ExecutorService}
     */
    ExecutorService executorService();

    /**
     * Returns the {@link ScheduledExecutorService} being used.
     * @return the {@link ScheduledExecutorService}
     */
    ScheduledExecutorService scheduledExecutorService();

    /**
     * Returns the {@link ForkJoinPool} being used.
     * @return the {@link ForkJoinPool}
     */
    ForkJoinPool forkJoinPool();

    /**
     * Runs the provided task as soon as possible.
     *
     * @param task the task
     */
    @NotNull
    Future<?> run(@NotNull Runnable task);

    @NotNull
    default CompletableFuture<Void> execute(@NotNull CheckedRunnable task) {
        return CompletableFutureUtil.runAsync(task, this::run);
    }

    @NotNull
    default <T> CompletableFuture<T> supply(@NotNull CheckedSupplier<T> supplier) {
        return CompletableFutureUtil.supplyAsync(supplier, this::run);
    }

    /**
     * Schedules the given task after the provided amount of milliseconds.
     *
     * @param task the task
     * @param delay the delay before executing the task
     */
    @NotNull
    ScheduledFuture<?> runLater(@NotNull Runnable task, @NotNull Duration delay);

    @NotNull
    default CompletableFuture<Void> executeLater(@NotNull CheckedRunnable task, @NotNull Duration delay) {
        return CompletableFutureUtil.runAsync(task, t -> runLater(t, delay));
    }

    @NotNull
    default <T> CompletableFuture<T> supplyLater(@NotNull CheckedSupplier<T> supplier, @NotNull Duration delay) {
        return CompletableFutureUtil.supplyAsync(supplier, task -> runLater(task, delay));
    }

    /**
     * Schedules the given task at the given rate.
     *
     * @param task the task
     * @param rate the rate in the given unit
     */
    @NotNull
    default ScheduledFuture<?> runAtFixedRate(@NotNull Runnable task, Duration rate) {
        return runAtFixedRate(task, rate, rate);
    }

    /**
     * Schedules a task to run at the given rate after the initial delay.
     *
     * @param task the task
     * @param initialDelay the initial delay in the provided unit
     * @param rate the rate to run the task at in the given unit
     */
    @NotNull
    ScheduledFuture<?> runAtFixedRate(@NotNull Runnable task, Duration initialDelay, Duration rate);

}
