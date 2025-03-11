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

/**
 * A simple list of priorities for use in {@link Subscribe#priority()}, listeners may be assigned to any value of {@code byte}.
 */
public final class EventPriorities {

    /**
     * Use to observe the event before any changes.
     * Applying changes to the event at this priority is bad practice.
     */
    public static final byte PRE = Byte.MIN_VALUE;

    public static final byte EARLIEST = (byte) -96;
    public static final byte EARLY = (byte) -48;

    /**
     * The default priority for {@link Subscribe#priority} unless otherwise specified.
     */
    public static final byte DEFAULT = (byte) 0;

    public static final byte LATE = (byte) 48;
    public static final byte LAST = (byte) 96;

    /**
     * Use to observe the event after all changes.
     * Applying changes to the event at this priority is bad practice.
     */
    public static final byte POST = Byte.MAX_VALUE;

    private EventPriorities() {}
}
