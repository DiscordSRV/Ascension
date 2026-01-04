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

package com.discordsrv.api.profile;

import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ProfileManager {

    @NotNull
    default Task<? extends @NotNull Profile> getProfile(UUID playerUUID) {
        Profile profile = getCachedProfile(playerUUID);
        if (profile != null) {
            return Task.completed(profile);
        }
        return queryProfile(playerUUID);
    }

    @NotNull
    Task<? extends Profile> queryProfile(UUID playerUUID);
    @Nullable
    Profile getCachedProfile(UUID playerUUID);

    @NotNull
    default Task<? extends @NotNull Profile> getProfile(long userId) {
        Profile profile = getCachedProfile(userId);
        if (profile != null) {
            return Task.completed(profile);
        }
        return queryProfile(userId);
    }

    @NotNull
    Task<? extends Profile> queryProfile(long userId);
    @Nullable
    Profile getCachedProfile(long userId);
}
