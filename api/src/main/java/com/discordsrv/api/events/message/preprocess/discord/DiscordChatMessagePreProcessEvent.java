/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import org.apache.commons.collections4.list.SetUniqueList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Indicates that a Discord message has been received and will be processed by DiscordSRV (if not already processed or cancelled).
 * This event may be called multiple times for the same message if the message is being forwarded to multiple {@link GameChannel GameChannels}.
 * <p>
 * Order of events:
 * <ul>
 * <li>{@link com.discordsrv.api.events.message.preprocess.discord.DiscordChatMessagePreProcessEvent} (this event)</li>
 * <li>{@link com.discordsrv.api.events.message.postprocess.discord.DiscordChatMessagePostProcessEvent}</li>
 * <li>{@link com.discordsrv.api.events.message.post.discord.DiscordChatMessagePostEvent}</li>
 * </ul>
 */
public class DiscordChatMessagePreProcessEvent implements Cancellable, Processable.NoArgument {

    private final GameChannel gameChannel;
    private final ReceivedDiscordMessage message;
    private final List<Object> additionalContexts = SetUniqueList.setUniqueList(new ArrayList<>());
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

    /**
     * The {@link GameChannel} this message will be forwarded to.
     * @return the game channel
     */
    public GameChannel getGameChannel() {
        return gameChannel;
    }

    /**
     * The Discord message that will be processed.
     * @return the Discord message
     */
    @NotNull
    public ReceivedDiscordMessage getMessage() {
        return message;
    }

    /**
     * Additional contexts that will be passed to the PlaceholderService when formatting this message.
     * @return an unmodifiable list of contexts, not including ones provided by DiscordSRV
     */
    @NotNull
    @Unmodifiable
    public List<Object> getAdditionalContexts() {
        return Collections.unmodifiableList(additionalContexts);
    }

    /**
     * Add a PlaceholderService context for formatting this message.
     * @param context the context to add
     */
    public void addAdditionalContext(@NotNull Object context) {
        this.additionalContexts.add(context);
    }

    /**
     * Remove a PlaceholderService context for formatting this message.
     * @param context the context to remove
     */
    public void removeAdditionalContext(@NotNull Object context) {
        this.additionalContexts.remove(context);
    }

    /**
     * The content of the Discord message that will be passed to DiscordSRV for processing.
     * @return the Discord message content
     */
    public String getContent() {
        return content;
    }

    /**
     * Change the Discord message content that will be passed to DiscordSRV for processing.
     * @param content the new Discord message content
     */
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
