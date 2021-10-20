/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.event.events.placeholder;

import com.discordsrv.api.event.events.Event;
import com.discordsrv.api.event.events.Processable;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public class PlaceholderLookupEvent implements Event, Processable {

    private final String placeholder;
    private final Set<Object> contexts;

    private boolean processed;
    private PlaceholderLookupResult result;

    public PlaceholderLookupEvent(String placeholder, Set<Object> contexts) {
        this.placeholder = placeholder;
        this.contexts = contexts;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public Set<Object> getContexts() {
        return contexts;
    }

    public Optional<Object> getContext(Class<?> type) {
        for (Object o : contexts) {
            if (type.isAssignableFrom(o.getClass())) {
                return Optional.of(o);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    /**
     * Returns the {@link PlaceholderLookupResult} from a {@link #process(PlaceholderLookupResult)} matching required criteria.
     * @return the placeholder lookup result provided by a listener
     * @throws IllegalStateException if {@link #isProcessed()} doesn't return true
     */
    @NotNull
    public PlaceholderLookupResult getResultFromProcessing() {
        if (!processed) {
            throw new IllegalStateException("This event has not been successfully processed yet, no result is available");
        }
        return result;
    }

    /**
     * Provides a {@link PlaceholderLookupResult} for the provided {@link #getPlaceholder()} and {@link #getContexts()}.
     * @param result the result
     * @throws IllegalStateException if the event is already processed
     */
    public void process(@NotNull PlaceholderLookupResult result) {
        if (processed) {
            throw new IllegalStateException("Already processed");
        }
        if (result.getType() == PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
            // Ignore unknown
            return;
        }

        this.result = result;
        this.processed = true;
    }

    @Override
    @Deprecated
    public void markAsProcessed() {
        throw new RuntimeException("Please use process(PlaceholderLookupResult) instead");
    }
}
