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

package com.discordsrv.api.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class PlaceholderLookupResult {

    public static final PlaceholderLookupResult DATA_NOT_AVAILABLE = new PlaceholderLookupResult(Type.DATA_NOT_AVAILABLE);
    public static final PlaceholderLookupResult UNKNOWN_PLACEHOLDER = new PlaceholderLookupResult(Type.UNKNOWN_PLACEHOLDER);

    public static PlaceholderLookupResult success(@Nullable Object result) {
        return new PlaceholderLookupResult(result);
    }

    public static PlaceholderLookupResult newLookup(@NotNull String placeholder, @NotNull Set<Object> context) {
        return new PlaceholderLookupResult(placeholder, context);
    }

    public static PlaceholderLookupResult reLookup(@NotNull String remainder, @NotNull Object result, @NotNull Object... newContext) {
        return new PlaceholderLookupResult(remainder, result, new LinkedHashSet<>(Arrays.asList(newContext)));
    }

    public static PlaceholderLookupResult lookupFailed(@NotNull Throwable error) {
        return new PlaceholderLookupResult(error);
    }

    private final Type type;
    private final String placeholder;
    private final Object result;
    private final Throwable error;
    private final Set<Object> context;

    protected PlaceholderLookupResult(Type type) {
        this.type = type;
        this.placeholder = null;
        this.result = null;
        this.error = null;
        this.context = null;
    }

    protected PlaceholderLookupResult(Object value) {
        this.type = Type.SUCCESS;
        this.placeholder = null;
        this.result = value;
        this.error = null;
        this.context = null;
    }

    protected PlaceholderLookupResult(Throwable error) {
        this.type = Type.LOOKUP_FAILED;
        this.placeholder = null;
        this.result = null;
        this.error = error;
        this.context = null;
    }

    protected PlaceholderLookupResult(String placeholder, Set<Object> context) {
        this.type = Type.NEW_LOOKUP;
        this.placeholder = placeholder;
        this.result = null;
        this.error = null;
        this.context = context;
    }

    protected PlaceholderLookupResult(String placeholder, Object result, Set<Object> newContext) {
        this.type = Type.RE_LOOKUP;
        this.placeholder = placeholder;
        this.result = result;
        this.error = null;
        this.context = newContext;
    }

    public Type getType() {
        return type;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public Object getResult() {
        return result;
    }

    public Throwable getError() {
        return error;
    }

    public Set<Object> getContext() {
        return context;
    }

    public enum Type {

        SUCCESS,
        // Completely new placeholder lookup
        NEW_LOOKUP,
        // Refining a result further
        RE_LOOKUP,

        LOOKUP_FAILED,
        DATA_NOT_AVAILABLE,
        UNKNOWN_PLACEHOLDER
    }
}
