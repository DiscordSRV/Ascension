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

package com.discordsrv.api.events.message.receive.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.events.PlayerEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Indicates that a chat message was received and will be processed
 * at {@link EventPriorities#DEFAULT} unless cancelled or processed by a 3rd party.
 */
public class GameChatMessageReceiveEvent extends AbstractGameMessageReceiveEvent implements PlayerEvent {

    private final DiscordSRVPlayer player;
    private MinecraftComponent message;
    private GameChannel gameChannel;

    public GameChatMessageReceiveEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @NotNull MinecraftComponent message,
            @NotNull GameChannel gameChannel
    ) {
        this(triggeringEvent, player, message, gameChannel, false);
    }

    @ApiStatus.Experimental
    public GameChatMessageReceiveEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @NotNull MinecraftComponent message,
            @NotNull GameChannel gameChannel,
            boolean cancelled
    ) {
        super(triggeringEvent, cancelled);
        this.player = player;
        this.message = message;
        this.gameChannel = gameChannel;
    }

    @NotNull
    public MinecraftComponent getMessage() {
        return message;
    }

    public void setMessage(@NotNull MinecraftComponent message) {
        this.message = message;
    }

    @NotNull
    public GameChannel getGameChannel() {
        return gameChannel;
    }

    public void setGameChannel(@NotNull GameChannel gameChannel) {
        this.gameChannel = gameChannel;
    }

    @Override
    public @NotNull DiscordSRVPlayer getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return "GameChatMessageReceiveEvent{"
                + "player=" + player + ", "
                + "gameChannel=" + GameChannel.toString(gameChannel)
                + '}';
    }
}
