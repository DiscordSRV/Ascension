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

package com.discordsrv.api.channel;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * An in-game channel for sending Minecraft messages to.
 */
@PlaceholderPrefix("gamechannel_")
public interface GameChannel {

    String DEFAULT_NAME = "global";

    /**
     * Gets the name of the plugin/mod/extension that 'owns' this game channel.
     * @return the name of the owner of this game channel
     */
    @NotNull
    String getOwnerName();

    /**
     * Gets the name of this channel.
     * @return the channel name
     */
    @Placeholder("name")
    @NotNull
    String getChannelName();

    /**
     * Determines if this channel is a two-way chat channel.
     * @return if this is a two-way chat channel
     */
    boolean isChat();

    /**
     * Players that will receive messages for this channel, these players must not be included in {@link #sendMessage(MinecraftComponent)}.
     * @return the recipients for this channel
     * @see #sendMessage(MinecraftComponent)
     */
    @NotNull
    Collection<? extends DiscordSRVPlayer> getRecipients();

    /**
     * Send a message to this {@link GameChannel}'s participants which are not included in {@link #getRecipients()}.
     * @param component the message
     * @see #getRecipients()
     */
    default void sendMessage(@NotNull MinecraftComponent component) {}

    /**
     * Sends the given message to the given player, used with {@link #getRecipients()}. May be used to apply personalized filters.
     * @param player the player
     * @param component the message
     * @see #getRecipients()
     */
    default void sendMessageToPlayer(@NotNull DiscordSRVPlayer player, @NotNull MinecraftComponent component) {
        player.sendMessageFromDiscord(component);
    }

    static String toString(@Nullable GameChannel channel) {
        if (channel == null) {
            return null;
        }
        return channel.getClass().getSimpleName() + "{" + channel.getOwnerName() + ":" + channel.getChannelName() + '}';
    }
}
