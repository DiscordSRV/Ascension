/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A cluster of Discord messages, or just a single message.
 */
public interface ReceivedDiscordMessageCluster {

    /**
     * Gets the messages in this cluster.
     * @return the messages in this cluster
     */
    @NotNull
    Set<? extends ReceivedDiscordMessage> getMessages();

    /**
     * Deletes all the messages from this cluster, one request per message.
     * @return a future that fails if any of the requests fail.
     */
    @NotNull
    CompletableFuture<Void> deleteAll();

    /**
     * Edits all the messages in this cluster, one request per edit.
     * @param newMessage the new content of the messages
     * @return a future that fails if any of the requests fail.
     */
    @NotNull
    CompletableFuture<ReceivedDiscordMessageCluster> editAll(SendableDiscordMessage newMessage);

}
