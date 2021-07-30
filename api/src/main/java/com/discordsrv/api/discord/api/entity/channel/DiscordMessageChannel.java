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

package com.discordsrv.api.discord.api.entity.channel;

import com.discordsrv.api.discord.api.entity.Snowflake;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A Discord channel that can send/receive messages.
 */
public interface DiscordMessageChannel extends Snowflake {

    /**
     * Sends the provided message to the channel.
     * @param message the channel to send to the channel
     * @return a future returning the message after being sent
     * @throws com.discordsrv.api.discord.api.exception.NotReadyException if DiscordSRV is not ready, {@link com.discordsrv.api.DiscordSRVApi#isReady()}
     */
    @NotNull
    CompletableFuture<ReceivedDiscordMessage> sendMessage(SendableDiscordMessage message);

    /**
     * Edits the message identified by the id.
     * @param id the id of the message to edit
     * @param message the new message content
     * @return a future returning the message after being edited
     * @throws com.discordsrv.api.discord.api.exception.NotReadyException if DiscordSRV is not ready, {@link com.discordsrv.api.DiscordSRVApi#isReady()}
     */
    @NotNull
    CompletableFuture<ReceivedDiscordMessage> editMessageById(String id, SendableDiscordMessage message);

}
