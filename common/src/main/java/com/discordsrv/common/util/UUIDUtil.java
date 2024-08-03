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

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@PlaceholderPrefix("uuid_")
public final class UUIDUtil {

    private UUIDUtil() {}

    public static UUID fromShortOrFull(@NotNull String uuidString) {
        int length = uuidString.length();
        if (length == 32) {
            return fromShort(uuidString);
        } else if (length == 36) {
            return UUID.fromString(uuidString);
        }
        throw new IllegalArgumentException("Not a valid 36 or 32 character long UUID");
    }

    public static UUID fromShort(@NotNull String shortUUID) {
        if (shortUUID.length() != 32) {
            throw new IllegalArgumentException("Short uuids are 32 characters long");
        }

        String fullLengthUUID = shortUUID.substring(0, 8)
                + "-" + shortUUID.substring(8, 12)
                + "-" + shortUUID.substring(12, 16)
                + "-" + shortUUID.substring(16, 20)
                + "-" + shortUUID.substring(20);
        return UUID.fromString(fullLengthUUID);
    }

    @Placeholder("short")
    public static String toShort(@NotNull UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    @Placeholder("version")
    public static int getVersion(@NotNull UUID uuid) {
        return uuid.version();
    }

    @Placeholder("isgeyser")
    public static boolean isGeyser(@NotNull UUID uuid) {
        return uuid.getLeastSignificantBits() == 0;
    }

    @Placeholder("isoffline")
    public static boolean isOffline(@NotNull UUID uuid) {
        return !isGeyser(uuid) && uuid.version() == 3;
    }
}
