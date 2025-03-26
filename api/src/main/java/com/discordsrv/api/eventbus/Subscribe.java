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

import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.Processable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Placed on a public non-abstract non-static method that has only 1 parameter,
 * being an event extending {@link Event} or {@link net.dv8tion.jda.api.events.GenericEvent}.
 * <p>
 * You can register a listener through {@link EventBus#subscribe(Object)}, {@link DiscordSRV#eventBus()} to get the event bus.
 * Registered {@link com.discordsrv.api.module.Module}s are automatically registered to the event bus.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * If this listener ignores events that are cancelled ({@link Cancellable}).
     * <b>Defaults to {@code true}</b>
     * @return if cancelled events are ignored
     * @see Cancellable#isCancelled()
     */
    boolean ignoreCancelled() default true;

    /**
     * If this listener ignores events that are already processed ({@link com.discordsrv.api.events.Processable}).
     * <b>Defaults to {@code true}</b>
     * @return if already processed events are ignored
     * @see Processable#isProcessed()
     */
    boolean ignoreProcessed() default true;

    /**
     * The priority for this event listener, this determines the order that event listeners receive events.
     * {@link Byte#MIN_VALUE} (same as {@link EventPriorities#PRE}) and {@link Byte#MAX_VALUE} (same as {@link EventPriorities#POST})
     * are reserved for observing the events, and should not be used applying modifications.
     *
     * @return the priority of this event listener
     * @see EventPriorities
     */
    byte priority() default 0;

}
