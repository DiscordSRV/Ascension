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

package com.discordsrv.api.events.message.preprocess.game;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.PlayerEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A server switch message was received,
 * DiscordSRV will process it (if enabled, not already processed and not cancelled) at priority {@link com.discordsrv.api.eventbus.EventPriorities#DEFAULT}.
 * <p>
 * Order of events:
 * <ul>
 * <li>{@link com.discordsrv.api.events.message.preprocess.game.ServerSwitchMessagePreProcessEvent} (this event)</li>
 * <li>{@link com.discordsrv.api.events.message.postprocess.game.ServerSwitchMessagePostProcessEvent}</li>
 * <li>{@link com.discordsrv.api.events.message.post.game.ServerSwitchMessagePostEvent}</li>
 * </ul>
 */
public class ServerSwitchMessagePreProcessEvent extends AbstractGameMessagePreProcessEvent {

    public ServerSwitchMessagePreProcessEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @Nullable MinecraftComponent message
    ) {
        this(triggeringEvent, player, message, false);
    }

    @ApiStatus.Experimental
    public ServerSwitchMessagePreProcessEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @Nullable MinecraftComponent message,
            boolean cancelled
    ) {
        super(triggeringEvent, cancelled, player, null);
        this.message = message;
    }

    @Override
    public String toString() {
        return "ServerSwitchMessageReceiveEvent{"
                + "player=" + player
                + "}";
    }
}
