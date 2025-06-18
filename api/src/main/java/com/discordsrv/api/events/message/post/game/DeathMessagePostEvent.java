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
import com.discordsrv.api.events.message.postprocess.game.DeathMessagePostProcessEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates that a death message was forwarded to Discord.
 * <p>
 * Order of events:
 * <li> {@link com.discordsrv.api.events.message.preprocess.game.DeathMessagePreProcessEvent}
 * <li> {@link com.discordsrv.api.events.message.postprocess.game.DeathMessagePostProcessEvent}
 * <li> {@link com.discordsrv.api.events.message.post.game.DeathMessagePostEvent} (this event)
 */
public class DeathMessagePostEvent extends AbstractGameMessagePostEvent<DeathMessagePostProcessEvent> {

    public DeathMessagePostEvent(
            @NotNull DeathMessagePostProcessEvent preEvent,
            @NotNull ReceivedDiscordMessageCluster discordMessage
    ) {
        super(preEvent, discordMessage);
    }
}
