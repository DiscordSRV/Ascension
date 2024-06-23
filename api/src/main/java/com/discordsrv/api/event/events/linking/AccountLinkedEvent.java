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

package com.discordsrv.api.event.events.linking;

import com.discordsrv.api.event.events.Event;
import com.discordsrv.api.profile.IProfile;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An event for when an account pair has been linked successfully.
 */
public class AccountLinkedEvent implements Event {

    private final IProfile profile;

    public AccountLinkedEvent(@NotNull IProfile profile) {
        if (!profile.isLinked()) {
            throw new IllegalStateException("Profile is not linked");
        }
        this.profile = profile;
    }

    /**
     * The profile of the linked account pair.
     * @return the profile
     */
    @NotNull
    public IProfile getProfile() {
        return profile;
    }

    /**
     * The UUID of the player that was linked, this player may not be connected to the server at the time of this event.
     * @return the player's {@link UUID}
     */
    @NotNull
    public UUID getPlayerUUID() {
        UUID uuid = profile.playerUUID();
        if (uuid == null) {
            throw new IllegalStateException("playerUUID is null");
        }
        return uuid;
    }

    /**
     * The user id of the user that was linked.
     * @return the user's Discord user id
     */
    public long getUserId() {
        Long userId = profile.userId();
        if (userId == null) {
            throw new IllegalStateException("userId is null");
        }
        return userId;
    }
}
