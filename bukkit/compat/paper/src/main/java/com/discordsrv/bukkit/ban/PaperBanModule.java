/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.destroystokyo.paper.profile.PlayerProfile;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import com.discordsrv.common.util.ComponentUtil;
import io.papermc.paper.ban.BanListType;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.BanEntry;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@ApiStatus.AvailableSince("Paper 1.21.1")
public class PaperBanModule extends AbstractModule<BukkitDiscordSRV> implements Listener, PunishmentModule.Bans {

    public PaperBanModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        switch (event.getCause()) {
            case BANNED:
            case IP_BANNED:
            case PLUGIN:
                break;
        }

        Player player = event.getPlayer();
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) {
            getBan(player.getUniqueId())
                    .whenComplete((punishment, t) -> module.notifyBanned(discordSRV.playerProvider().player(player), punishment));
        }
    }

    @Override
    public Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        ProfileBanList banList = discordSRV.server().getBanList(BanListType.PROFILE);
        PlayerProfile profile = discordSRV.server().createProfile(playerUUID);

        BanEntry<PlayerProfile> ban = banList.getBanEntry(profile);
        if (ban == null) {
            return null;
        }
        Date expiration = ban.getExpiration();
        String reason = ban.getReason();

        return Task.completed(new Punishment(
                expiration != null ? expiration.toInstant() : null,
                reason != null ? ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(reason)) : null,
                ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(ban.getSource()))
        )) ;
    }

    @Override
    public Task<Void> addBan(
            @NotNull UUID playerUUID,
            @Nullable Instant until,
            @Nullable MinecraftComponent reason,
            @NotNull MinecraftComponent punisher
    ) {
        ProfileBanList banList = discordSRV.server().getBanList(BanListType.PROFILE);
        PlayerProfile profile = discordSRV.server().createProfile(playerUUID);

        String reasonLegacy = reason != null ? BukkitComponentSerializer.legacy().serialize(ComponentUtil.fromAPI(reason)) : null;
        String punisherLegacy = BukkitComponentSerializer.legacy().serialize(ComponentUtil.fromAPI(punisher));
        banList.addBan(profile, reasonLegacy, until != null ? Date.from(until) : null, punisherLegacy);
        return Task.completed(null);
    }

    @Override
    public Task<Void> removeBan(@NotNull UUID playerUUID) {
        ProfileBanList banList = discordSRV.server().getBanList(BanListType.PROFILE);
        PlayerProfile profile = discordSRV.server().createProfile(playerUUID);
        banList.pardon(profile);
        return Task.completed(null);
    }
}
