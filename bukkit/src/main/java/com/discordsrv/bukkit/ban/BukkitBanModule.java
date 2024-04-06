package com.discordsrv.bukkit.ban;

import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.bansync.BanSyncModule;
import com.discordsrv.common.module.type.AbstractModule;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitBanModule extends AbstractModule<BukkitDiscordSRV> implements Listener, PunishmentModule.Bans {

    public BukkitBanModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        if (!player.isBanned()) {
            return;
        }

        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) {
            getBan(player.getUniqueId()).thenApply(Punishment::reason)
                    .whenComplete((reason, t) -> module.notifyBanned(discordSRV.playerProvider().player(player), reason));
        }
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
