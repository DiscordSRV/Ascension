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

package com.discordsrv.api.events.message.postprocess.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.message.preprocess.discord.DiscordChatMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <p>
 * Order of events:
 * <li> {@link com.discordsrv.api.events.message.preprocess.discord.DiscordChatMessagePreProcessEvent}
 * <li> {@link com.discordsrv.api.events.message.postprocess.discord.DiscordChatMessagePostProcessEvent} (this event)
 * <li> {@link com.discordsrv.api.events.message.post.discord.DiscordChatMessagePostEvent}
 */
public class DiscordChatMessagePostProcessEvent implements Event, Cancellable {

    private final DiscordChatMessagePreProcessEvent preEvent;
    private final GameChannel gameChannel;
    private MinecraftComponent message;
    private final List<DiscordSRVPlayer> recipients;
    private boolean cancelled;

    public DiscordChatMessagePostProcessEvent(
            @NotNull DiscordChatMessagePreProcessEvent preEvent,
            @NotNull GameChannel gameChannel,
            @NotNull MinecraftComponent message,
            @Nullable List<DiscordSRVPlayer> recipients
    ) {
        this.preEvent = preEvent;
        this.gameChannel = gameChannel;
        this.message = message;
        this.recipients = recipients;
    }

    @NotNull
    public DiscordChatMessagePreProcessEvent getPreEvent() {
        return preEvent;
    }

    @NotNull
    public GameChannel getGameChannel() {
        return gameChannel;
    }

    @NotNull
    public MinecraftComponent getMessage() {
        return message;
    }

    public void setMessage(@NotNull MinecraftComponent message) {
        this.message = message;
    }

    /**
     * Returns the list of recipients, in a modifiable list. This will be {@code null} if recipients cannot be modified.
     * @return the modifiable list of recipients or {@code null}
     */
    @Nullable
    public List<DiscordSRVPlayer> getRecipients() {
        return recipients;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
