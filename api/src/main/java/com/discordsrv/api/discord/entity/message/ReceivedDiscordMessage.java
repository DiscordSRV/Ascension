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

package com.discordsrv.api.discord.entity.message;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.Snowflake;
import com.discordsrv.api.discord.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A message received from Discord.
 */
public interface ReceivedDiscordMessage extends Snowflake {

    /**
     * Gets the content of this message. This will return {@code null} if the bot doesn't have the MESSAGE_CONTENT intent,
     * and this message was not from the bot, did not mention the bot and was not a direct message.
     * @return the message content or {@code null}
     */
    @Nullable
    String getContent();

    /**
     * Gets the embeds of this message.
     * @return the message embeds
     */
    @NotNull
    @Unmodifiable
    List<DiscordMessageEmbed> getEmbeds();

    /**
     * Returns {@code true} if this is a webhook message, {@link #getAuthor()} to get webhook username or avatar url.
     * @return {@code true} if this is a webhook message
     */
    boolean isWebhookMessage();

    /**
     * Gets the URL to jump to this message.
     * @return the jump url
     */
    @NotNull
    @Placeholder("message_jump_url")
    String getJumpUrl();

    /**
     * Gets the attachments of this message.
     * @return this message's attachments
     */
    @NotNull
    List<Attachment> getAttachments();

    /**
     * Determines if this message was sent by this DiscordSRV instance's Discord bot,
     * or a webhook being used by this DiscordSRV instance.
     * @return true if this message was sent by this DiscordSRV instance
     */
    boolean isFromSelf();

    /**
     * Gets the user that sent the message.
     * @return the user that sent the message
     */
    @NotNull
    DiscordUser getAuthor();

    /**
     * Gets the channel the message was sent in.
     * @return the channel the message was sent in
     */
    @NotNull
    DiscordMessageChannel getChannel();

    /**
     * Gets the messages this message is replying to.
     * @return the messages this message is replying to or a empty optional
     */
    @Nullable
    ReceivedDiscordMessage getReplyingTo();

    /**
     * Gets the text channel the message was sent in. Not present if this message is a dm.
     * @return an optional potentially containing the text channel the message was sent in
     */
    @Nullable
    DiscordTextChannel getTextChannel();

    /**
     * Gets the dm channel the message was sent in. Not present if this message was sent in a server.
     * @return an optional potentially containing the dm channel the message was sent in
     */
    @Nullable
    DiscordDMChannel getDMChannel();

    /**
     * Gets the Discord server member that sent this message.
     * This is not present if the message was sent by a webhook.
     * @return an optional potentially containing the Discord server member that sent this message
     */
    @Nullable
    DiscordGuildMember getMember();

    /**
     * Gets the Discord server the message was posted in. This is not present if the message was a dm.
     * @return an optional potentially containing the Discord server the message was posted in
     */
    @Nullable
    default DiscordGuild getGuild() {
        DiscordTextChannel textChannel = getTextChannel();

        return textChannel != null ? textChannel.getGuild() : null;
    }

    /**
     * Deletes this message.
     *
     * @return a future that will fail if the request fails
     */
    @NotNull
    CompletableFuture<Void> delete();

    /**
     * Edits this message to the provided message.
     *
     * @param message the new message
     * @return a future that will fail if the request fails, otherwise the new message provided by the request response
     * @throws IllegalArgumentException if the message is not a webhook message,
     * but the provided {@link SendableDiscordMessage} specifies a webhook username.
     */
    CompletableFuture<ReceivedDiscordMessage> edit(@NotNull SendableDiscordMessage message);

    class Attachment {

        private final String fileName;
        private final String url;
        private final String proxyUrl;
        private final int sizeBytes;

        public Attachment(String fileName, String url, String proxyUrl, int sizeBytes) {
            this.fileName = fileName;
            this.url = url;
            this.proxyUrl = proxyUrl;
            this.sizeBytes = sizeBytes;
        }

        public String fileName() {
            return fileName;
        }

        public String url() {
            return url;
        }

        public String proxyUrl() {
            return proxyUrl;
        }

        public int sizeBytes() {
            return sizeBytes;
        }
    }
}
