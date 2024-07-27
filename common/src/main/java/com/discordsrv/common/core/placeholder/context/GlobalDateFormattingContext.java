/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.common.DiscordSRV;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class GlobalDateFormattingContext {

    private static final String TIMESTAMP_IDENTIFIER = "timestamp";

    private final LoadingCache<String, DateTimeFormatter> cache;

    public GlobalDateFormattingContext(DiscordSRV discordSRV) {
        this.cache = discordSRV.caffeineBuilder()
                .expireAfterAccess(30, TimeUnit.SECONDS)
                .build(DateTimeFormatter::ofPattern);
    }

    @Placeholder("date")
    public String formatDate(ZonedDateTime time, @PlaceholderRemainder String format) {
        if (format.startsWith(TIMESTAMP_IDENTIFIER)) {
            String style = format.substring(TIMESTAMP_IDENTIFIER.length());
            if (!style.isEmpty() && !style.startsWith(":")) {
                return null;
            }

            return "<t:" + time.toEpochSecond() + style + ">";
        }

        DateTimeFormatter formatter = cache.get(format);
        if (formatter == null) {
            throw new IllegalStateException("Illegal state");
        }
        return formatter.format(time);
    }

}
