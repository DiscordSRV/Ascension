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

package com.discordsrv.api.events.message.receive.discord;

import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.message.process.discord.DiscordChatMessageProcessEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates that a Discord message has been received and will be processed unless cancelled.
 * This runs once per Discord message, before {@link DiscordChatMessageProcessEvent}(s).
 */
public class DiscordChatMessageReceiveEvent implements Cancellable {

    private final ReceivedDiscordMessage message;
    private final DiscordMessageChannel channel;
    private boolean cancelled;

    public DiscordChatMessageReceiveEvent(@NotNull ReceivedDiscordMessage discordMessage, @NotNull DiscordMessageChannel channel) {
        this.message = discordMessage;
        this.channel = channel;
        if (!(channel instanceof DiscordTextChannel) && !(channel instanceof DiscordThreadChannel)) {
            throw new IllegalStateException("Cannot process messages that aren't from a text channel or thread");
        }
    }

    public ReceivedDiscordMessage getMessage() {
        return message;
    }

    public DiscordMessageChannel getChannel() {
        return channel;
    }

    public DiscordGuild getGuild() {
        if (channel instanceof DiscordTextChannel) {
            return ((DiscordTextChannel) channel).getGuild();
        } else if (channel instanceof DiscordThreadChannel) {
            return ((DiscordThreadChannel) channel).getParentChannel().getGuild();
        } else {
            throw new IllegalStateException("Message isn't from a text channel or thread");
        }
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
    public String toString() {
        return "DiscordChatMessageReceiveEvent{"
                + "message.author=" + message.getAuthor() + ", "
                + "channel=" + channel
                + '}';
    }
}
