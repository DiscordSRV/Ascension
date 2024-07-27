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

package com.discordsrv.api.events.message.process.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Processable;
import com.discordsrv.api.events.message.receive.discord.DiscordChatMessageReceiveEvent;

/**
 * Indicates that a Discord message is about to be processed, this will run once per {@link GameChannel} destination,
 * meaning it could run multiple times for a single Discord message. This runs after {@link DiscordChatMessageReceiveEvent}.
 */
public class DiscordChatMessageProcessEvent implements Cancellable, Processable.NoArgument {

    private final DiscordMessageChannel discordChannel;
    private final ReceivedDiscordMessage message;
    private String content;
    private final GameChannel destinationChannel;
    private boolean cancelled;
    private boolean processed;

    public DiscordChatMessageProcessEvent(
            DiscordMessageChannel discordChannel,
            ReceivedDiscordMessage message,
            GameChannel destinationChannel
    ) {
        this.discordChannel = discordChannel;
        this.message = message;
        this.content = message.getContent();
        this.destinationChannel = destinationChannel;
    }

    public DiscordMessageChannel getDiscordChannel() {
        return discordChannel;
    }

    public ReceivedDiscordMessage getMessage() {
        return message;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public GameChannel getDestinationChannel() {
        return destinationChannel;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    @Override
    public void markAsProcessed() {
        if (cancelled) {
            throw new IllegalStateException("Cannot process cancelled event");
        }
        if (processed) {
            throw new IllegalStateException("Cannot process already processed event");
        }
        this.processed = true;
    }
}
