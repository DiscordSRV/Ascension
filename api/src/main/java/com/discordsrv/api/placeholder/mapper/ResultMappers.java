/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.placeholder.mapper;

import com.discordsrv.api.placeholder.PlaceholderService;

import java.util.function.Supplier;

public final class ResultMappers {

    private static final ThreadLocal<Boolean> PLAIN = ThreadLocal.withInitial(() -> false);

    private ResultMappers() {}

    public static boolean isPlainContext() {
        return PLAIN.get();
    }

    /**
     * Utility method to run the provided {@link Runnable} where {@link PlaceholderService}s
     * will use plain text without Discord formatting (instead of converting to Discord formatting).
     * @param runnable a task that will be executed immediately
     */
    public static void runInPlainContext(Runnable runnable) {
        getInPlainContext(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Utility method to run the provided {@link Runnable} where {@link PlaceholderService}s
     * will use plain text without Discord formatting (instead of converting to Discord formatting).
     * @param supplier a supplier that will be executed immediately
     * @return the output of the supplier provided as parameter
     */
    public static <T> T getInPlainContext(Supplier<T> supplier) {
        PLAIN.set(true);
        T output = supplier.get();
        PLAIN.set(false);
        return output;
    }
}
