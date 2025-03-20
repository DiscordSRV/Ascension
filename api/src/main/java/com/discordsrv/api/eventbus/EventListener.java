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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * An event listener.
 */
public interface EventListener {

    /**
     * The full name of the class for this event listener.
     * @return the event listener class name
     * @see Class#getName()
     */
    @NotNull
    String className();

    /**
     * The name of the method for this event listener.
     * @return the method name for this event listener
     * @see Method#getName()
     */
    @NotNull
    String methodName();

    /**
     * The event this listener is listening to.
     * @return the event extending {@link Event} or {@link net.dv8tion.jda.api.events.GenericEvent}
     */
    @NotNull
    Class<?> eventClass();

}
