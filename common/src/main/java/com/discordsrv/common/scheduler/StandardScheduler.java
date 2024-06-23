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

import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.scheduler.executor.DynamicCachingThreadPoolExecutor;
import com.discordsrv.common.scheduler.threadfactory.CountingForkJoinWorkerThreadFactory;
import com.discordsrv.common.scheduler.threadfactory.CountingThreadFactory;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.*;

public class StandardScheduler implements Scheduler {

    private final DiscordSRV discordSRV;
    private final ThreadPoolExecutor executorService;
    private final ScheduledThreadPoolExecutor scheduledExecutorService;
    private final ForkJoinPool forkJoinPool;
    private final ExceptionHandlingExecutor executor = new ExceptionHandlingExecutor();

    public StandardScheduler(DiscordSRV discordSRV) {
        this(
                discordSRV,
                new DynamicCachingThreadPoolExecutor(
                        /* Core pool size */
                        1,
                        /* Max pool size: cpu cores - 2 or at least 4 */
                        Math.max(4, Runtime.getRuntime().availableProcessors() - 2),
                        /* Timeout */
                        60, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(),
                        new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "Executor #%s")
                ),
                new ScheduledThreadPoolExecutor(
                        1, /* Core pool size */
                        new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "Scheduled Executor #%s")
                ),
                new ForkJoinPool(
                        /* parallelism */
                        Math.min(
                                /* max of 10 */
                                10,
                                /* cpu cores / 2 or at least 1 */
                                Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
                        ),
                        new CountingForkJoinWorkerThreadFactory(Scheduler.THREAD_NAME_PREFIX + "ForkJoinPool Worker #%s"),
                        null,
                        false /* async mode -> FIFO */
                )
        );
    }

    private StandardScheduler(
            DiscordSRV discordSRV,
            ThreadPoolExecutor executorService,
            ScheduledThreadPoolExecutor scheduledExecutorService,
            ForkJoinPool forkJoinPool
    ) {
        this.discordSRV = discordSRV;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.forkJoinPool = forkJoinPool;
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onShuttingDown(DiscordSRVShuttingDownEvent event) {
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
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
    public Executor executor() {
        return executor;
    }

    @Override
    public ExecutorService executorService() {
        return executorService;
    }

    @Override
    public ScheduledExecutorService scheduledExecutorService() {
        return scheduledExecutorService;
    }

    @Override
    public ForkJoinPool forkJoinPool() {
        return forkJoinPool;
    }

    @Override
    public @NotNull Future<?> run(@NotNull Runnable task) {
        return executorService.submit(wrap(task));
    }

    @Override
    public @NotNull ScheduledFuture<?> runLater(@NotNull Runnable task, @NotNull Duration delay) {
        return scheduledExecutorService.schedule(wrap(task), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public @NotNull ScheduledFuture<?> runAtFixedRate(@NotNull Runnable task, Duration initialDelay, Duration rate) {
        return scheduledExecutorService.scheduleAtFixedRate(wrap(task), initialDelay.toMillis(), rate.toMillis(), TimeUnit.MILLISECONDS);
    }

    public class ExceptionHandlingExecutor implements Executor {

        @Override
        public void execute(@NotNull Runnable command) {
            run(command);
        }
    }
}
