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

package com.discordsrv.api.discord.connection.details;

import com.discordsrv.api.DiscordSRVApi;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

/**
 * A helper class to provide {@link DiscordGatewayIntent}s and {@link DiscordCacheFlag}s for the Discord connection.
 * @see DiscordSRVApi#discordConnectionDetails()
 */
@SuppressWarnings("unused") // API
public interface DiscordConnectionDetails {

    /**
     * Requests that the provided {@link DiscordGatewayIntent}s be passed to the Discord connection.
     *
     * @param gatewayIntent the first gateway intent to add
     * @param gatewayIntents more gateway intents
     * @return {@code true} if the Discord connection is yet to be created and the intent will become active once it is
     */
    boolean requestGatewayIntent(@NotNull DiscordGatewayIntent gatewayIntent, @NotNull DiscordGatewayIntent... gatewayIntents);

    /**
     * Requests that the provided {@link DiscordCacheFlag}s be passed to the Discord connection.
     *
     * @param cacheFlag the first cache flag
     * @param cacheFlags more cache flags
     * @return {@code true} if the Discord connection is yet to be created and the intent will become active once it is
     * @throws IllegalArgumentException if one of the requested {@link CacheFlag}s requires a {@link GatewayIntent} that hasn't been requested
     */
    boolean requestCacheFlag(@NotNull DiscordCacheFlag cacheFlag, @NotNull DiscordCacheFlag... cacheFlags);

}
