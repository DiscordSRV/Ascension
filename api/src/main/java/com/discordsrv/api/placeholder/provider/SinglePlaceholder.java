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

package com.discordsrv.api.placeholder.provider;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Supplier;

public class SinglePlaceholder implements PlaceholderProvider {

    private final String matchPlaceholder;
    private final Supplier<Object> resultProvider;

    public SinglePlaceholder(String placeholder, Object result) {
        this(placeholder, () -> result);
    }

    public SinglePlaceholder(String placeholder, Supplier<Object> resultProvider) {
        this.matchPlaceholder = placeholder;
        this.resultProvider = resultProvider;
    }

    @Override
    public @NotNull PlaceholderLookupResult lookup(@NotNull String placeholder, @NotNull Set<Object> context) {
        boolean perfectMatch = placeholder.equals(matchPlaceholder);
        if (!perfectMatch && !placeholder.startsWith(matchPlaceholder + '_') && !placeholder.startsWith(matchPlaceholder + ':')) {
            return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
        }

        try {
            Object result = resultProvider.get();
            if (perfectMatch) {
                return PlaceholderLookupResult.success(result);
            }

            return PlaceholderLookupResult.reLookup(placeholder.substring(matchPlaceholder.length()), result, true);
        } catch (Throwable t) {
            return PlaceholderLookupResult.lookupFailed(t);
        }
    }
}
