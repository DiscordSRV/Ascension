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

package com.discordsrv.api.discord.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DiscordFormattingUtil {

    private static final Pattern CODE_STRING_REGEX_PREFIX = Pattern.compile("^`");
    private static final Pattern CODE_STRING_REGEX_SUFFIX = Pattern.compile("`(?=`)|`$");
    private static final List<Character> FORMATTING_CHARACTERS = Arrays.asList('*', '_', '|', '`', '~', ':', '[');
    private static final Pattern FORMATTING_PATTERN = Pattern.compile(
            // Group 1 is for picking up if the character is part of a URL, in which case escaping will not occur
            "(https?://.*\\.[^ ]*)?"
            // Group 2 the formatting character
            + "(["
            + FORMATTING_CHARACTERS.stream()
            .map(character -> Pattern.quote(String.valueOf(character)))
            .collect(Collectors.joining())
            + "])"
    );
    private static final Pattern QUOTE_PATTERN = Pattern.compile("(^|\n)>");
    private static final Pattern MENTION_PATTERN = Pattern.compile("(<[@#][0-9]{16,20}>)");

    private DiscordFormattingUtil() {}

    public static String escapeContent(String content) {
        content = escapeEscapes(content);
        content = escapeFormatting(content);
        content = escapeQuotes(content);
        content = escapeMentions(content);
        return content;
    }

    public static String escapeEscapes(String content) {
        return content.replace("\\", "\\\\");
    }

    public static String escapeFormatting(String content) {
        Matcher matcher = FORMATTING_PATTERN.matcher(content);
        StringBuffer output = new StringBuffer();
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                continue;
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement("\\" + matcher.group(2)));
            lastEnd = matcher.end();
        }
        output.append(content.substring(lastEnd));
        return output.toString();
    }

    public static String escapeQuotes(String input) {
        return QUOTE_PATTERN.matcher(input).replaceAll("$1" + Matcher.quoteReplacement("\\>"));
    }

    public static String escapeMentions(String input) {
        return MENTION_PATTERN.matcher(input).replaceAll(Matcher.quoteReplacement("\\") + "$1");
    }

    public static String escapeCodeBlockContent(String input) {
        return input.replace("``", "`\u200B`"); // zero-width-space
    }

    public static String makeCodeString(String content) {
        // Add zero width spaces to prevent double backticks inside the block,
        // and single backticks at the beginning and end of the block
        content = CODE_STRING_REGEX_PREFIX.matcher(content).replaceAll("\u200B`"); // zero-width-space
        content = CODE_STRING_REGEX_SUFFIX.matcher(content).replaceAll("`\u200B"); // zero-width-space
        return "``" + content + "``";
    }
}
