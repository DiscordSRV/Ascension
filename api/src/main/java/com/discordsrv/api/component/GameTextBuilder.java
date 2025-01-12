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

package com.discordsrv.api.component;

import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft equivalent for {@link com.discordsrv.api.discord.entity.message.SendableDiscordMessage.Formatter}.
 */
public interface GameTextBuilder {

    @NotNull
    GameTextBuilder addContext(Object... context);

    default GameTextBuilder addPlaceholder(String placeholder, Object replacement) {
        return addContext(new SinglePlaceholder(placeholder, replacement));
    }

    default GameTextBuilder addPlaceholder(String placeholder, Supplier<Object> replacementSupplier) {
        return addContext(new SinglePlaceholder(placeholder, replacementSupplier));
    }

    @NotNull
    default GameTextBuilder addReplacement(String target, Object replacement) {
        return addReplacement(Pattern.compile(target, Pattern.LITERAL), replacement);
    }

    @NotNull
    default GameTextBuilder addReplacement(Pattern target, Object replacement) {
        return addReplacement(target, matcher -> replacement);
    }

    @NotNull
    default GameTextBuilder addReplacement(String target, Supplier<Object> replacement) {
        return addReplacement(Pattern.compile(target, Pattern.LITERAL), replacement);
    }

    @NotNull
    default GameTextBuilder addReplacement(Pattern target, Supplier<Object> replacement) {
        return addReplacement(target, matcher -> replacement.get());
    }

    @NotNull
    default GameTextBuilder addReplacement(String target, Function<Matcher, Object> replacement) {
        return addReplacement(Pattern.compile(target, Pattern.LITERAL), replacement);
    }

    @NotNull
    GameTextBuilder addReplacement(Pattern target, Function<Matcher, Object> replacement);

    @NotNull
    GameTextBuilder applyPlaceholderService();

    @NotNull
    MinecraftComponent build();
}
