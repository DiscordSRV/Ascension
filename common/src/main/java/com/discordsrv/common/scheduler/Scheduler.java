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

package com.discordsrv.common.scheduler;

import org.jetbrains.annotations.ApiStatus;
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
    Future<?> run(@NotNull Runnable task);

    /**
     * Schedules the given task after the provided amount of milliseconds.
     *
     * @param task the task
     * @param delay the delay before executing the task
     */
    ScheduledFuture<?> runLater(Runnable task, Duration delay);

    /**
     * Schedules the given task at the given rate.
     *
     * @param task the task
     * @param rate the rate in the given unit
     */
    @ApiStatus.NonExtendable
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
    @ApiStatus.NonExtendable
    ScheduledFuture<?> runAtFixedRate(@NotNull Runnable task, Duration initialDelay, Duration rate);

}
