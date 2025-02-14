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

package com.discordsrv.common.core.scheduler;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.util.TaskUtil;
import com.discordsrv.common.util.function.CheckedRunnable;
import com.discordsrv.common.util.function.CheckedSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused") // API
public interface ServerScheduler extends Scheduler {

    int TICKS_PER_SECOND = 20;
    long MILLISECONDS_PER_TICK = (1000L / TICKS_PER_SECOND);

    static long timeToTicks(long time, TimeUnit unit) {
        return millisToTicks(unit.toMillis(time));
    }

    static long millisToTicks(long milliseconds) {
        return milliseconds / MILLISECONDS_PER_TICK;
    }

    static long ticksToMillis(long ticks) {
        return ticks * MILLISECONDS_PER_TICK;
    }

    /**
     * Runs the provided task on the server's main thread as soon as possible.
     * @param task the task
     */
    void runOnMainThread(@NotNull Runnable task);

    @NotNull
    default Task<Void> supplyOnMainThread(@NotNull CheckedRunnable task) {
        return TaskUtil.runAsync(task, this::runOnMainThread);
    }

    @NotNull
    default <T> Task<T> supplyOnMainThread(@NotNull CheckedSupplier<T> task) {
        return TaskUtil.supplyAsync(task, this::runOnMainThread);
    }

    /**
     * Runs the provided task in on the server's main thread in the provided amount of ticks.
     * @param task the task
     * @param ticks the time in ticks
     * @see #TICKS_PER_SECOND
     * @see #timeToTicks(long, TimeUnit)
     */
    void runOnMainThreadLaterInTicks(@NotNull Runnable task, int ticks);

    default @NotNull Task<Void> executeOnMainThreadLaterInTicks(@NotNull CheckedRunnable task, int ticks) {
        return TaskUtil.runAsync(task, t -> runOnMainThreadLaterInTicks(t, ticks));
    }

    default @NotNull <T> Task<T> supplyOnMainThreadLaterInTicks(@NotNull CheckedSupplier<T> task, int ticks) {
        return TaskUtil.supplyAsync(task, t -> runOnMainThreadLaterInTicks(t, ticks));
    }

    /**
     * Runs the task on the server's main thread continuously at provided rate in ticks.
     *
     * @param task the task
     * @param rateTicks the rate in ticks
     */
    default void runOnMainThreadAtFixedRateInTicks(@NotNull Runnable task, int rateTicks) {
        runOnMainThreadAtFixedRateInTicks(task, 0, rateTicks);
    }

    /**
     * Runs the task on the server's main thread continuously at provided rate in ticks after the initial delay in ticks.
     *
     * @param task the task
     * @param initialTicks the initial delay in ticks
     * @param rateTicks the rate in ticks
     */
    void runOnMainThreadAtFixedRateInTicks(@NotNull Runnable task, int initialTicks, int rateTicks);


}
