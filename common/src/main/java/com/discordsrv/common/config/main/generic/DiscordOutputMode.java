/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;

public enum DiscordOutputMode {

    PLAIN("", "", PlainPlaceholderFormat.Formatting.PLAIN),
    MARKDOWN("", "", PlainPlaceholderFormat.Formatting.DISCORD_MARKDOWN),
    ANSI("```ansi\n", "```", PlainPlaceholderFormat.Formatting.ANSI),
    LOG("```accesslog\n", "```", PlainPlaceholderFormat.Formatting.PLAIN),
    CODE_BLOCK("```\n", "```", PlainPlaceholderFormat.Formatting.PLAIN),
    DIFF("```diff\n", "```", PlainPlaceholderFormat.Formatting.PLAIN),
    OFF("", "", null);

    private final String prefix;
    private final String suffix;
    private final PlainPlaceholderFormat.Formatting plainFormat;

    DiscordOutputMode(String prefix, String suffix, PlainPlaceholderFormat.Formatting plainFormat) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.plainFormat = plainFormat;
    }

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }

    public PlainPlaceholderFormat.Formatting plainFormat() {
        return plainFormat;
    }

    public int blockLength() {
        return prefix().length() + suffix().length();
    }
}
