package com.discordsrv.common.scheduler.executor;

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
