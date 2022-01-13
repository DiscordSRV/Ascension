/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

/**
 * A simple enum to dictate the order that event listeners will be executed, going from {@link #POST} to {@link #POST}.
 */
public enum EventPriority {

    /**
     * This is the first in the priority order, this should be used to observe the event before any processing.
     */
    PRE,

    /**
     * This is the earliest in the processing. This should be used to cancel events.
     */
    EARLIEST,

    /**
     * This should be used to modify events.
     */
    EARLY,

    /**
     * The default priority, right in the middle of the priority order. Use this if you need to override
     * one of DiscordSRV's implementations for {@link com.discordsrv.api.event.events.Processable}s.
     */
    DEFAULT,

    /**
     * This is where DiscordSRV's integrations for other plugins will process {@link com.discordsrv.api.event.events.Processable}'s.
     */
    LATE,

    /**
     * This is where DiscordSRV's default implementations for {@link com.discordsrv.api.event.events.Processable}'s will run.
     */
    LAST,

    /**
     * This is the last in the priority order, this should be used to observe the event after all processing is complete.
     */
    POST

}
