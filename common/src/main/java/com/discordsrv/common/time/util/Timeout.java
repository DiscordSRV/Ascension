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
