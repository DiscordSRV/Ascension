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

package com.discordsrv.api.event.events.message.receive.discord;

import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.event.events.Cancellable;
import com.discordsrv.api.event.events.Processable;
import org.jetbrains.annotations.NotNull;

public class DiscordMessageProcessingEvent implements Cancellable, Processable {

    private final ReceivedDiscordMessage discordMessage;
    private String messageContent;
    private DiscordTextChannel channel;
    private boolean cancelled;
    private boolean processed;

    public DiscordMessageProcessingEvent(@NotNull ReceivedDiscordMessage discordMessage, @NotNull DiscordTextChannel channel) {
        this.discordMessage = discordMessage;
        this.messageContent = discordMessage.getContent().orElse(null);
        this.channel = channel;
    }

    public ReceivedDiscordMessage getDiscordMessage() {
        return discordMessage;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(@NotNull String messageContent) {
        this.messageContent = messageContent;
    }

    public DiscordTextChannel getChannel() {
        return channel;
    }

    public void setChannel(DiscordTextChannel channel) {
        this.channel = channel;
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
        this.processed = true;
    }

}
