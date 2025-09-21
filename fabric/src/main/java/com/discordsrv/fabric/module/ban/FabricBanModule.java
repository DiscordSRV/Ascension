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
import net.kyori.adventure.text.Component;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
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

    //? if minecraft: >=1.21.9 {
    public static void onBan(PlayerConfigEntry playerConfigEntry) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module == null) return;

        UUID playerUUID = playerConfigEntry.id();
    //? } else {
    /*public static void onBan(GameProfile gameProfile) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module == null) return;

        UUID playerUUID = discordSRV.componentFactory().getId(gameProfile);
    *///? }
        IPlayer player = discordSRV.playerProvider().player(playerUUID);
        if (player == null) {
            throw new RuntimeException("Player " + playerUUID + " not present in player provider");
        }

        instance.getBan(playerUUID).whenComplete((punishment, t) -> {
            if (punishment != null) {
                module.notifyBanned(player, punishment);
            }
        });
    }

    //? if minecraft: >=1.21.9 {
    public static void onPardon(PlayerConfigEntry entry) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) instance.removeBan(entry.id());
    }
    //? } else {
    public static void onPardon(GameProfile gameProfile) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) instance.removeBan(discordSRV.componentFactory().getId(gameProfile));
    }
    //? }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        BannedPlayerList banList = discordSRV.getServer().getPlayerManager().getUserBanList();

        //? if minecraft: >=1.21.9 {
        Optional<PlayerConfigEntry> playerConfigEntry = discordSRV.getServer().getApiServices().nameToIdCache().getByUuid(playerUUID);
        if (playerConfigEntry.isEmpty()) {
            return Task.completed(null);
        }
        BannedPlayerEntry banEntry = banList.get(playerConfigEntry.get());
        //? } else {
        Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getUserCache()).getByUuid(playerUUID);
        if (gameProfile.isEmpty()) {
            return Task.completed(null);
        }

        //? if minecraft: >=1.21.9 {
        BannedPlayerEntry banEntry = banList.get(PlayerConfigEntry.fromNickname(gameProfile.get().name()));
        //? } else {
        BannedPlayerEntry banEntry = banList.get(gameProfile.get());
        //? }

        //? }
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
            //? if minecraft: >=1.21.9 {
            PlayerConfigEntry entry = null;
            Optional<PlayerConfigEntry> entryOptional = discordSRV.getServer().getApiServices().nameToIdCache().getByUuid(playerUUID);
            if (entryOptional.isPresent()) {
                entry = entryOptional.get();
            }
            //? } else {
            /*UserCache userCache = server.getUserCache();

            GameProfile entry = null;
            if (userCache != null) {
                entry = userCache.getByUuid(playerUUID).orElse(null);
            }
            *///? }

            String reasonProvided = reason != null ? reason.asPlainString() : null;
            Date expiration = until != null ? Date.from(until) : null;

            BannedPlayerEntry banEntry = new BannedPlayerEntry(entry, new Date(), reasonProvided, expiration, punisher.asPlainString());
            server.getPlayerManager().getUserBanList().add(banEntry);

            ServerPlayerEntity serverPlayerEntity = server.getPlayerManager().getPlayer(playerUUID);
            if (serverPlayerEntity != null) {
                Text text = discordSRV.componentFactory().toNative(reason != null ? reason.asAdventure() : Component.empty());
                if (reason == null || reason.asPlainString().isEmpty()) {
                    text = discordSRV.componentFactory().toNative(Component.translatable("multiplayer.disconnect.banned"));
                }
                serverPlayerEntity.networkHandler.disconnect(text);
            }
        } catch (Exception e) {
            discordSRV.logger().error("Failed to ban player", e);
        }

        return Task.completed(null);
    }

    @Override
    public Task<Void> removeBan(@NotNull UUID playerUUID) {
        BannedPlayerList banList = discordSRV.getServer().getPlayerManager().getUserBanList();

        //? if minecraft: >=1.21.9 {
        discordSRV.getServer().getApiServices().nameToIdCache().getByUuid(playerUUID).ifPresent(name -> {
            banList.remove(name);
        });
        //? } else {
        Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getUserCache()).getByUuid(playerUUID);
        if (gameProfile.isEmpty()) {
            return Task.completed(null);
        }

        banList.remove(gameProfile.get());
        //? }
        return Task.completed(null);
    }
}
