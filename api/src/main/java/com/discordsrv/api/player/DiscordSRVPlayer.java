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

package com.discordsrv.api.player;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * A DiscordSRV player.
 */
public interface DiscordSRVPlayer {

    /**
     * The username of the player.
     * @return the player's username
     */
    @NotNull
    @Placeholder("name")
    String username();

    /**
     * The {@link UUID} of the player.
     * @return the player's unique id
     */
    @NotNull
    @Placeholder(value = "uuid", relookup = "uuid")
    UUID uniqueId();

    /**
     * Gets the locale of the player.
     * @return the player's locale, or {@code null} if it isn't known
     */
    @Nullable
    Locale locale();

    /**
     * Sends the provided message to the player.
     * @param component the message
     */
    void sendMessage(@NotNull MinecraftComponent component);

}
