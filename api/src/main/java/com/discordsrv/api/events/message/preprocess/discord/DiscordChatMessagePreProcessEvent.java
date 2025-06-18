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

package com.discordsrv.api.events.message.preprocess.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Processable;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates that a Discord message has been received and will be processed unless cancelled.
 * <p>
 * Order of events:
 * <li> {@link com.discordsrv.api.events.message.preprocess.discord.DiscordChatMessagePreProcessEvent} (this event)
 * <li> {@link com.discordsrv.api.events.message.postprocess.discord.DiscordChatMessagePostProcessEvent}
 * <li> {@link com.discordsrv.api.events.message.post.discord.DiscordChatMessagePostEvent}
 */
public class DiscordChatMessagePreProcessEvent implements Cancellable, Processable.NoArgument {

    private final GameChannel gameChannel;
    private final ReceivedDiscordMessage message;
    private String content;
    private boolean cancelled;
    private boolean processed;

    public DiscordChatMessagePreProcessEvent(
            @NotNull GameChannel gameChannel,
            @NotNull ReceivedDiscordMessage discordMessage
    ) {
        this.gameChannel = gameChannel;
        this.message = discordMessage;
        this.content = discordMessage.getContent();
    }

    public GameChannel getGameChannel() {
        return gameChannel;
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

    public DiscordMessageChannel getChannel() {
        return message.getChannel();
    }

    public DiscordGuild getGuild() {
        return message.getGuild();
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
    public void markAsProcessed() {
        Processable.NoArgument.super.markAsProcessed();
        this.processed = true;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    @Override
    public String toString() {
        return "DiscordChatMessageReceiveEvent{"
                + "message.author=" + message.getAuthor() + ", "
                + "gameChannel=" + GameChannel.toString(gameChannel)
                + '}';
    }
}
