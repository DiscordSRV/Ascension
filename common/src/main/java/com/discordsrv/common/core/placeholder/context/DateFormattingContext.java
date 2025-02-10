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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.common.DiscordSRV;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.TimeUnit;

public class DateFormattingContext {

    private static final String TIMESTAMP_IDENTIFIER = "timestamp";

    private final DiscordSRV discordSRV;
    private final LoadingCache<String, DateTimeFormatter> cache;

    public DateFormattingContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.cache = discordSRV.caffeineBuilder()
                .expireAfterAccess(30, TimeUnit.SECONDS)
                .build(DateTimeFormatter::ofPattern);
    }

    @Placeholder("date")
    public CharSequence formatDate(TemporalAccessor time, @PlaceholderRemainder String format) {
        if (format.startsWith(TIMESTAMP_IDENTIFIER)) {
            String style = format.substring(TIMESTAMP_IDENTIFIER.length());
            if ((!style.isEmpty() && !style.startsWith(":")) || !time.isSupported(ChronoField.INSTANT_SECONDS)) {
                return null;
            }

            return FormattedText.of("<t:" + time.getLong(ChronoField.INSTANT_SECONDS) + style + ">");
        }

        DateTimeFormatter formatter = cache.get(format);
        if (formatter == null) {
            throw new IllegalStateException("Illegal state");
        }

        try {
            return formatter.format(time);
        } catch (DateTimeException e) {
            return e.getMessage();
        }
    }

    @Placeholder(value = "start_date", relookup = "date")
    public ZonedDateTime getStartDate() {
        return discordSRV.getInitializeTime();
    }

    @Placeholder(value = "now_date", relookup = "date")
    public ZonedDateTime getNowDate() {
        return ZonedDateTime.now();
    }

    @Placeholder(value = "now_time")
    public OffsetDateTime getTimeNow() {
        return OffsetDateTime.now();
    }
}
