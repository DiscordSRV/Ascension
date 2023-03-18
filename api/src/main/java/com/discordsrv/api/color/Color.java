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

package com.discordsrv.api.color;

import java.util.Objects;

/**
 * Simple helper class for handling rgb colors, that is compatible with headless java installations.
 */
public class Color {

    /**
     * Discord's blurple color (<a href="https://discord.com/branding">Discord branding</a>).
     */
    public static final Color BLURPLE = new Color(0x5865F2);
    public static final Color WHITE = new Color(0xFFFFFF);
    public static final Color BLACK = new Color(0);

    private final int rgb;

    /**
     * Accepts any integer, but will only account for the first 24 bits.
     * @param rgb the rgb value
     */
    public Color(int rgb) {
        this.rgb = rgb & 0xFFFFFF;
    }

    public Color(int red, int green, int blue) {
        if (red < 0 || red > 255) {
            throw new IllegalArgumentException("Red is out of range");
        } else if (green < 0 || green > 255) {
            throw new IllegalArgumentException("Green is out of range");
        } else if (blue < 0 || blue > 255) {
            throw new IllegalArgumentException("Blue is out of range");
        }

        this.rgb = ((red & 0xFF) << 16)
                 + ((green & 0xFF) << 8)
                 + (blue & 0xFF);
    }

    public Color(String hex) {
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Input hex must be exactly 6 characters long");
        }

        this.rgb = Integer.parseInt(hex, 16);
    }

    public int rgb() {
        return rgb;
    }

    public String hex() {
        return Integer.toHexString(0xF000000 | rgb).substring(1);
    }

    public int red() {
        return (rgb & 0xFF0000) >> 16;
    }

    public int green() {
        return (rgb & 0x00FF00) >> 8;
    }

    public int blue() {
        return rgb & 0x0000FF;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Color color = (Color) o;
        return rgb == color.rgb;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rgb);
    }

    @Override
    public String toString() {
        return "Color{#" + hex() + "}";
    }
}
