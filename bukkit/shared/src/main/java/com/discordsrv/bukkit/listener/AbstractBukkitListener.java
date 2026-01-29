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

package com.discordsrv.bukkit.listener;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class to catch errors to our own event bus and debugging via {@link AbstractBukkitEventObserver}.
 * @param <E> the event type
 */
public abstract class AbstractBukkitListener<E extends Event> extends AbstractBukkitEventObserver implements Listener {

    public AbstractBukkitListener(BukkitDiscordSRV discordSRV, Logger logger) {
        super(discordSRV, logger);
    }

    @Override
    public void enable() {
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    /**
     * Passes events to {@link #handleEvent(Event, Void)}, and logs errors to our own loggers, which can be forwarded to our debug logs.
     * @param event the event from the handler
     */
    protected final void handleEventWithErrorHandling(@NotNull E event) {
        try {
            handleEvent(event, null);
        } catch (Throwable throwable) {
            logger().error("Failed to pass " + event.getClass().getName() + " to " + getClass().getName(), throwable);
        }
    }

    /**
     * The event handler, the event listener should pass all events to {@link #handleEventWithErrorHandling(Event)} which will pass them here.
     * @param event the event from the listener
     * @param __ always {@code null}, used as a distraction to avoid users from invoking this method
     */
    @ApiStatus.OverrideOnly
    protected abstract void handleEvent(@NotNull E event, Void __);
}
