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

package com.discordsrv.bukkit.ban;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Server;

import java.time.Instant;
import java.util.UUID;

public final class PaperBanList {

    public static final boolean IS_AVAILABLE;

    static {
        boolean is = false;
        try {
            BanList.Type.valueOf("PROFILE");
            is = true;
        } catch (IllegalArgumentException ignored) {}
        IS_AVAILABLE = is;
    }

    private PaperBanList() {}

    public static BanList<UUID> banList(Server server) {
        return server.getBanList(BanList.Type.PROFILE);
    }

    public static BanEntry<?> getBanEntry(Server server, UUID playerUUID) {
        return banList(server).getBanEntry(playerUUID);
    }

    public static void addBan(Server server, UUID playerUUID, Instant until, String reason, String punisher) {
        banList(server).addBan(playerUUID, reason, until, punisher);
    }

    public static void removeBan(Server server, UUID playerUUID) {
        banList(server).pardon(playerUUID);
    }
}
