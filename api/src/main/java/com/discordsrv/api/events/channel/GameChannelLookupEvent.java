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

package com.discordsrv.api.events.channel;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.events.Processable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event is used to lookup {@link GameChannel}s by their name (and optionally plugin name).
 * This is also used to determine which plugin's channel should take priority when multiple plugins
 * define channels with the same name ({@link EventPriorities}).
 *
 * @see #isDefault()
 */
public class GameChannelLookupEvent implements Processable.Argument<GameChannel> {

    private final String pluginName;
    private final String channelName;

    private boolean processed;
    private GameChannel channel;

    public GameChannelLookupEvent(@Nullable String pluginName, @NotNull String channelName) {
        this.pluginName = pluginName;
        this.channelName = channelName;
    }

    /**
     * If this is {@code true} the default channel should be returned, if it exists.
     * @return if this lookup is for the default channel
     */
    public boolean isDefault() {
        return GameChannel.DEFAULT_NAME.equals(getChannelName());
    }

    /**
     * The channel name being looked up, this should be case-insensitive wherever possible.
     * @return the channel name
     */
    @NotNull
    public String getChannelName() {
        return channelName;
    }

    /**
     * Returns the channel from a {@link #process(GameChannel)} matching required criteria.
     * @return the game channel provided by a listener
     * @throws IllegalStateException if {@link #isProcessed()} doesn't return true
     */
    @NotNull
    public GameChannel getChannelFromProcessing() {
        if (!processed) {
            throw new IllegalStateException("This event has not been successfully processed yet, no channel is available");
        }
        return channel;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    /**
     * Provides a {@link GameChannel} for the provided channel name ({@link #getChannelName()}).
     * If this is the {@link #isDefault()} channel, any channel name is accepted.
     * @param channel the channel
     * @throws IllegalStateException if the event is already processed
     * @throws IllegalArgumentException if the provided channel doesn't match {@link #getChannelName()} and {@link #isDefault()} is {@code false}
     */
    @Override
    public void process(@NotNull GameChannel channel) {
        if (processed) {
            throw new IllegalStateException("Already processed");
        }
        if (pluginName != null && !pluginName.equalsIgnoreCase(channel.getOwnerName())) {
            // Not the plugin we're looking for, ignore
            return;
        }

        String channelName = channel.getChannelName();
        if (!isDefault() && !channelName.equalsIgnoreCase(this.channelName)) {
            throw new IllegalArgumentException("The provided channel is named "
                    + channelName + " when it should've been: " + this.channelName);
        }

        this.channel = channel;
        this.processed = true;
    }
}
