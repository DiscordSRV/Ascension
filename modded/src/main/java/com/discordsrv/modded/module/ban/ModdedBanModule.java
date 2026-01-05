/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.modded.module.ban;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.module.AbstractModdedModule;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class ModdedBanModule extends AbstractModdedModule implements PunishmentModule.Bans {

    private static ModdedBanModule instance;

    public ModdedBanModule(ModdedDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "BAN_MODULE"));

        instance = this;
    }

    //? if minecraft: >=1.21.9 {
    public static void onBan(net.minecraft.server.players.NameAndId playerConfigEntry) {
        if (instance == null) return;
        ModdedDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module == null) return;

        UUID playerUUID = playerConfigEntry.id();
    //?} else {
    /*public static void onBan(GameProfile gameProfile) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module == null) return;

        UUID playerUUID = discordSRV.getIdFromGameProfile(gameProfile);
    *///?}
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
    public static void onPardon(net.minecraft.server.players.NameAndId entry) {
        if (instance == null) return;
        ModdedDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) instance.removeBan(entry.id());
    }
    //?} else {
    /*public static void onPardon(GameProfile gameProfile) {
        if (instance == null) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) instance.removeBan(discordSRV.getIdFromGameProfile(gameProfile));
    }
    *///?}

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        UserBanList banList = discordSRV.getServer().getPlayerList().getBans();

        //? if minecraft: >=1.21.9 {
        Optional<net.minecraft.server.players.NameAndId> playerConfigEntry = discordSRV.getServer().services().nameToIdCache().get(playerUUID);
        if (playerConfigEntry.isEmpty()) {
            return Task.completed(null);
        }
        UserBanListEntry banEntry = banList.get(playerConfigEntry.get());
        //?} else {
        /*Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getProfileCache()).get(playerUUID);
        if (gameProfile.isEmpty()) {
            return Task.completed(null);
        }
        UserBanListEntry banEntry = banList.get(gameProfile.get());
        *///?}

        if (banEntry == null) {
            return Task.completed(null);
        }
        Date expiration = banEntry.getExpires();

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
            net.minecraft.server.players.NameAndId entry = null;
            Optional<net.minecraft.server.players.NameAndId> entryOptional = discordSRV.getServer().services().nameToIdCache().get(playerUUID);
            if (entryOptional.isPresent()) {
                entry = entryOptional.get();
            }
            //?} else {
            /*net.minecraft.server.players.GameProfileCache userCache = server.getProfileCache();

            GameProfile entry = null;
            if (userCache != null) {
                entry = userCache.get(playerUUID).orElse(null);
            }
            *///?}

            String reasonProvided = reason != null ? reason.asPlainString() : null;
            Date expiration = until != null ? Date.from(until) : null;

            UserBanListEntry banEntry = new UserBanListEntry(entry, new Date(), reasonProvided, expiration, punisher.asPlainString());
            server.getPlayerList().getBans().add(banEntry);

            ServerPlayer serverPlayerEntity = server.getPlayerList().getPlayer(playerUUID);
            if (serverPlayerEntity != null) {
                net.minecraft.network.chat.Component text = discordSRV.componentFactory().toNative(reason != null ? reason.asAdventure() : Component.empty());
                if (reason == null || reason.asPlainString().isEmpty()) {
                    text = discordSRV.componentFactory().toNative(Component.translatable("multiplayer.disconnect.banned"));
                }
                serverPlayerEntity.connection.disconnect(text);
            }
        } catch (Exception e) {
            discordSRV.logger().error("Failed to ban player", e);
        }

        return Task.completed(null);
    }

    @Override
    public Task<Void> removeBan(@NotNull UUID playerUUID) {
        UserBanList banList = discordSRV.getServer().getPlayerList().getBans();

        //? if minecraft: >=1.21.9 {
        discordSRV.getServer().services().nameToIdCache().get(playerUUID).ifPresent(name -> {
            banList.remove(name);
        });
        //?} else {
        /*Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getProfileCache()).get(playerUUID);
        if (gameProfile.isEmpty()) {
            return Task.completed(null);
        }

        banList.remove(gameProfile.get());
        *///?}
        return Task.completed(null);
    }
}
