/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.events;

import com.discordsrv.api.eventbus.EventBus;
import com.discordsrv.api.eventbus.EventListener;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.eventbus.internal.EventStateHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Event} that can be cancelled.
 */
public interface Cancellable extends Event {

    /**
     * If this event is cancelled.
     * @return true if this event is cancelled
     */
    boolean isCancelled();

    /**
     * Sets the cancelled state of this event, which may or may not be followed by event listeners.
     * @param cancelled the new cancelled state of this event
     * @see Subscribe#ignoreCancelled()
     */
    void setCancelled(boolean cancelled);

    /**
     * Returns the {@link EventListener} that cancelled this event.
     * This is changed every time the event goes from not being cancelled to being cancelled.
     *
     * @return the event listener that cancelled this event or {@code null} if it was cancelled before being passed to the {@link EventBus}
     * @throws IllegalStateException if the event isn't cancelled
     */
    @ApiStatus.NonExtendable
    @Nullable
    default EventListener whoCancelled() {
        EventListener listener = EventStateHolder.CANCELLED.get();
        if (listener == null) {
            throw new IllegalStateException("Event is not cancelled");
        } else if (listener == EventStateHolder.UNKNOWN_LISTENER) {
            return null;
        }

        return listener;
    }
}
