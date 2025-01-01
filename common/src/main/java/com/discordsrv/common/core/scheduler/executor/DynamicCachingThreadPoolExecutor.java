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

package com.discordsrv.common.core.scheduler.executor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

/**
 * A {@link ThreadPoolExecutor} that acts like a {@link Executors#newCachedThreadPool()} with a max pool size while allowing queueing tasks.
 */
public class DynamicCachingThreadPoolExecutor extends ThreadPoolExecutor {

    private int corePoolSize;

    public DynamicCachingThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            @NotNull TimeUnit unit,
            @NotNull BlockingQueue<Runnable> workQueue,
            @NotNull ThreadFactory threadFactory
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.corePoolSize = corePoolSize;
    }

    @Override
    public int getCorePoolSize() {
        return corePoolSize;
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        super.setCorePoolSize(this.corePoolSize);
    }

    @Override
    public synchronized void execute(@NotNull Runnable command) {
        super.setCorePoolSize(getMaximumPoolSize());
        super.execute(command);
        super.setCorePoolSize(this.corePoolSize);
    }
}
