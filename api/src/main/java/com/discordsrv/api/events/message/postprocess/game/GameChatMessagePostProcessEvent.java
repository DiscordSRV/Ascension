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
import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A game chat message was processed by DiscordSRV and is about to be forwarded.
 * <b>Unlike other game chat postprocess and post events, this will be called once per Discord server</b>
 * <p>
 * Order of events:
 * <ul>
 * <li>{@link com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent}</li>
 * <li>{@link com.discordsrv.api.events.message.postprocess.game.GameChatMessagePostProcessEvent} (this event)</li>
 * <li>{@link com.discordsrv.api.events.message.post.game.GameChatMessagePostEvent}</li>
 * </ul>
 */
public class GameChatMessagePostProcessEvent extends AbstractGameMessagePostProcessEvent<GameChatMessagePreProcessEvent> {

    public GameChatMessagePostProcessEvent(
            @NotNull GameChatMessagePreProcessEvent preEvent,
            @NotNull DiscordSRVPlayer player,
            @NotNull List<DiscordGuildMessageChannel> channels,
            @NotNull SendableDiscordMessage message
    ) {
        super(preEvent, player, channels, message);
    }
}
