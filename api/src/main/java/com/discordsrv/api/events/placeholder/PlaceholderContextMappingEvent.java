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

package com.discordsrv.api.events.placeholder;

import com.discordsrv.api.events.Event;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Used to map contexts into different objects by DiscordSRV's PlaceholderService.
 */
public class PlaceholderContextMappingEvent implements Event {

    private final List<Object> contexts;

    public PlaceholderContextMappingEvent(List<Object> contexts) {
        this.contexts = contexts;
    }

    @SuppressWarnings("unchecked") // It is checked
    public <T> void map(Class<T> type, Function<T, ?> mappingFunction) {
        for (int i = 0; i < contexts.size(); i++) {
            Object context = contexts.get(i);
            if (type.isAssignableFrom(context.getClass())) {
                contexts.set(i, mappingFunction.apply((T) context));
            }
        }
    }

    public void addContext(Object context) {
        this.contexts.add(context);
    }

    public void removeContext(Object context) {
        this.contexts.remove(context);
    }

    public List<Object> getContexts() {
        return Collections.unmodifiableList(contexts);
    }
}
