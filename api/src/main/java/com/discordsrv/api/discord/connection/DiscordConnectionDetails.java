/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.connection;

import com.discordsrv.api.DiscordSRVApi;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A helper class to provide {@link GatewayIntent}s and {@link CacheFlag}s to the {@link net.dv8tion.jda.api.JDA} instance created by DiscordSRV during startup.
 * @see DiscordSRVApi#discordConnectionDetails()
 */
@SuppressWarnings("unused") // API
public interface DiscordConnectionDetails {

    /**
     * If {@link #requestGatewayIntent(GatewayIntent, GatewayIntent...)}} and {@link #requestCacheFlag(CacheFlag, CacheFlag...)} can be used.
     * @return true, if {@link GatewayIntent}s and {@link CacheFlag} will be accepted
     */
    boolean readyToTakeDetails();

    /**
     * The current gateway intents.
     * @return the current set of gateway intents
     */
    @NotNull
    Set<GatewayIntent> getGatewayIntents();

    /**
     * Requests that the provided {@link GatewayIntent}s be passed to {@link net.dv8tion.jda.api.JDA}.
     *
     * @param gatewayIntent the first gateway intent to add
     * @param gatewayIntents more gateway intents
     * @throws IllegalStateException if DiscordSRV is already connecting/connected to Discord
     * @see #readyToTakeDetails()
     */
    void requestGatewayIntent(@NotNull GatewayIntent gatewayIntent, @NotNull GatewayIntent... gatewayIntents);

    /**
     * The current cache flags.
     * @return the current set of cache flags
     */
    @NotNull
    Set<CacheFlag> getCacheFlags();

    /**
     * Requests that the provided {@link CacheFlag} be passed to {@link net.dv8tion.jda.api.JDA}.
     *
     * @param cacheFlag the first cache flag
     * @param cacheFlags more cache flags
     * @throws IllegalStateException if DiscordSRV is already connecting/connected to Discord
     * @throws IllegalArgumentException if one of the requested {@link CacheFlag}s requires a {@link GatewayIntent} that hasn't been requested
     * @see #readyToTakeDetails()
     */
    void requestCacheFlag(@NotNull CacheFlag cacheFlag, @NotNull CacheFlag... cacheFlags);

}
