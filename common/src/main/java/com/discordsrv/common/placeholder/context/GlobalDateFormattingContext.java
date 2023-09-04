package com.discordsrv.common.placeholder.context;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.common.DiscordSRV;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class GlobalDateFormattingContext {

    private final LoadingCache<String, DateTimeFormatter> cache;

    public GlobalDateFormattingContext(DiscordSRV discordSRV) {
        this.cache = discordSRV.caffeineBuilder()
                .expireAfterAccess(30, TimeUnit.SECONDS)
                .build(DateTimeFormatter::ofPattern);
    }

    @Placeholder("date")
    public String formatDate(ZonedDateTime time, @PlaceholderRemainder String format) {
        DateTimeFormatter formatter = cache.get(format);
        if (formatter == null) {
            throw new IllegalStateException("Illegal state");
        }
        return formatter.format(time);
    }

}
