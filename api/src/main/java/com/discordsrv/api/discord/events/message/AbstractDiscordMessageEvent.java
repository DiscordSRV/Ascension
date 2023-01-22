/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.events.message;

import com.discordsrv.api.discord.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.events.AbstractDiscordEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDiscordMessageEvent<T extends GenericMessageEvent> extends AbstractDiscordEvent<T> {

    private final DiscordMessageChannel channel;

    public AbstractDiscordMessageEvent(T jdaEvent, DiscordMessageChannel channel) {
        super(jdaEvent);
        this.channel = channel;
    }

    public boolean isGuildMessage() {
        return getDMChannel() == null;
    }

    /**
     * The Discord text channel if this event originated from a message sent in a text channel.
     * This will not be present on messages from threads (see {@link #getThreadChannel()}).
     * @return an optional potentially containing a {@link DiscordTextChannel}
     */
    @Nullable
    public DiscordTextChannel getTextChannel() {
        return channel instanceof DiscordTextChannel
               ? (DiscordTextChannel) channel
               : null;
    }

    @Nullable
    public DiscordThreadChannel getThreadChannel() {
        return channel instanceof DiscordThreadChannel
               ? (DiscordThreadChannel) channel
               : null;
    }

    @Nullable
    public DiscordDMChannel getDMChannel() {
        return channel instanceof DiscordDMChannel
               ? (DiscordDMChannel) channel
               : null;
    }

    @NotNull
    public DiscordMessageChannel getChannel() {
        return channel;
    }

}
