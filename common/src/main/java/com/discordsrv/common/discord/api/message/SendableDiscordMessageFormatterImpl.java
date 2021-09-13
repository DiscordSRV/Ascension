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

import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.api.util.DiscordFormattingUtil;
import com.discordsrv.api.placeholder.FormattedText;
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
        this.builder = builder.clone();
    }

    @Override
    public SendableDiscordMessage.Formatter addContext(Object... context) {
        this.context.addAll(Arrays.asList(context));
        return this;
    }

    @Override
    public SendableDiscordMessage.Formatter addReplacement(Pattern target, Function<Matcher, Object> replacement) {
        this.replacements.put(target, wrapFunction(replacement));
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
                            wrapFunction(
                                    matcher -> discordSRV.placeholderService().getResultAsString(matcher, context)))
                    .get();
        };
        builder.setContent(placeholders.apply(builder.getContent()));

        List<DiscordMessageEmbed> embeds = new ArrayList<>(builder.getEmbeds());
        builder.getEmbeds().clear();

        for (DiscordMessageEmbed embed : embeds) {
            DiscordMessageEmbed.Builder embedBuilder = embed.toBuilder();

            // TODO: check which parts allow formatting more thoroughly
            ComponentResultConverter.plainComponents(() -> {
                embedBuilder.setAuthor(
                        placeholders.apply(
                                embedBuilder.getAuthorName()),
                        placeholders.apply(
                                embedBuilder.getAuthorUrl()),
                        placeholders.apply(
                                embedBuilder.getAuthorImageUrl()));

                embedBuilder.setTitle(
                        placeholders.apply(
                                embedBuilder.getTitle()),
                        placeholders.apply(
                                embedBuilder.getTitleUrl()));

                embedBuilder.setThumbnailUrl(
                        placeholders.apply(
                                embedBuilder.getThumbnailUrl()));

                embedBuilder.setImageUrl(
                        placeholders.apply(
                                embedBuilder.getImageUrl()));

                embedBuilder.setFooter(
                        placeholders.apply(
                                embedBuilder.getFooter()),
                        placeholders.apply(
                                embedBuilder.getFooterImageUrl()));
            });

            embedBuilder.setDescription(
                    placeholders.apply(
                            embedBuilder.getDescription())
            );

            List<DiscordMessageEmbed.Field> fields = new ArrayList<>(embedBuilder.getFields());
            embedBuilder.getFields().clear();

            fields.forEach(field -> embedBuilder.addField(
                    placeholders.apply(
                            field.getTitle()),
                    placeholders.apply(
                            field.getValue()),
                    field.isInline()
            ));

            builder.addEmbed(embedBuilder.build());
        }

        ComponentResultConverter.plainComponents(() -> {
            builder.setWebhookUsername(placeholders.apply(builder.getWebhookUsername()));
            builder.setWebhookAvatarUrl(placeholders.apply(builder.getWebhookAvatarUrl()));
        });

        return builder.build();
    }

    private Function<Matcher, Object> wrapFunction(Function<Matcher, Object> function) {
        return matcher -> {
            Object result = function.apply(matcher);
            if (result instanceof FormattedText) {
                // Process as regular text
                return result.toString();
            } else if (result instanceof CharSequence) {
                // Escape content
                return DiscordFormattingUtil.escapeContent(
                        result.toString());
            }

            // Use default behaviour for everything else
            return result;
        };
    }
}
