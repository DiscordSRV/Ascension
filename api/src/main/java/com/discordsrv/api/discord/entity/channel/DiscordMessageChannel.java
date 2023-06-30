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

package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A Discord channel that can send/receive messages.
 */
public interface DiscordMessageChannel extends DiscordChannel {

    /**
     * Sends the provided message to the channel.
     *
     * @param message the message to send to the channel
     * @return a future returning the message after being sent
     */
    @NotNull
    CompletableFuture<ReceivedDiscordMessage> sendMessage(@NotNull SendableDiscordMessage message);

    /**
     * Deletes the message identified by the id.
     *
     * @param id the id of the message to delete
     * @param webhookMessage if the message is a webhook message or not
     * @return a future that will fail if the request fails
     */
    CompletableFuture<Void> deleteMessageById(long id, boolean webhookMessage);

    /**
     * Edits the message identified by the id.
     *
     * @param id the id of the message to edit
     * @param message the new message content
     * @return a future returning the message after being edited
     */
    @NotNull
    CompletableFuture<ReceivedDiscordMessage> editMessageById(long id, @NotNull SendableDiscordMessage message);

    /**
     * Returns the JDA representation of this object. This should not be used if it can be avoided.
     * @return the JDA representation of this object
     * @see DiscordSRVApi#jda()
     */
    MessageChannel getAsJDAMessageChannel();
}
