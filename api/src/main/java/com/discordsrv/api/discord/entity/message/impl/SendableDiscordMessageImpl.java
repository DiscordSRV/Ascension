/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.entity.message.impl;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.entity.interaction.component.actionrow.MessageActionRow;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.util.DiscordFormattingUtil;
import com.discordsrv.api.placeholder.FormattedText;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.PlainPlaceholderFormat;
import com.discordsrv.api.placeholder.util.Placeholders;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendableDiscordMessageImpl implements SendableDiscordMessage {

    private final String content;
    private final List<DiscordMessageEmbed> embeds;
    private final List<MessageActionRow> actionRows;
    private final Set<AllowedMention> allowedMentions;
    private final String webhookUsername;
    private final String webhookAvatarUrl;
    private final Map<InputStream, String> attachments;
    private final boolean suppressedNotifications;
    private final boolean suppressedEmbeds;
    private final Long replyingToMessageId;

    protected SendableDiscordMessageImpl(
            String content,
            List<DiscordMessageEmbed> embeds,
            List<MessageActionRow> actionRows,
            Set<AllowedMention> allowedMentions,
            String webhookUsername,
            String webhookAvatarUrl,
            Map<InputStream, String> attachments,
            boolean suppressedNotifications,
            boolean suppressedEmbeds,
            Long replyingToMessageId
    ) {
        this.content = content;
        this.embeds = Collections.unmodifiableList(embeds);
        this.actionRows = Collections.unmodifiableList(actionRows);
        this.allowedMentions = Collections.unmodifiableSet(allowedMentions);
        this.webhookUsername = webhookUsername;
        this.webhookAvatarUrl = webhookAvatarUrl;
        this.attachments = Collections.unmodifiableMap(attachments);
        this.suppressedNotifications = suppressedNotifications;
        this.suppressedEmbeds = suppressedEmbeds;
        this.replyingToMessageId = replyingToMessageId;
    }

    public SendableDiscordMessageImpl withReplyingToMessageId(Long replyingToMessageId) {
        return new SendableDiscordMessageImpl(
                content,
                embeds,
                actionRows,
                allowedMentions,
                webhookUsername,
                webhookAvatarUrl,
                attachments,
                suppressedNotifications,
                suppressedEmbeds,
                replyingToMessageId
        );
    }

    @Override
    public boolean isEmpty() {
        return (content == null || content.isEmpty()) && embeds.isEmpty() && attachments.isEmpty() && actionRows.isEmpty();
    }

    @Override
    public @Nullable String getContent() {
        return content;
    }

    @Override
    public @NotNull List<DiscordMessageEmbed> getEmbeds() {
        return embeds;
    }

    @NotNull
    @Override
    public List<MessageActionRow> getActionRows() {
        return actionRows;
    }

    @Override
    public @NotNull Set<AllowedMention> getAllowedMentions() {
        return allowedMentions;
    }

    @Override
    public @Nullable String getWebhookUsername() {
        return webhookUsername;
    }

    @Override
    public @Nullable String getWebhookAvatarUrl() {
        return webhookAvatarUrl;
    }

    @Override
    public boolean isSuppressedNotifications() {
        return suppressedNotifications;
    }

    @Override
    public boolean isSuppressedEmbeds() {
        return suppressedEmbeds;
    }

    @Override
    public Long getMessageIdToReplyTo() {
        return replyingToMessageId;
    }

    @Override
    public Map<InputStream, String> getAttachments() {
        return attachments;
    }

    public static class BuilderImpl implements SendableDiscordMessage.Builder {

        private String content;
        private final List<DiscordMessageEmbed> embeds = new ArrayList<>();
        private final List<MessageActionRow> actionRows = new ArrayList<>();
        private final Set<AllowedMention> allowedMentions = new LinkedHashSet<>();
        private String webhookUsername;
        private String webhookAvatarUrl;
        private final Map<InputStream, String> attachments = new LinkedHashMap<>();
        private boolean suppressedNotifications;
        private boolean suppressedEmbeds;
        private Long replyingToMessageId;

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
            return Collections.unmodifiableList(embeds);
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
        public List<MessageActionRow> getActionRows() {
            return actionRows;
        }

        @Override
        public Builder setActionRows(MessageActionRow... rows) {
            this.actionRows.clear();
            this.actionRows.addAll(Arrays.asList(rows));
            return this;
        }

        @Override
        public Builder addActionRow(MessageActionRow row) {
            this.actionRows.add(row);
            return this;
        }

        @Override
        public Builder removeActionRow(MessageActionRow row) {
            this.actionRows.remove(row);
            return this;
        }

        @Override
        public @NotNull Set<AllowedMention> getAllowedMentions() {
            return allowedMentions;
        }

        @Override
        public @NotNull Builder setAllowedMentions(@NotNull Collection<AllowedMention> allowedMentions) {
            this.allowedMentions.clear();
            this.allowedMentions.addAll(allowedMentions);
            return this;
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
        public Builder addAttachment(InputStream inputStream, String fileName) {
            this.attachments.put(inputStream, fileName);
            return this;
        }

        @Override
        public boolean isEmpty() {
            return (content == null || content.isEmpty()) && embeds.isEmpty() && attachments.isEmpty() && actionRows.isEmpty();
        }

        @Override
        public Builder setSuppressedNotifications(boolean suppressedNotifications) {
            this.suppressedNotifications = suppressedNotifications;
            return this;
        }

        @Override
        public boolean isSuppressedEmbeds() {
            return suppressedEmbeds;
        }

        @Override
        public Builder setMessageIdToReplyTo(Long messageId) {
            replyingToMessageId = messageId;
            return this;
        }

        @Override
        public Long getMessageIdToReplyTo() {
            return replyingToMessageId;
        }

        @Override
        public Builder setSuppressedEmbeds(boolean suppressedEmbeds) {
            this.suppressedEmbeds = suppressedEmbeds;
            return this;
        }

        @Override
        public boolean isSuppressedNotifications() {
            return suppressedNotifications;
        }

        @Override
        public @NotNull SendableDiscordMessage build() {
            return new SendableDiscordMessageImpl(content, embeds, actionRows, allowedMentions, webhookUsername, webhookAvatarUrl, attachments, suppressedNotifications, suppressedEmbeds, replyingToMessageId);
        }

        @Override
        public Formatter toFormatter() {
            return new FormatterImpl(clone());
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
        private final Map<Pattern, Function<Matcher, Object>> replacements = new LinkedHashMap<>();

        private final SendableDiscordMessage.Builder builder;

        public FormatterImpl(SendableDiscordMessage.Builder builder) {
            this.builder = builder;
        }

        @Override
        public SendableDiscordMessage.@NotNull Formatter addContext(Object... context) {
            this.context.addAll(Arrays.asList(context));
            return this;
        }

        @Override
        public SendableDiscordMessage.@NotNull Formatter addReplacement(Pattern target, Function<Matcher, Object> replacement) {
            this.replacements.put(target, wrapFunction(replacement));
            return this;
        }

        @Override
        public @NotNull Formatter applyPlaceholderService() {
            DiscordSRVApi api = DiscordSRVApi.get();
            if (api == null) {
                throw new IllegalStateException("DiscordSRVApi not available");
            }
            this.replacements.put(
                    PlaceholderService.PATTERN,
                    wrapFunction(matcher -> api.placeholderService().getResultAsPlain(matcher, context))
            );
            return this;
        }

        private Function<Matcher, Object> wrapFunction(Function<Matcher, Object> function) {
            return matcher -> {
                Object result = function.apply(matcher);
                if (result instanceof FormattedText || PlainPlaceholderFormat.FORMATTING.get() != PlainPlaceholderFormat.Formatting.DISCORD) {
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

        @Override
        public @NotNull SendableDiscordMessage build() {
            DiscordSRVApi api = DiscordSRVApi.get();
            if (api == null) {
                throw new IllegalStateException("DiscordSRVApi not available");
            }

            Function<String, String> placeholders = input -> {
                if (input == null) {
                    return null;
                }

                Placeholders placeholderUtil = new Placeholders(input)
                        .addAll(replacements);

                // Empty string -> null (so we don't provide empty strings to random fields)
                String output = placeholderUtil.toString();
                return output.isEmpty() ? null : output;
            };
            Function<String, String> discordPlaceholders = input -> {
                if (input == null) {
                    return null;
                }

                // Empty string -> null (so we don't provide empty strings to random fields)
                String output = api.discordPlaceholders().map(input, in -> {
                    // Since this will be processed in parts, we don't want parts to return null, only the full output
                    String out = placeholders.apply(in);
                    return out == null ? "" : out;
                });
                return output.isEmpty() ? null : output;
            };


            PlainPlaceholderFormat.with(
                    PlainPlaceholderFormat.Formatting.DISCORD,
                    () -> builder.setContent(discordPlaceholders.apply(builder.getContent()))
            );

            List<DiscordMessageEmbed> embeds = new ArrayList<>(builder.getEmbeds());
            embeds.forEach(builder::removeEmbed);

            for (DiscordMessageEmbed embed : embeds) {
                DiscordMessageEmbed.Builder embedBuilder = embed.toBuilder();

                embedBuilder.setAuthor(
                        cutToLength(
                                placeholders.apply(embedBuilder.getAuthorName()),
                                MessageEmbed.AUTHOR_MAX_LENGTH
                        ),
                        placeholders.apply(embedBuilder.getAuthorUrl()),
                        placeholders.apply(embedBuilder.getAuthorImageUrl()));

                embedBuilder.setTitle(
                        cutToLength(
                                placeholders.apply(embedBuilder.getTitle()),
                                MessageEmbed.TITLE_MAX_LENGTH
                        ),
                        placeholders.apply(embedBuilder.getTitleUrl())
                );

                embedBuilder.setThumbnailUrl(
                        placeholders.apply(embedBuilder.getThumbnailUrl())
                );

                embedBuilder.setImageUrl(
                        placeholders.apply(embedBuilder.getImageUrl())
                );

                embedBuilder.setFooter(
                        cutToLength(
                                placeholders.apply(embedBuilder.getFooter()),
                                MessageEmbed.TEXT_MAX_LENGTH
                        ),
                        placeholders.apply(embedBuilder.getFooterImageUrl())
                );

                PlainPlaceholderFormat.with(PlainPlaceholderFormat.Formatting.DISCORD, () -> embedBuilder.setDescription(
                        cutToLength(
                                discordPlaceholders.apply(embedBuilder.getDescription()),
                                MessageEmbed.DESCRIPTION_MAX_LENGTH
                        )
                ));

                List<DiscordMessageEmbed.Field> fields = new ArrayList<>(embedBuilder.getFields());
                embedBuilder.getFields().clear();

                PlainPlaceholderFormat.with(PlainPlaceholderFormat.Formatting.DISCORD, () ->
                        fields.forEach(field -> embedBuilder.addField(
                                cutToLength(
                                        placeholders.apply(field.getTitle()),
                                        MessageEmbed.TITLE_MAX_LENGTH
                                ),
                                cutToLength(
                                        placeholders.apply(field.getValue()),
                                        MessageEmbed.VALUE_MAX_LENGTH
                                ),
                                field.isInline()
                        ))
                );

                builder.addEmbed(embedBuilder.build());
            }

            builder.setWebhookUsername(placeholders.apply(builder.getWebhookUsername()));
            builder.setWebhookAvatarUrl(placeholders.apply(builder.getWebhookAvatarUrl()));

            return builder.build();
        }

        private String cutToLength(String input, int maxLength) {
            if (input == null) {
                return null;
            }
            if (input.length() > maxLength) {
                return input.substring(0, maxLength);
            }
            return input;
        }
    }
}
