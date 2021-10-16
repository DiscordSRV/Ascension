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

package com.discordsrv.common.listener;

import com.discordsrv.api.event.bus.EventListener;
import com.discordsrv.api.event.events.Cancellable;
import com.discordsrv.api.event.events.Processable;
import com.discordsrv.common.DiscordSRV;

public abstract class AbstractListener {

    protected final DiscordSRV discordSRV;

    public AbstractListener(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public boolean checkProcessor(Processable event) {
        if (!event.isProcessed()) {
            return false;
        }

        String whoProcessed = event.whoProcessed()
                .map(EventListener::className)
                .orElse("Unknown");
        if (!whoProcessed.startsWith("com.discordsrv")) {
            discordSRV.logger().debug(event + " was handled by non-DiscordSRV handler: " + whoProcessed);
        }
        return true;
    }

    public boolean checkCancellation(Cancellable event) {
        if (!event.isCancelled()) {
            return false;
        }

        String whoCancelled = event.whoCancelled()
                .map(EventListener::className)
                .orElse("Unknown");
        discordSRV.logger().debug(event + " was cancelled by " + whoCancelled);
        return true;
    }
}
