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

package com.discordsrv.api.discord.api.entity.message;

import com.discordsrv.api.discord.api.entity.message.impl.SendableDiscordMessageImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A message that can be sent to Discord.
 */
@SuppressWarnings("unused") // API
public interface SendableDiscordMessage {

    /**
     * Creates a new builder for {@link SendableDiscordMessage}.
     * @return a new builder
     */
    @NotNull
    static Builder builder() {
        return new SendableDiscordMessageImpl.BuilderImpl();
    }

    /**
     * The raw content of the message.
     * @return the unmodified content of the message
     */
    @NotNull
    Optional<String> getContent();

    /**
     * Gets the embeds of the message.
     * @return the unmodifiable list of embeds in this message
     */
    @NotNull
    List<DiscordMessageEmbed> getEmbeds();

    /**
     * Gets the allowed mentions of the message.
     * @return the allowed mentions in this message
     */
    @NotNull
    Set<AllowedMention> getAllowedMentions();

    /**
     * Gets the webhook username.
     * @return the webhook username or {@code null} if this isn't a webhook message
     */
    @NotNull
    Optional<String> getWebhookUsername();

    /**
     * Gets the webhook avatar url.
     * @return the webhook avatar url or {@code null} if no webhook avatar url is specified
     */
    @NotNull
    Optional<String> getWebhookAvatarUrl();

    /**
     * Returns true if the {@link #getWebhookUsername() webhook username} is specified.
     * @return true if this is a webhook message
     */
    default boolean isWebhookMessage() {
        return getWebhookUsername().isPresent();
    }

    @SuppressWarnings("UnusedReturnValue") // API
    interface Builder {

        /**
         * Gets the current content of this message in this builder.
         * @return the content
         */
        @Nullable
        String getContent();

        /**
         * Changes the content of this builder.
         * @param content the new content
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder setContent(String content);

        /**
         * Gets the embeds that are currently in this builder.
         * @return this builder's current embeds
         */
        @NotNull
        List<DiscordMessageEmbed> getEmbeds();

        /**
         * Adds an embed to this builder.
         * @param embed the embed to add
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder addEmbed(DiscordMessageEmbed embed);

        /**
         * Removes an embed from this builder.
         * @param embed the embed to remove
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder removeEmbed(DiscordMessageEmbed embed);

        /**
         * Gets the allowed mentions in this builder.
         * @return the builder's current allowed mentions
         */
        @Nullable
        Set<AllowedMention> getAllowedMentions();

        /**
         * Adds an allowed mention to this builder.
         * @param allowedMention the allowed mention to add
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder addAllowedMention(AllowedMention allowedMention);

        /**
         * Removes an allowed mention from this builder.
         * @param allowedMention the allowed mention to remove
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder removeAllowedMention(AllowedMention allowedMention);

        /**
         * Gets the webhook username for this builder or {@code null} if webhooks are not being used.
         * @return the webhook username
         */
        @Nullable
        String getWebhookUsername();

        /**
         * Sets the webhook username, setting this enabled webhooks for this message.
         * @param webhookUsername the new webhook username
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder setWebhookUsername(String webhookUsername);

        /**
         * Gets the webhook avatar url for this builder.
         * @return the webhook avatar url
         */
        @Nullable
        String getWebhookAvatarUrl();

        /**
         * Sets the webhook avatar url for this builder.
         * @param webhookAvatarUrl the new webhook avatar url
         * @throws IllegalStateException if there is no webhook username set
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder setWebhookAvatarUrl(String webhookAvatarUrl);

        @NotNull
        Builder convertToNonWebhook();

        /**
         * Builds a {@link SendableDiscordMessage} from this builder.
         * @return the new {@link SendableDiscordMessage}
         */
        @NotNull
        SendableDiscordMessage build();

        /**
         * Creates a new formatter with a clone of this {@link Builder}.
         * @return the new {@link Formatter}
         */
        Formatter toFormatter();

        /**
         * Creates a copy of this {@link Builder}.
         * @return a copy of this builder
         */
        Builder clone();
    }

    /**
     * Discord equivalent for {@link com.discordsrv.api.component.EnhancedTextBuilder}.
     */
    interface Formatter {

        /**
         * Adds context for replacing placeholders via DiscordSRV's {@link com.discordsrv.api.placeholder.PlaceholderService}.
         * @param context the context to add
         * @return the formatted, useful for chaining
         */
        @NotNull
        Formatter addContext(Object... context);

        @NotNull
        default Formatter addReplacement(String target, Object replacement) {
            return addReplacement(Pattern.compile(target, Pattern.LITERAL), replacement);
        }

        @NotNull
        default Formatter addReplacement(Pattern target, Object replacement) {
            return addReplacement(target, matcher -> replacement);
        }

        @NotNull
        default Formatter addReplacement(String target, Supplier<Object> replacement) {
            return addReplacement(Pattern.compile(target, Pattern.LITERAL), replacement);
        }

        @NotNull
        default Formatter addReplacement(Pattern target, Supplier<Object> replacement) {
            return addReplacement(target, matcher -> replacement.get());
        }

        @NotNull
        default Formatter addReplacement(String target, Function<Matcher, Object> replacement) {
            return addReplacement(Pattern.compile(target, Pattern.LITERAL), replacement);
        }

        @NotNull
        Formatter addReplacement(Pattern target, Function<Matcher, Object> replacement);

        @NotNull
        Formatter applyPlaceholderService();

        @NotNull
        Formatter convertToNonWebhook();

        @NotNull
        SendableDiscordMessage build();
    }

}
