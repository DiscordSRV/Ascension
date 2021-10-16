/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.api.entity.message.impl;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.DiscordSRVApiProvider;
import com.discordsrv.api.discord.api.entity.message.AllowedMention;
import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.api.util.DiscordFormattingUtil;
import com.discordsrv.api.placeholder.FormattedText;
import com.discordsrv.api.placeholder.PlaceholderResultStringifier;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.util.Placeholders;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendableDiscordMessageImpl implements SendableDiscordMessage {

    private final String content;
    private final List<DiscordMessageEmbed> embeds;
    private final Set<AllowedMention> allowedMentions;
    private final String webhookUsername;
    private final String webhookAvatarUrl;

    protected SendableDiscordMessageImpl(String content,
                                         List<DiscordMessageEmbed> embeds,
                                         Set<AllowedMention> allowedMentions,
                                         String webhookUsername,
                                         String webhookAvatarUrl) {
        this.content = content;
        this.embeds = embeds;
        this.allowedMentions = allowedMentions;
        this.webhookUsername = webhookUsername;
        this.webhookAvatarUrl = webhookAvatarUrl;
    }

    @Override
    public @NotNull Optional<String> getContent() {
        return Optional.ofNullable(content);
    }

    @Override
    public @NotNull List<DiscordMessageEmbed> getEmbeds() {
        return embeds;
    }

    @Override
    public @NotNull Set<AllowedMention> getAllowedMentions() {
        return allowedMentions;
    }

    @Override
    public @NotNull Optional<String> getWebhookUsername() {
        return Optional.ofNullable(webhookUsername);
    }

    @Override
    public @NotNull Optional<String> getWebhookAvatarUrl() {
        return Optional.of(webhookAvatarUrl);
    }

    public static class BuilderImpl implements SendableDiscordMessage.Builder {

        private String content;
        private final List<DiscordMessageEmbed> embeds = new ArrayList<>();
        private final Set<AllowedMention> allowedMentions = new HashSet<>();
        private String webhookUsername;
        private String webhookAvatarUrl;

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public @NotNull BuilderImpl setContent(String content) {
            this.content = content;
            return this;
        }

        @Override
        public @NotNull List<DiscordMessageEmbed> getEmbeds() {
            return embeds;
        }

        @Override
        public @NotNull Builder addEmbed(DiscordMessageEmbed embed) {
            this.embeds.add(embed);
            return this;
        }

        @Override
        public @NotNull Builder removeEmbed(DiscordMessageEmbed embed) {
            this.embeds.remove(embed);
            return this;
        }

        @Override
        public Set<AllowedMention> getAllowedMentions() {
            return allowedMentions;
        }

        @Override
        public @NotNull Builder addAllowedMention(AllowedMention allowedMention) {
            this.allowedMentions.add(allowedMention);
            return this;
        }

        @Override
        public @NotNull Builder removeAllowedMention(AllowedMention allowedMention) {
            this.allowedMentions.remove(allowedMention);
            return this;
        }

        @Override
        public String getWebhookUsername() {
            return webhookUsername;
        }

        @Override
        public @NotNull BuilderImpl setWebhookUsername(String webhookUsername) {
            this.webhookUsername = webhookUsername;
            return this;
        }

        @Override
        public String getWebhookAvatarUrl() {
            return webhookAvatarUrl;
        }

        @Override
        public @NotNull BuilderImpl setWebhookAvatarUrl(String webhookAvatarUrl) {
            this.webhookAvatarUrl = webhookAvatarUrl;
            return this;
        }

        @Override
        public @NotNull SendableDiscordMessage build() {
            return new SendableDiscordMessageImpl(content, embeds, allowedMentions, webhookUsername, webhookAvatarUrl);
        }

        @Override
        public Formatter toFormatter() {
            return new FormatterImpl(
                    clone());
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public Builder clone() {
            BuilderImpl clone = new BuilderImpl();
            clone.setContent(content);
            embeds.forEach(clone::addEmbed);
            allowedMentions.forEach(clone::addAllowedMention);
            clone.setWebhookUsername(webhookUsername);
            clone.setWebhookAvatarUrl(webhookAvatarUrl);
            return clone;
        }
    }

    public static class FormatterImpl implements Formatter {

        private final Set<Object> context = new HashSet<>();
        private final Map<Pattern, Function<Matcher, Object>> replacements = new HashMap<>();

        private final SendableDiscordMessage.Builder builder;

        public FormatterImpl(SendableDiscordMessage.Builder builder) {
            this.builder = builder;
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
            DiscordSRVApi api = DiscordSRVApiProvider.optional().orElse(null);

            Function<String, String> placeholders = input -> {
                if (input == null) {
                    return null;
                }

                Placeholders placeholderUtil = new Placeholders(input)
                        .addAll(replacements);

                if (api != null) {
                    placeholderUtil = placeholderUtil.replaceAll(PlaceholderService.PATTERN,
                            wrapFunction(matcher ->
                                    api.placeholderService().getResultAsString(matcher, context)));
                }

                return placeholderUtil.get();
            };
            builder.setContent(placeholders.apply(builder.getContent()));

            List<DiscordMessageEmbed> embeds = new ArrayList<>(builder.getEmbeds());
            builder.getEmbeds().clear();

            for (DiscordMessageEmbed embed : embeds) {
                DiscordMessageEmbed.Builder embedBuilder = embed.toBuilder();

                // TODO: check which parts allow formatting more thoroughly
                PlaceholderResultStringifier.plainComponents(() -> {
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

            PlaceholderResultStringifier.plainComponents(() -> {
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
}
