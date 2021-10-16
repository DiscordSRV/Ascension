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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PlaceholderResultStringifier {

    /**
     * @see #plainComponents(Runnable)
     */
    @ApiStatus.Internal
    ThreadLocal<Boolean> PLAIN_COMPONENT_CONTEXT = new ThreadLocal<>();

    /**
     * Utility method to run the provided {@link Runnable} where {@link PlaceholderService}s
     * will replace {@link com.discordsrv.api.component.MinecraftComponent}s
     * as plain without formatting (instead of converting to Discord formatting).
     * @param runnable a task that will be executed immediately
     */
    static void plainComponents(Runnable runnable) {
        PLAIN_COMPONENT_CONTEXT.set(true);
        runnable.run();
        PLAIN_COMPONENT_CONTEXT.set(false);
    }

    /**
     * Converts a successful placeholder lookup result into a {@link String}.
     * @param result the result
     * @return the result in {@link String} form
     */
    String convertPlaceholderResult(@NotNull Object result);

}
