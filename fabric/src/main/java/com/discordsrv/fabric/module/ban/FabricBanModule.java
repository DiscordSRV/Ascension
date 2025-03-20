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

package com.discordsrv.fabric.module.ban;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class FabricBanModule extends AbstractFabricModule implements PunishmentModule.Bans {

    private static FabricBanModule instance;

    public FabricBanModule(FabricDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "BAN_MODULE"));

        instance = this;
    }

    public static void onBan(GameProfile gameProfile) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module == null) return;

        UUID playerUUID = gameProfile.getId();
        IPlayer player = discordSRV.playerProvider().player(gameProfile.getId());
        if (player == null) {
            throw new RuntimeException("Player " + playerUUID + " not present in player provider");
        }

        instance.getBan(playerUUID).whenComplete((punishment, t) -> {
            if (punishment != null) {
                module.notifyBanned(player, punishment);
            }
        });
    }

    public static void onPardon(GameProfile gameProfile) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) instance.removeBan(gameProfile.getId());
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        BannedPlayerList banList = discordSRV.getServer().getPlayerManager().getUserBanList();

        Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getUserCache()).getByUuid(playerUUID);
        if (gameProfile.isEmpty()) {
            return Task.completed(null);
        }

        BannedPlayerEntry banEntry = banList.get(gameProfile.get());
        if (banEntry == null) {
            return Task.completed(null);
        }
        Date expiration = banEntry.getExpiryDate();

        return Task.completed(new Punishment(
                expiration != null ? expiration.toInstant() : null,
                ComponentUtil.fromPlain(banEntry.getReason()),
                ComponentUtil.fromPlain(banEntry.getSource())
        ));
    }

    @Override
    public Task<Void> addBan(
            @NotNull UUID playerUUID,
            @Nullable Instant until,
            @Nullable MinecraftComponent reason,
            @NotNull MinecraftComponent punisher
    ) {
        try {
            MinecraftServer server = discordSRV.getServer();
            UserCache userCache = server.getUserCache();

            GameProfile gameProfile = null;
            if (userCache != null) {
                gameProfile = userCache.getByUuid(playerUUID).orElse(null);
            }

            String reasonProvided = reason != null ? reason.asPlainString() : null;
            Date expiration = until != null ? Date.from(until) : null;

            BannedPlayerEntry banEntry = new BannedPlayerEntry(gameProfile, new Date(), reasonProvided, expiration, punisher.asPlainString());
            server.getPlayerManager().getUserBanList().add(banEntry);

            ServerPlayerEntity serverPlayerEntity = server.getPlayerManager().getPlayer(playerUUID);
            if (serverPlayerEntity != null) {
                serverPlayerEntity.networkHandler.disconnect(
                        reason != null
                            ? discordSRV.getAdventure().asNative(reason.asAdventure())
                            : Text.translatable("multiplayer.disconnect.banned")
                );
            }
        } catch (Exception e) {
            discordSRV.logger().error("Failed to ban player", e);
        }

        return Task.completed(null);
    }

    @Override
    public Task<Void> removeBan(@NotNull UUID playerUUID) {
        BannedPlayerList banList = discordSRV.getServer().getPlayerManager().getUserBanList();

        Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getUserCache()).getByUuid(playerUUID);
        if (gameProfile.isEmpty()) {
            return Task.completed(null);
        }

        banList.remove(gameProfile.get());
        return Task.completed(null);
    }
}
