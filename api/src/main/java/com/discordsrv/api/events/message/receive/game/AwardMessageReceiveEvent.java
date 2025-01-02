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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Indicates that an advancement or achievement message was received will be processed
 * at {@link EventPriorities#DEFAULT} unless cancelled or processed by a 3rd party.
 */
public class AwardMessageReceiveEvent extends AbstractGameMessageReceiveEvent implements PlayerEvent {

    private final DiscordSRVPlayer player;
    private MinecraftComponent name;
    private MinecraftComponent title;
    private GameChannel gameChannel;

    public AwardMessageReceiveEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @Nullable MinecraftComponent name,
            @Nullable MinecraftComponent title,
            @Nullable GameChannel gameChannel,
            boolean cancelled
    ) {
        super(triggeringEvent, cancelled);
        this.player = player;
        this.name = name;
        this.title = title;
        this.gameChannel = gameChannel;
    }

    @Override
    @NotNull
    public DiscordSRVPlayer getPlayer() {
        return player;
    }

    @Nullable
    public MinecraftComponent getName() {
        return name;
    }

    public void setName(@Nullable MinecraftComponent name) {
        this.name = name;
    }

    @Nullable
    public MinecraftComponent getTitle() {
        return title;
    }

    public void setTitle(@Nullable MinecraftComponent title) {
        this.title = title;
    }

    public GameChannel getGameChannel() {
        return gameChannel;
    }

    public void setGameChannel(GameChannel gameChannel) {
        this.gameChannel = gameChannel;
    }

    @Override
    public String toString() {
        return "AwardMessageReceiveEvent{"
                + "player=" + player + ", "
                + "gameChannel=" + GameChannel.toString(gameChannel)
                + '}';
    }
}
