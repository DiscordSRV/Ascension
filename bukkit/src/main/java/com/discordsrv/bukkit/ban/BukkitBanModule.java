package com.discordsrv.bukkit.ban;

import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitBanModule extends AbstractModule<BukkitDiscordSRV> implements PunishmentModule.Bans {

    public BukkitBanModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "BUKKIT_BAN"));
    }

    @Override
    public CompletableFuture<Punishment> getBan(@NotNull UUID playerUUID) {
        CompletableFuture<BanEntry> entryFuture;
        if (PaperBanList.IS_AVAILABLE) {
            entryFuture = CompletableFuture.completedFuture(PaperBanList.getBanEntry(discordSRV.server(), playerUUID));
        } else {
            BanList banList = discordSRV.server().getBanList(BanList.Type.NAME);
            entryFuture = discordSRV.playerProvider().lookupOfflinePlayer(playerUUID)
                    .thenApply(offlinePlayer -> banList.getBanEntry(offlinePlayer.username()));
        }

        return entryFuture.thenApply(ban -> {
            Date expiration = ban.getExpiration();
            return new PunishmentModule.Punishment(expiration != null ? expiration.toInstant() : null, ban.getReason(), ban.getSource());
        });
    }

    @Override
    public CompletableFuture<Void> addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable String reason, @NotNull String punisher) {
        if (PaperBanList.IS_AVAILABLE) {
            PaperBanList.addBan(discordSRV.server(), playerUUID, until, reason, punisher);
            return CompletableFuture.completedFuture(null);
        }

        BanList banList = discordSRV.server().getBanList(BanList.Type.NAME);
        return discordSRV.playerProvider().lookupOfflinePlayer(playerUUID).thenApply(offlinePlayer -> {
            banList.addBan(offlinePlayer.username(), reason, until != null ? Date.from(until) : null, punisher);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> removeBan(@NotNull UUID playerUUID) {
        if (PaperBanList.IS_AVAILABLE) {
            PaperBanList.removeBan(discordSRV.server(), playerUUID);
            return CompletableFuture.completedFuture(null);
        }

        BanList banList = discordSRV.server().getBanList(BanList.Type.NAME);
        return discordSRV.playerProvider().lookupOfflinePlayer(playerUUID).thenApply(offlinePlayer -> {
            banList.pardon(offlinePlayer.username());
            return null;
        });
    }
}
