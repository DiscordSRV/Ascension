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

package com.discordsrv.api.discord.api.message;

import com.discordsrv.api.discord.api.message.impl.SendableDiscordMessageImpl;

import java.util.List;

@SuppressWarnings("unused") // API
public interface SendableDiscordMessage {

    static Builder builder() {
        return new SendableDiscordMessageImpl.BuilderImpl();
    }

    /**
     * The raw content of the message.
     * @return the unmodified content of the message
     */
    String getContent();

    /**
     * Gets the embeds of the message.
     * @return the unmodifiable list of embeds in this message
     */
    List<DiscordMessageEmbed> getEmbeds();

    /**
     * Gets the webhook username.
     * @return the webhook username or {@code null} if this isn't a webhook message
     */
    String getWebhookUsername();

    /**
     * Gets the webhook avatar url.
     * @return the webhook avatar url or {@code null} if no webhook avatar url is specified
     */
    String getWebhookAvatarUrl();

    /**
     * Returns true if the {@link #getWebhookUsername() webhook username} is specified.
     * @return true if this is a webhook message
     */
    default boolean isWebhookMessage() {
        return getWebhookUsername() != null;
    }

    interface Builder {

        /**
         * Gets the current content of this message in this builder.
         * @return the content
         */
        String getContent();

        /**
         * Changes the content of this builder.
         * @param content the new content
         * @return the builder, useful for chaining
         */
        Builder setContent(String content);

        /**
         * Gets the embeds that are currently in this builder.
         * @return this builder's current embeds
         */
        List<DiscordMessageEmbed> getEmbeds();

        /**
         * Adds an embed to this builder.
         * @param embed the embed to add
         * @return the builder, useful for chaining
         */
        Builder addEmbed(DiscordMessageEmbed embed);

        /**
         * Removes an embed from this builder.
         * @param embed the embed to remove
         * @return the builder, useful for chaining
         */
        Builder removeEmbed(DiscordMessageEmbed embed);

        /**
         * Gets the webhook username for this builder or {@code null} if webhooks are not being used.
         * @return the webhook username
         */
        String getWebhookUsername();

        /**
         * Sets the webhook username, setting this enabled webhooks for this message.
         * @param webhookUsername the new webhook username
         * @return the builder, useful for chaining
         */
        Builder setWebhookUsername(String webhookUsername);

        /**
         * Gets the webhook avatar url for this builder.
         * @return the webhook avatar url
         */
        String getWebhookAvatarUrl();

        /**
         * Sets the webhook avatar url for this builder.
         * @param webhookAvatarUrl the new webhook avatar url
         * @throws IllegalStateException if there is no webhook username set
         * @return the builder, useful for chaining
         */
        Builder setWebhookAvatarUrl(String webhookAvatarUrl);

        /**
         * Builds a {@link SendableDiscordMessage} from this builder.
         * @return the new {@link SendableDiscordMessage}
         */
        SendableDiscordMessage build();
    }


}
