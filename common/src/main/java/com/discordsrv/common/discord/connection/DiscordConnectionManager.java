/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

public interface DiscordConnectionManager {

    /**
     * The default number of milliseconds to wait for shutdown before ending without completing rate limited requests.
     */
    int DEFAULT_SHUTDOWN_TIMEOUT = 10;

    /**
     * Gets the instance.
     * @return the jda instance, if connected
     */
    @Nullable
    JDA instance();

    /**
     * Attempts to connect to Discord.
     */
    void connect();

    /**
     * Shuts down the Discord connection after waiting for queued requests to complete.
     * Waits the provided number of milliseconds before running {@link #shutdownNow()}.
     *
     * @param timeoutSeconds the maximum number of seconds to wait for JDA to shut down
     */
    void shutdown(int timeoutSeconds);

    /**
     * Shuts down the Discord connection without waiting for queued requests to be completed.
     */
    void shutdownNow();

}
