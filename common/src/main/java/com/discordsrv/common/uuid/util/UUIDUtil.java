package com.discordsrv.common.uuid.util;

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
}
