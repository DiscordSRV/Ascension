/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.event.bus;

import com.discordsrv.api.event.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

/**
 * DiscordSRV's event bus, handling all events extending {@link Event}s and {@link GenericEvent}s.
 */
@SuppressWarnings("unused") // API
public interface EventBus {

    /**
     * Subscribes the provided event listener to this {@link EventBus}.
     * @param eventListener an event listener with at least one valid {@link Subscribe} method.
     *
     * @throws IllegalArgumentException if the given listener does not contain any valid listeners
     */
    void subscribe(@NotNull Object eventListener);

    /**
     * Unsubscribes a listener that was registered before.
     * @param eventListener an event listener that was subscribed with {@link #subscribe(Object)} before
     */
    void unsubscribe(@NotNull Object eventListener);

    /**
     * Publishes a DiscordSRV {@link Event} to this {@link EventBus}.
     *
     * @param event the event
     */
    @Blocking
    void publish(@NotNull Event event);

    /**
     * Publishes a JDA {@link GenericEvent} to this {@link EventBus}.
     *
     * @param event the event
     */
    @Blocking
    void publish(@NotNull GenericEvent event);

}
