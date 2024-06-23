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

import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
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
            getBan(player.getUniqueId())
                    .whenComplete((punishment, t) -> module.notifyBanned(discordSRV.playerProvider().player(player), punishment));
        }
    }

    @Override
    public CompletableFuture<com.discordsrv.api.punishment.Punishment> getBan(@NotNull UUID playerUUID) {
        CompletableFuture<BanEntry> entryFuture;
        if (PaperBanList.IS_AVAILABLE) {
            entryFuture = CompletableFuture.completedFuture(PaperBanList.getBanEntry(discordSRV.server(), playerUUID));
        } else {
            BanList banList = discordSRV.server().getBanList(BanList.Type.NAME);
            entryFuture = discordSRV.playerProvider().lookupOfflinePlayer(playerUUID)
                    .thenApply(offlinePlayer -> banList.getBanEntry(offlinePlayer.username()));
        }

        return entryFuture.thenApply(ban -> {
            if (ban == null) {
                return null;
            }
            Date expiration = ban.getExpiration();
            return new Punishment(expiration != null ? expiration.toInstant() : null, ban.getReason(), ban.getSource());
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
