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

package com.discordsrv.common.scheduler;

import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.scheduler.threadfactory.CountingForkJoinWorkerThreadFactory;
import com.discordsrv.common.scheduler.threadfactory.CountingThreadFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public class StandardScheduler implements Scheduler {

    private final DiscordSRV discordSRV;
    private final ThreadPoolExecutor executor;
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private final ForkJoinPool forkJoinPool;

    public StandardScheduler(DiscordSRV discordSRV) {
        this(
                discordSRV,
                new ThreadPoolExecutor(
                        4, /* Core pool size */
                        16, /* Max pool size */
                        60, TimeUnit.SECONDS, /* Timeout */
                        new SynchronousQueue<>(),
                        new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "Scheduled Executor #%s")
                ),
                new ScheduledThreadPoolExecutor(
                        2, /* Core pool size */
                        new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "Scheduled Executor #%s")
                ),
                new ForkJoinPool(
                        Math.max(1, Runtime.getRuntime().availableProcessors() - 1), /* Parallelism - not core pool size */
                        new CountingForkJoinWorkerThreadFactory(Scheduler.THREAD_NAME_PREFIX + "ForkJoinPool Worker #%s"),
                        null,
                        false /* FIFO */
                )
        );
    }

    private StandardScheduler(
            DiscordSRV discordSRV,
            ThreadPoolExecutor executor,
            ScheduledThreadPoolExecutor scheduledExecutor,
            ForkJoinPool forkJoinPool
    ) {
        this.discordSRV = discordSRV;
        this.executor = executor;
        this.scheduledExecutor = scheduledExecutor;
        this.forkJoinPool = forkJoinPool;
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onShuttingDown(DiscordSRVShuttingDownEvent event) {
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
        forkJoinPool.shutdownNow();
    }

    private Runnable wrap(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                discordSRV.logger().error(Thread.currentThread().getName() + " ran into an exception", t);
            }
        };
    }

    @Override
    public ExecutorService executor() {
        return executor;
    }

    @Override
    public ScheduledExecutorService scheduledExecutor() {
        return scheduledExecutor;
    }

    @Override
    public ForkJoinPool forkExecutor() {
        return forkJoinPool;
    }

    @Override
    public Future<?> run(@NotNull Runnable task) {
        return executor.submit(wrap(task));
    }

    @Override
    public ForkJoinTask<?> runFork(@NotNull Runnable task) {
        return forkJoinPool.submit(wrap(task));
    }

    @Override
    public ScheduledFuture<?> runLater(Runnable task, long timeMillis) {
        return scheduledExecutor.schedule(wrap(task), timeMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> runAtFixedRate(@NotNull Runnable task, long initialDelayMillis, long rateMillis) {
        return scheduledExecutor.scheduleAtFixedRate(wrap(task), initialDelayMillis, rateMillis, TimeUnit.MILLISECONDS);
    }

}
