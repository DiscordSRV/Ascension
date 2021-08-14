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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholders {

    private final String inputText;
    private final Map<Pattern, Function<Matcher, Object>> replacements = new HashMap<>();

    public Placeholders(String inputText) {
        this.inputText = inputText;
    }

    public Placeholders addAll(Map<Pattern, Function<Matcher, Object>> replacements) {
        replacements.forEach(this.replacements::put);
        return this;
    }

    public Placeholders replace(String target, Object replacement) {
        return replace(target, matcher -> replacement);
    }

    public Placeholders replaceAll(Pattern pattern, Object replacement) {
        return replaceAll(pattern, matcher -> replacement);
    }

    public Placeholders replace(String target, Supplier<Object> replacement) {
        return replaceAll(Pattern.compile(target, Pattern.LITERAL), matcher -> replacement);
    }

    public Placeholders replaceAll(Pattern pattern, Supplier<Object> replacement) {
        return replaceAll(pattern, matcher -> replacement);
    }

    public Placeholders replace(String target, Function<Matcher, Object> replacement) {
        return replaceAll(Pattern.compile(target, Pattern.LITERAL), replacement);
    }

    public Placeholders replaceAll(Pattern pattern, Function<Matcher, Object> replacement) {
        this.replacements.put(pattern, replacement);
        return this;
    }

    public String get() {
        String input = inputText;
        for (Map.Entry<Pattern, Function<Matcher, Object>> entry : replacements.entrySet()) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(input);
            if (!matcher.find()) {
                continue;
            }

            Function<Matcher, Object> replacement = entry.getValue();
            Object value = replacement.apply(matcher);

            input = matcher.replaceAll(String.valueOf(value));
        }
        return input;
    }
}
