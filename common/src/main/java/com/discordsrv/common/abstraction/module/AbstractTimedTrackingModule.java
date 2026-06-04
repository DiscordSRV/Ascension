/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.abstraction.module;

import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Base module abstraction for features that run a single repeating timed task.
 *
 * <p>Current behavior:</p>
 * <ul>
 *   <li>On {@link #enable()}, runs {@link #onTimedEnable()} and then (re)starts the timed task.</li>
 *   <li>On {@link #reload(Consumer)}, only restarts the timed task (does not call enable/disable hooks).</li>
 *   <li>On {@link #disable()}, cancels the timed task and then runs {@link #onTimedDisable()}.</li>
 *   <li>Only one scheduled future is tracked at a time; restarting always cancels the previous one first.</li>
 * </ul>
 */
public abstract class AbstractTimedTrackingModule extends AbstractModule<DiscordSRV> {

    /**
     * The currently active scheduled repeating task, or {@code null} when not scheduled.
     */
    private Future<?> timedFuture;

    protected AbstractTimedTrackingModule(DiscordSRV discordSRV, NamedLogger logger) {
        super(discordSRV, logger);
    }

    @Override
    public void enable() {
        restartTimedTask();
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        restartTimedTask();
    }

    @Override
    public void disable() {
        stopTimedTask();
    }

    protected final void restartTimedTask() {
        stopTimedTask();

        if (!shouldRunTimedTask()) {
            return;
        }

        if (getMaximumInterval() != null && getMaximumInterval().minus(getMinimumInterval()).toMillis() < 0) {
            throw new IllegalStateException("Maximum timed task interval must be greater than or equal to the minimum interval, got: " + getMaximumInterval() + " < " + getMinimumInterval());
        }

        if (getMinimumInterval().toMillis() <= 0) {
            throw new IllegalStateException("Minimum timed task interval must be positive, got: " + getMinimumInterval());
        }

        Duration interval = timedTaskInterval();
        if (interval.minus(getMinimumInterval()).toMillis() < 0) {
            interval = getMinimumInterval();
            discordSRV.logger().debug("Timed task interval is below the minimum, adjusting to minimum: " + getMinimumInterval());
        } else if (getMaximumInterval() != null && interval.minus(getMaximumInterval()).toMillis() > 0) {
            interval = getMaximumInterval();
            discordSRV.logger().debug("Timed task interval is above the maximum, adjusting to maximum: " + getMaximumInterval());
        }

        // Schedules fixed-rate execution with initial delay equal to the configured interval.
        timedFuture = discordSRV.scheduler().runAtFixedRate(this::runTimedTask, interval);
    }

    protected final void stopTimedTask() {
        if (timedFuture != null) {
            timedFuture.cancel(true);
            timedFuture = null;
        }
    }

    protected void onTimedEnable() {}

    protected void onTimedDisable() {}

    /**
     * Determines whether the timed task should be scheduled at this moment.
     *
     * @return true to schedule; false to skip scheduling
     */
    protected abstract boolean shouldRunTimedTask();

    /**
     * Gets the minimum allowed interval for the timed task.
     * This will be used as a lower bound for {@link #timedTaskInterval()}.
     *
     * @throws IllegalStateException if the returned interval is not positive or if it's higher than {@link #getMaximumInterval()}.
     */
    protected abstract Duration getMinimumInterval();

    /**
     * Gets the maximal allowed interval for the timer task.
     * This will be used as an upper bound for {@link #timedTaskInterval()}.
     * If it returns null it will be treated as if there's no upper bound.
     *
     * @throws IllegalStateException if the returned interval is not positive (unless it's null) or if it's lower than {@link #getMinimumInterval()}.
     */
    protected Duration getMaximumInterval() {
        return null;
    }

    /**
     * Gets the user defined interval for the timed task.
     * If the interval is out of bounds with regard to {@link #getMinimumInterval()} and {@link #getMaximumInterval()}, it will be adjusted to fit within those bounds.
     */
    @NotNull
    protected abstract Duration timedTaskInterval();

    /**
     * Timed task invoked by the scheduler on each interval.
     */
    protected abstract void runTimedTask();
}