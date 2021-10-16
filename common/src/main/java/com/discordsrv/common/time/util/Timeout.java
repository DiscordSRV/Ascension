/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.time.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper class to track of time since something was done last, to avoid repeating tasks within a short timespan.
 */
public class Timeout {

    private final AtomicLong last = new AtomicLong(0);
    private final long timeoutMS;

    public Timeout(long time, @NotNull TimeUnit unit) {
        this(unit.toMillis(time));
    }

    public Timeout(long timeoutMS) {
        this.timeoutMS = timeoutMS;
    }

    /**
     * Checks if the last invocation of this method was not within the timeout period,
     * if true updates the time to the current time.
     * @return if the last time this was invoked was outside the timeout period
     */
    public boolean checkAndUpdate() {
        long currentTime = System.currentTimeMillis();
        synchronized (last) {
            long time = last.get();
            if (time + timeoutMS < currentTime) {
                last.set(currentTime);
                return true;
            }
        }
        return false;
    }
}
