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

package com.discordsrv.common.scheduler;

import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused") // API
public interface ServerScheduler extends Scheduler {

    int TICKS_PER_SECOND = 20;
    long MILLISECONDS_PER_TICK = (1000L / TICKS_PER_SECOND);

    @ApiStatus.NonExtendable
    default long timeToTicks(long time, TimeUnit unit) {
        return millisToTicks(unit.toMillis(time));
    }

    @ApiStatus.NonExtendable
    default long millisToTicks(long milliseconds) {
        return milliseconds / MILLISECONDS_PER_TICK;
    }

    @ApiStatus.NonExtendable
    default long ticksToMillis(long ticks) {
        return ticks * MILLISECONDS_PER_TICK;
    }

    /**
     * Runs the provided task on the server's main thread as soon as possible.
     * @param task the task
     */
    void runOnMainThread(Runnable task);

    /**
     * Runs the provided task in on the server's main thread in the provided amount of ticks.
     * @param task the task
     * @param ticks the time in ticks
     * @see #TICKS_PER_SECOND
     * @see #timeToTicks(long, TimeUnit)
     */
    void runOnMainThreadLaterInTicks(Runnable task, int ticks);

    /**
     * Runs the task on the server's main thread continuously at provided rate in ticks.
     *
     * @param task the task
     * @param rateTicks the rate in ticks
     */
    @ApiStatus.NonExtendable
    default void runOnMainThreadAtFixedRateInTicks(Runnable task, int rateTicks) {
        runOnMainThreadAtFixedRateInTicks(task, 0, rateTicks);
    }

    /**
     * Runs the task on the server's main thread continuously at provided rate in ticks after the initial delay in ticks.
     *
     * @param task the task
     * @param initialTicks the initial delay in ticks
     * @param rateTicks the rate in ticks
     */
    void runOnMainThreadAtFixedRateInTicks(Runnable task, int initialTicks, int rateTicks);


}
