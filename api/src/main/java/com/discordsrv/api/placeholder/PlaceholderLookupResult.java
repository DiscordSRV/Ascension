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

package com.discordsrv.api.placeholder;

import java.util.Set;

public class PlaceholderLookupResult {

    public static final PlaceholderLookupResult LOOKUP_FAILED = new PlaceholderLookupResult(Type.LOOKUP_FAILED);
    public static final PlaceholderLookupResult DATA_NOT_AVAILABLE = new PlaceholderLookupResult(Type.DATA_NOT_AVAILABLE);
    public static final PlaceholderLookupResult UNKNOWN_PLACEHOLDER = new PlaceholderLookupResult(Type.UNKNOWN_PLACEHOLDER);

    public static PlaceholderLookupResult success(Object result) {
        return new PlaceholderLookupResult(result);
    }

    public static PlaceholderLookupResult newLookup(String placeholder, Set<Object> extras) {
        return new PlaceholderLookupResult(placeholder, extras);
    }

    private final Type type;
    private final Object value;
    private final Set<Object> extras;

    protected PlaceholderLookupResult(Type type) {
        this.type = type;
        this.value = null;
        this.extras = null;
    }

    protected PlaceholderLookupResult(Object value) {
        this.type = Type.SUCCESS;
        this.value = value;
        this.extras = null;
    }

    protected PlaceholderLookupResult(String placeholder, Set<Object> extras) {
        this.type = Type.NEW_LOOKUP;
        this.value = placeholder;
        this.extras = extras;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public Set<Object> getExtras() {
        return extras;
    }

    public enum Type {

        SUCCESS,
        NEW_LOOKUP,

        LOOKUP_FAILED,
        DATA_NOT_AVAILABLE,
        UNKNOWN_PLACEHOLDER
    }
}
