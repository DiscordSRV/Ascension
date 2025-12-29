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

package com.discordsrv.api.eventbus;

import com.discordsrv.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * DiscordSRV's event bus, handling all events extending {@link Event}s and {@link GenericEvent}s.
 */
public interface EventBus {

    /**
     * Subscribes the provided event listener to this {@link EventBus}.
     * @param eventListener an event listener with at least one valid {@link Subscribe} method.
     *
     * @throws IllegalArgumentException if the given listener does not contain any valid listeners
     */
    void subscribe(@NotNull Object eventListener);

    /**
     * Unsubscribes a listener that was registered before. This may be used to unsubscribe non-annotation based listeners via their {@link EventListener} objects.
     * @param eventListener an event listener that was subscribed with {@link #subscribe(Object)} before, or {@link EventListener} returned by one of the other subscribe methods
     */
    void unsubscribe(@NotNull Object eventListener);

    /**
     * Subscribes to the given event, the provided {@link Consumer} will receive the events.
     * With {@link EventPriorities#DEFAULT} priority, ignoring canceled and processed events.
     * @param eventClass the event class
     * @param listener the listener which will receive events
     * @param <E> type extending DiscordSRV {@link Event} or JDA {@link GenericEvent}.
     * @return a new {@link EventListener} which can be unsubscribed via {@link #unsubscribe(Object)}
     */
    default <E> EventListener subscribe(@NotNull Class<E> eventClass, @NotNull Consumer<E> listener) {
        return subscribe(eventClass, EventPriorities.DEFAULT, listener);
    }

    /**
     * Subscribes to the given event, the provided {@link Consumer} will receive the events.
     * Ignoring canceled and processed events.
     * @param eventClass the event class
     * @param listenerPriority priority of the listener
     * @param listener the listener which will receive events
     * @param <E> type extending DiscordSRV {@link Event} or JDA {@link GenericEvent}.
     * @return a new {@link EventListener} which can be unsubscribed via {@link #unsubscribe(Object)}
     */
    default <E> EventListener subscribe(@NotNull Class<E> eventClass, byte listenerPriority, @NotNull Consumer<E> listener) {
        return subscribe(eventClass, true, true, listenerPriority, listener);
    }

    /**
     * Subscribes to the given event, the provided {@link Consumer} will receive the events.
     * @param eventClass the event class
     * @param ignoreCanceled if canceled events should be ignored
     * @param ignoreProcessed if processed events should be ignored
     * @param listenerPriority priority of the listener
     * @param listener the listener which will receive events
     * @param <E> type extending DiscordSRV {@link Event} or JDA {@link GenericEvent}.
     * @return a new {@link EventListener} which can be unsubscribed via {@link #unsubscribe(Object)}
     */
    <E> EventListener subscribe(@NotNull Class<E> eventClass, boolean ignoreCanceled, boolean ignoreProcessed, byte listenerPriority, @NotNull Consumer<E> listener);

    /**
     * Gets the listeners for a given event listener.
     *
     * @param eventListener an event listener that has valid {@link Subscribe} methods.
     * @return a set of event listener in the provided class according to this {@link EventBus}
     */
    Collection<? extends EventListener> getListeners(@NotNull Object eventListener);

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
