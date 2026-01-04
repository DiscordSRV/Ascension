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

package com.discordsrv.api.placeholder.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlaceholdersTest {

    @Test
    public void orderTest() {
        Placeholders placeholders = new Placeholders("a");

        placeholders.replace("b", "c");
        placeholders.replace("a", "b");

        assertEquals("b", placeholders.toString());
    }

    @Test
    public void uselessContentTest() {
        Placeholders placeholders = new Placeholders("stuff a stuff");

        placeholders.replace("a", "b");

        assertEquals("stuff b stuff", placeholders.toString());
    }

    @Test
    public void multipleTest() {
        Placeholders placeholders = new Placeholders("a b");

        placeholders.replace("a", "c");
        placeholders.replace("b", "d");

        assertEquals("c d", placeholders.toString());
    }

    @Test
    public void multipleSamePatternTest() {
        Placeholders placeholders = new Placeholders("a a");

        AtomicBoolean used = new AtomicBoolean(false);
        placeholders.replace("a", matcher -> {
            if (used.get()) {
                return "c";
            } else {
                used.set(true);
                return "b";
            }
        });

        assertEquals("b c", placeholders.toString());
    }
}
