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

package com.discordsrv.api.events.message.post.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.message.postprocess.discord.DiscordChatMessagePostProcessEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates that a message from Discord was forwarded to the provided {@link GameChannel}.
 * <p>
 * Order of events:
 * <ul>
 * <li>{@link com.discordsrv.api.events.message.preprocess.discord.DiscordChatMessagePreProcessEvent}</li>
 * <li>{@link com.discordsrv.api.events.message.postprocess.discord.DiscordChatMessagePostProcessEvent}</li>
 * <li>{@link com.discordsrv.api.events.message.post.discord.DiscordChatMessagePostEvent} (this event)</li>
 * </ul>
 */
public class DiscordChatMessagePostEvent implements Event {

    private final DiscordChatMessagePostProcessEvent preEvent;
    private final MinecraftComponent message;
    private final GameChannel channel;

    public DiscordChatMessagePostEvent(
            @NotNull DiscordChatMessagePostProcessEvent preEvent,
            @NotNull MinecraftComponent message,
            @NotNull GameChannel channel
    ) {
        this.preEvent = preEvent;
        this.message = message;
        this.channel = channel;
    }

    public DiscordChatMessagePostProcessEvent getPreEvent() {
        return preEvent;
    }

    public MinecraftComponent getMessage() {
        return message;
    }

    public GameChannel getChannel() {
        return channel;
    }
}
