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

package com.discordsrv.api.events.message.render;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.PlayerEvent;
import com.discordsrv.api.events.Processable;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameChatRenderEvent implements PlayerEvent, Processable.Argument<MinecraftComponent>, Cancellable {

    private final Object triggeringEvent;
    private final DiscordSRVPlayer player;
    private final GameChannel channel;
    private final MinecraftComponent message;
    private MinecraftComponent annotatedMessage;
    private boolean cancelled = false;

    public GameChatRenderEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @NotNull GameChannel channel,
            @NotNull MinecraftComponent message
    ) {
        this.triggeringEvent = triggeringEvent;
        this.player = player;
        this.channel = channel;
        this.message = message;
    }

    public Object getTriggeringEvent() {
        return triggeringEvent;
    }

    @Override
    public @NotNull DiscordSRVPlayer getPlayer() {
        return player;
    }

    public GameChannel getChannel() {
        return channel;
    }

    public MinecraftComponent getMessage() {
        return message;
    }

    public MinecraftComponent getAnnotatedMessage() {
        return annotatedMessage;
    }

    @Override
    public boolean isProcessed() {
        return annotatedMessage != null;
    }

    @Override
    public void process(MinecraftComponent annotatedMessage) {
        if (isProcessed()) {
            throw new IllegalStateException("Already processed");
        }
        this.annotatedMessage = annotatedMessage;
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
