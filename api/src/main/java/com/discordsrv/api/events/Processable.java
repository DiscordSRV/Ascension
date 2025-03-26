/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.eventbus.internal.EventStateHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Event} that can be processed.
 */
public interface Processable extends Event {

    /**
     * Has this event has been processed.
     * @return true if this event has been processed
     */
    boolean isProcessed();

    /**
     * Returns the {@link EventListener} that processed this event.
     *
     * @return the event listener that processed this event or {@code null} if it was processed before being passed to the {@link EventBus}
     * @throws IllegalStateException if the event has not been processed
     */
    @ApiStatus.NonExtendable
    @Nullable
    default EventListener whoProcessed() {
        EventListener listener = EventStateHolder.PROCESSED.get();
        if (listener == null) {
            throw new IllegalStateException("Event has not been processed");
        } else if (listener == EventStateHolder.UNKNOWN_LISTENER) {
            return null;
        }

        return listener;
    }

    interface NoArgument extends Processable {

        /**
         * Marks this event as processed. This cannot be reversed.
         * @see #isProcessed()
         */
        void markAsProcessed();
    }

    interface Argument<T> extends Processable {

        /**
         * Marks this event as processed with the given argument. This cannot be reversed.
         * @param input the argument for processing the event
         * @see #isProcessed()
         */
        void process(T input);
    }
}
