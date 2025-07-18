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

package com.discordsrv.api.events.vanish;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An event to indicate that the given {@link DiscordSRVPlayer Players} vanish status has changed.
 * DiscordSRV listens to this event.
 */
public class PlayerVanishStatusChangeEvent implements Event {

    private final DiscordSRVPlayer player;
    private final boolean newStatus;

    private boolean sendFakeMessage;
    private MinecraftComponent fakeMessage;

    public PlayerVanishStatusChangeEvent(
            @NotNull DiscordSRVPlayer player,
            boolean newStatus,
            boolean sendFakeMessage,
            @Nullable MinecraftComponent fakeMessage
    ) {
        this.player = player;
        this.newStatus = newStatus;
        this.sendFakeMessage = sendFakeMessage;
        this.fakeMessage = fakeMessage;
    }

    @NotNull
    public DiscordSRVPlayer getPlayer() {
        return player;
    }

    public boolean isNewStatus() {
        return newStatus;
    }

    public boolean isSendFakeMessage() {
        return sendFakeMessage;
    }

    public void setSendFakeMessage(boolean sendFakeMessage) {
        this.sendFakeMessage = sendFakeMessage;
    }

    @Nullable
    public MinecraftComponent getFakeMessage() {
        return fakeMessage;
    }

    /**
     * Sets the fake join/leave message for this vanish status change.
     * @param fakeMessage the fake join/leave message or {@code null}
     */
    public void setFakeMessage(@Nullable MinecraftComponent fakeMessage) {
        this.fakeMessage = fakeMessage;
    }
}
