/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.exception;

import com.discordsrv.common.DiscordSRV;

public class StorageException extends RuntimeException {

    public StorageException(Throwable cause) {
        super(null, cause);
    }

    public StorageException(String message) {
        super(message);
    }

    public void log(DiscordSRV discordSRV) {
        String baseMessage = "Failed to initialize storage";
        Throwable cause = getCause();
        String message = getMessage();
        if (cause != null && message != null) {
            discordSRV.logger().error(baseMessage, this);
        } else if (message != null) {
            discordSRV.logger().error(baseMessage + ": " + message);
        } else if (cause != null) {
            discordSRV.logger().error(baseMessage, cause);
        }
    }
}
