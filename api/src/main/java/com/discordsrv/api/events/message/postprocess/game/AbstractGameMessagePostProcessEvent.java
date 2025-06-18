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

package com.discordsrv.api.events.message.postprocess.game;

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.message.preprocess.game.AbstractGameMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

public abstract class AbstractGameMessagePostProcessEvent<PE extends AbstractGameMessagePreProcessEvent>
        implements Event, Cancellable {

    private final PE preEvent;
    private final DiscordSRVPlayer player;
    private final List<DiscordGuildMessageChannel> channels;
    private SendableDiscordMessage message;
    private boolean cancelled;

    public AbstractGameMessagePostProcessEvent(
            @NotNull PE preEvent,
            @Nullable DiscordSRVPlayer player,
            @NotNull List<DiscordGuildMessageChannel> channels,
            @NotNull SendableDiscordMessage message
    ) {
        this.preEvent = preEvent;
        this.player = player;
        this.channels = Collections.unmodifiableList(channels);
        this.message = message;
    }

    @NotNull
    public PE getPreEvent() {
        return preEvent;
    }

    @NotNull
    public DiscordSRVPlayer getPlayer() {
        return player;
    }

    @NotNull
    @Unmodifiable
    public List<DiscordGuildMessageChannel> getChannels() {
        return channels;
    }

    @NotNull
    public SendableDiscordMessage getMessage() {
        return message;
    }

    public void setMessage(@NotNull SendableDiscordMessage message) {
        this.message = message;
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
