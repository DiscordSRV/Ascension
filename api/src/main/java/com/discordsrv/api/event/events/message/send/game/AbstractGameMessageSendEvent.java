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

package com.discordsrv.api.event.events.message.send.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.event.events.Cancellable;
import com.discordsrv.api.event.events.Processable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractGameMessageSendEvent implements Cancellable, Processable {

    private String discordMessage;
    private String discordUsername;
    private GameChannel targetChannel;
    private boolean cancelled;
    private boolean processed;

    public AbstractGameMessageSendEvent(@NotNull String discordMessage, @Nullable String discordUsername, @NotNull GameChannel targetChannel) {
        this.discordMessage = discordMessage;
        this.discordUsername = discordUsername;
        this.targetChannel = targetChannel;
    }

    @NotNull
    public String getDiscordMessage() {
        return discordMessage;
    }

    public void setDiscordMessage(@NotNull String discordMessage) {
        this.discordMessage = discordMessage;
    }

    @Nullable
    public String getDiscordUsername() {
        return discordUsername;
    }

    public void setDiscordUsername(@Nullable String discordUsername) {
        this.discordUsername = discordUsername;
    }

    @NotNull
    public GameChannel getTargetChannel() {
        return targetChannel;
    }

    public void setTargetChannel(@NotNull GameChannel targetChannel) {
        this.targetChannel = targetChannel;
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
