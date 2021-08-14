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

package com.discordsrv.common.discord.api.message;

import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.placeholder.converter.ComponentResultConverter;
import com.discordsrv.common.string.util.Placeholders;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendableDiscordMessageFormatterImpl implements SendableDiscordMessage.Formatter {

    private final Set<Object> context = new HashSet<>();
    private final Map<Pattern, Function<Matcher, Object>> replacements = new HashMap<>();

    private final DiscordSRV discordSRV;
    private final SendableDiscordMessage.Builder builder;

    public SendableDiscordMessageFormatterImpl(DiscordSRV discordSRV, SendableDiscordMessage.Builder builder) {
        this.discordSRV = discordSRV;
        this.builder = builder;
    }

    @Override
    public SendableDiscordMessage.Formatter addContext(Object... context) {
        this.context.addAll(Arrays.asList(context));
        return this;
    }

    @Override
    public SendableDiscordMessage.Formatter addReplacement(Pattern target, Function<Matcher, Object> replacement) {
        this.replacements.put(target, replacement);
        return this;
    }

    @Override
    public SendableDiscordMessage build() {
        Function<String, String> placeholders = input -> {
            if (input == null) {
                return null;
            }

            return new Placeholders(input)
                    .addAll(replacements)
                    .replaceAll(PlaceholderService.PATTERN,
                            matcher -> discordSRV.placeholderService().getResultAsString(matcher, context))
                    .get();
        };

        ComponentResultConverter.plain(() ->
                builder.setWebhookUsername(placeholders.apply(builder.getWebhookUsername())));
        builder.setContent(placeholders.apply(builder.getContent()));

        // TODO: rest of the content, escaping unwanted characters

        return builder.build();
    }
}
