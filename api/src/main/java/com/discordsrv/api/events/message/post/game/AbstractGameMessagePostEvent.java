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

package com.discordsrv.api.events.message.post.game;

import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.message.postprocess.game.AbstractGameMessagePostProcessEvent;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractGameMessagePostEvent<PE extends AbstractGameMessagePostProcessEvent<?>> implements Event {

    private final PE preEvent;
    private final ReceivedDiscordMessageCluster discordMessage;

    public AbstractGameMessagePostEvent(
            @NotNull PE preEvent,
            @NotNull ReceivedDiscordMessageCluster discordMessage
    ) {
        this.preEvent = preEvent;
        this.discordMessage = discordMessage;
    }

    @NotNull
    public PE getPreEvent() {
        return preEvent;
    }

    /**
     * Gets the {@link ReceivedDiscordMessageCluster} containing the sent message(s).
     * @return the message cluster
     */
    public ReceivedDiscordMessageCluster getDiscordMessage() {
        return discordMessage;
    }

}
