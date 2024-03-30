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
