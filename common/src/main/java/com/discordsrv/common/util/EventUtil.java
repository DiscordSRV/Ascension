/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.util;

import com.discordsrv.api.eventbus.EventListener;
import com.discordsrv.api.events.Cancellable;
import com.discordsrv.api.events.Processable;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;

public final class EventUtil {

    private EventUtil() {}

    public static boolean checkProcessor(DiscordSRV discordSRV, Processable event, Logger logger) {
        if (!event.isProcessed()) {
            return false;
        }

        EventListener processor = event.whoProcessed();
        String whoProcessed = processor != null ? processor.className() : "Unknown";
        if (!whoProcessed.startsWith("com.discordsrv")) {
            logger.debug(event + " was handled by non-DiscordSRV handler: " + whoProcessed);
        }
        return true;
    }

    public static boolean checkCancellation(DiscordSRV discordSRV, Cancellable event, Logger logger) {
        if (!event.isCancelled()) {
            return false;
        }

        EventListener canceller = event.whoCancelled();
        String whoCancelled = canceller != null ? canceller.className() : "Unknown";
        logger.debug(event + " was cancelled by " + whoCancelled);
        return true;
    }
}
