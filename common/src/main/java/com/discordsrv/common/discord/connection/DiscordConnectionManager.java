/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.discord.connection;

import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface DiscordConnectionManager {

    /**
     * The default amount of milliseconds to wait for shutdown before ending without completing ratelimited requests.
     */
    long DEFAULT_SHUTDOWN_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    /**
     * Gets the instance.
     * @return the jda instance, if connected
     */
    @Nullable
    JDA instance();

    /**
     * Are gateway intents and cache flags accepted.
     * @return true for yes
     */
    boolean areDetailsAccepted();

    /**
     * Attempts to connect to Discord.
     * @return a {@link CompletableFuture}
     */
    CompletableFuture<Void> connect();

    /**
     * Shuts down the Discord connection and connects again.
     * @return a {@link CompletableFuture}
     */
    CompletableFuture<Void> reconnect();

    /**
     * Shuts down the Discord connection after waiting for queued requests to complete. Blocks until shutdown is completed.
     * @return a {@link CompletableFuture}
     * @see #DEFAULT_SHUTDOWN_TIMEOUT
     */
    default CompletableFuture<Void> shutdown() {
        return shutdown(DEFAULT_SHUTDOWN_TIMEOUT);
    }

    /**
     * Shuts down the Discord connection after waiting for queued requests to complete.
     * Waits the provided amount of milliseconds before running {@link #shutdownNow()}.
     *
     * @param timeoutMillis the maximum amount of milliseconds to wait for shut down
     * @return a {@link CompletableFuture}
     */
    CompletableFuture<Void> shutdown(long timeoutMillis);

    /**
     * Shuts down the Discord connection without waiting for queued requests to be completed.
     */
    void shutdownNow();

}
