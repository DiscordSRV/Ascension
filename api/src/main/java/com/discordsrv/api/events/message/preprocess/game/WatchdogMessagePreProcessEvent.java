/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;

/**
 * A server watchdog message was received and will be processed by DiscordSRV.
 * <p>
 * Order of events:
 * <ul>
 * <li>{@link com.discordsrv.api.events.message.preprocess.game.WatchdogMessagePreProcessEvent} (this event)</li>
 * <li>{@link com.discordsrv.api.events.message.postprocess.game.WatchdogMessagePostProcessEvent}</li>
 * <li>{@link com.discordsrv.api.events.message.post.game.WatchdogMessagePostEvent}</li>
 * </ul>
 */
public class WatchdogMessagePreProcessEvent extends AbstractGameMessagePreProcessEvent {

    private int timeout;
    private ZonedDateTime triggeredAt;

    public WatchdogMessagePreProcessEvent(
            @Nullable Object triggeringEvent,
            int timeout,
            ZonedDateTime triggeredAt
    ) {
        this(triggeringEvent, timeout, triggeredAt, false);
    }

    public WatchdogMessagePreProcessEvent(
            @Nullable Object triggeringEvent,
            int timeout,
            ZonedDateTime triggeredAt,
            boolean cancelled
    ) {
        super(triggeringEvent, cancelled, null, null);
        this.timeout = timeout;
        this.triggeredAt = triggeredAt;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public ZonedDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(ZonedDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }
}
