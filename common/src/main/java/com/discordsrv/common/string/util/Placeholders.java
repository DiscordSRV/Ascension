/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.string.util;

import javax.annotation.CheckReturnValue;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholders {

    private final String inputText;
    private final Map<Pattern, Supplier<String>> replacements = new HashMap<>();

    public Placeholders(String inputText) {
        this.inputText = inputText;
    }

    @CheckReturnValue
    public Placeholders replace(String replace, String replacement) {
        return replace(replace, () -> replacement);
    }

    @CheckReturnValue
    public Placeholders replaceAll(String regex, String replacement) {
        return replaceAll(regex, () -> replacement);
    }

    @CheckReturnValue
    public Placeholders replaceAll(Pattern pattern, String replacement) {
        return replaceAll(pattern, () -> replacement);
    }

    @CheckReturnValue
    public Placeholders replace(String replace, Supplier<String> replacement) {
        return replaceAll(Pattern.compile(replace, Pattern.LITERAL), replacement);
    }

    @CheckReturnValue
    public Placeholders replaceAll(String regex, Supplier<String> replacement) {
        return replaceAll(Pattern.compile(regex), replacement);
    }

    @CheckReturnValue
    public Placeholders replaceAll(Pattern pattern, Supplier<String> replacement) {
        replacements.put(pattern, replacement);
        return this;
    }

    public String get() {
        String input = inputText;
        for (Map.Entry<Pattern, Supplier<String>> entry : replacements.entrySet()) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(input);
            if (!matcher.find()) {
                continue;
            }

            Supplier<String> replacement = entry.getValue();
            input = matcher.replaceAll(replacement.get());
        }
        return input;
    }
}
