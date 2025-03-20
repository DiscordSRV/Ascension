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

package com.discordsrv.api.discord.entity.message;

import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.discord.entity.interaction.component.actionrow.MessageActionRow;
import com.discordsrv.api.discord.entity.message.impl.SendableDiscordMessageImpl;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A message that can be sent to Discord.
 */
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
    @Nullable
    String getContent();

    /**
     * Gets the embeds of the message.
     * @return the unmodifiable list of embeds in this message
     */
    @NotNull
    @Unmodifiable
    List<DiscordMessageEmbed> getEmbeds();

    /**
     * Gets the action rows.
     * @return an unmodifiable list of action rows in this message
     */
    @NotNull
    @Unmodifiable
    List<MessageActionRow> getActionRows();

    /**
     * Gets the allowed mentions of the message.
     * @return the unmodifiable list of allowed mentions in this message
     */
    @NotNull
    @Unmodifiable
    Set<AllowedMention> getAllowedMentions();

    /**
     * Gets the webhook username.
     * @return the webhook username or {@code null} if this isn't a webhook message
     */
    @Nullable
    String getWebhookUsername();

    /**
     * Gets the webhook avatar url.
     * @return the webhook avatar url or {@code null} if no webhook avatar url is specified
     */
    @Nullable
    String getWebhookAvatarUrl();

    /**
     * Returns true if the {@link #getWebhookUsername() webhook username} is specified.
     * @return true if this is a webhook message
     */
    default boolean isWebhookMessage() {
        return getWebhookUsername() != null;
    }

    /**
     * Gets the raw inputs streams and file names for attachments, for this message.
     * @return the map of input streams to file names
     */
    Map<InputStream, String> getAttachments();

    /**
     * If notifications for this message are suppressed.
     * @return if sending this message doesn't cause a notification
     */
    boolean isSuppressedNotifications();

    /**
     * If embeds for this message are suppressed.
     * @return if embeds for this message are suppressed
     */
    boolean isSuppressedEmbeds();

    /**
     * Gets the id for the message this message is in reply to
     * @return the message id
     */
    Long getMessageIdToReplyTo();

    /**
     * Creates a copy of this {@link SendableDiscordMessage} with the specified reply message id.
     *
     * @param replyingToMessageId the reply message id
     * @return a new {@link SendableDiscordMessage} identical to the current instance except for the reply message id
     */
    SendableDiscordMessage withReplyingToMessageId(Long replyingToMessageId);

    /**
     * Checks if this message has any sendable content.
     * @return {@code true} if there is no sendable content
     */
    boolean isEmpty();

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
         * Gets the action rows for this builder.
         * @return the action rows
         */
        List<MessageActionRow> getActionRows();

        /**
         * Sets the action rows for this builder.
         * @param rows the action rows
         * @return the builder, useful for chaining
         */
        Builder setActionRows(MessageActionRow... rows);

        /**
         * Adds an action row to this builder.
         * @param row the action row
         * @return the builder, useful for chaining
         */
        Builder addActionRow(MessageActionRow row);

        /**
         * Removes an action row from this builder.
         * @param row the action row
         * @return the builder, useful for chaining
         */
        Builder removeActionRow(MessageActionRow row);

        /**
         * Gets the allowed mentions in this builder.
         * @return the builder's current allowed mentions
         */
        @NotNull
        Set<AllowedMention> getAllowedMentions();

        /**
         * Sets the allowed mentions in for this builder.
         *
         * @param allowedMentions the allowed mentions
         * @return the builder, useful for chaining
         */
        @NotNull
        Builder setAllowedMentions(@NotNull Collection<AllowedMention> allowedMentions);

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

        /**
         * Adds an attachment to this builder.
         * @param inputStream an input stream containing the file contents
         * @param fileName the name of the file
         * @return the builder, useful for chaining
         */
        Builder addAttachment(InputStream inputStream, String fileName);

        /**
         * Sets if this message's notifications will be suppressed.
         * @param suppressedNotifications if notifications should be suppressed
         * @return this builder, useful for chaining
         */
        Builder setSuppressedNotifications(boolean suppressedNotifications);

        /**
         * Checks if this builder has notifications suppressed.
         * @return {@code true} if notifications should be suppressed for this message
         */
        boolean isSuppressedNotifications();

        /**
         * Sets if this message's embeds will be suppressed.
         * @param suppressedEmbeds if embeds should be suppressed
         * @return this builder, useful for chaining
         */
        Builder setSuppressedEmbeds(boolean suppressedEmbeds);

        /**
         * Checks if this builder has embeds suppressed.
         * @return {@code true} if embeds should be suppressed for this message
         */
        boolean isSuppressedEmbeds();

        /**
         * Sets the message this message should be in reply to.
         * @param messageId the id for the message this is in reply to
         * @return this builder, useful for chaining
         */
        Builder setMessageIdToReplyTo(Long messageId);

        /**
         * Sets the message this message should be in reply to.
         * @param message the message this is in reply to
         * @return this builder, useful for chaining
         */
        default Builder setMessageToReplyTo(@NotNull ReceivedDiscordMessage message) {
            return setMessageIdToReplyTo(message.getId());
        }

        /**
         * Gets the id for the message this message is in reply to
         * @return the message id
         */
        Long getMessageIdToReplyTo();

        /**
         * Checks if this builder has any sendable content.
         * @return {@code true} if there is no sendable content
         */
        boolean isEmpty();

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
     * Discord equivalent for {@link GameTextBuilder}.
     */
    interface Formatter {

        /**
         * Adds context for replacing placeholders via DiscordSRV's {@link com.discordsrv.api.placeholder.PlaceholderService}.
         * @param context the context to add
         * @return the formatted, useful for chaining
         */
        @NotNull
        Formatter addContext(Object... context);

        default Formatter addPlaceholder(String placeholder, Object replacement) {
            return addContext(new SinglePlaceholder(placeholder, replacement));
        }

        default Formatter addPlaceholder(String placeholder, Supplier<Object> replacementSupplier) {
            return addContext(new SinglePlaceholder(placeholder, replacementSupplier));
        }

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
        SendableDiscordMessage build();
    }

}
