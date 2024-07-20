/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.event.events.message.forward.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.event.events.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractGameMessageForwardedEvent implements Event {

    private final GameChannel originGameChannel;
    private final ReceivedDiscordMessageCluster discordMessage;

    public AbstractGameMessageForwardedEvent(
            @Nullable GameChannel originGameChannel,
            @NotNull ReceivedDiscordMessageCluster discordMessage
    ) {
        this.originGameChannel = originGameChannel;
        this.discordMessage = discordMessage;
    }

    public GameChannel getOriginGameChannel() {
        return originGameChannel;
    }

    /**
     * Gets the {@link ReceivedDiscordMessageCluster} containing the sent message(s).
     * @return the message cluster
     */
    public ReceivedDiscordMessageCluster getDiscordMessage() {
        return discordMessage;
    }

}
