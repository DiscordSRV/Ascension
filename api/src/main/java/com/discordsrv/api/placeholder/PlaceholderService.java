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

package com.discordsrv.api.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface PlaceholderService {

    /**
     * The primary pattern used by DiscordSRV to find placeholders.
     */
    Pattern PATTERN = Pattern.compile("(%)([^%]+)(%)");

    /**
     * The pattern DiscordSRV uses to find recursive placeholders.
     */
    Pattern RECURSIVE_PATTERN = Pattern.compile("(\\{)(.+)(})");

    void addResultStringifier(@NotNull PlaceholderResultStringifier resultConverter);
    void removeResultStringifier(@NotNull PlaceholderResultStringifier resultConverter);

    String replacePlaceholders(@NotNull String placeholder, @NotNull Set<Object> context);
    String replacePlaceholders(@NotNull String placeholder, @NotNull Object... context);

    PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, @NotNull Set<Object> context);
    PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, @NotNull Object... context);

    Object getResult(@NotNull Matcher matcher, @NotNull Set<Object> context);
    String getResultAsString(@NotNull Matcher matcher, @NotNull Set<Object> context);

}
