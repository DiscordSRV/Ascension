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

package com.discordsrv.bukkit.integration.punishments;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.feature.bansync.BanSyncModule;
//import com.discordsrv.common.feature.mutesync.MuteSyncModule;
import com.discordsrv.common.util.ComponentUtil;
import litebans.api.Database;
import litebans.api.Entry;
import litebans.api.Events;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public class LiteBansIntegration extends PluginIntegration<BukkitDiscordSRV>
        implements PunishmentModule.Bans, PunishmentModule.Mutes {

    private LiteBansEventListener listener;
    private Database liteBansDatabase;

    public LiteBansIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "LITEBANS_INTEGRATION"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "LiteBans";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("litebans.api.Events");
        } catch (ClassNotFoundException ignored) {
            return false;
        }
        return super.isEnabled();
    }

    @Override
    public void enable() {
        this.liteBansDatabase = Database.get();
        this.listener = new LiteBansEventListener(discordSRV);
        Events.get().register(this.listener);
    }

    @Override
    public void disable() {
        this.liteBansDatabase = null;
        if (this.listener != null) {
            Events.get().unregister(this.listener);
            this.listener = null;
        }
    }

    @Override
    public Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        return discordSRV.scheduler().supplyOnMainThread(() -> {
            Entry entry = this.liteBansDatabase.getBan(playerUUID, null, null);
            return entry == null ? null : punishment(entry);
        });
    }

    @Override
    public Task<Void> addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        discordSRV.scheduler().runOnMainThread(() -> discordSRV.console().commandExecutorProvider().getConsoleExecutor(null).runCommand("ban " +
                playerUUID + " " +
                "--sender=DiscordSRV " +
                (until != null ? ((until.toEpochMilli() - Instant.now().toEpochMilli()) / 1000) + "s " : "") +
                (reason != null ? reason.asPlainString() : "")
        ));
        return Task.completed(null);
    }

    @Override
    public Task<Void> removeBan(@NotNull UUID playerUUID) {
        discordSRV.scheduler().supplyOnMainThread(() -> discordSRV.console().commandExecutorProvider().getConsoleExecutor(null).runCommand("unban " + playerUUID));
        return Task.completed(null);
    }

    @Override
    public Task<@Nullable Punishment> getMute(@NotNull UUID playerUUID) {
        return discordSRV.scheduler().supplyOnMainThread(() -> {
            Entry entry = this.liteBansDatabase.getMute(playerUUID, null, null);
            if (entry == null) return null;
            return punishment(entry);
        });
    }

    @Override
    public Task<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        discordSRV.scheduler().runOnMainThread(() -> discordSRV.console().commandExecutorProvider().getConsoleExecutor(null).runCommand("mute " +
                playerUUID + " " +
                "--sender=DiscordSRV " +
                (until != null ? ((until.toEpochMilli() - Instant.now().toEpochMilli()) / 1000) + "s " : "") +
                (reason != null ? reason.asPlainString() : "")
        ));
        return Task.completed(null);
    }

    @Override
    public Task<Void> removeMute(@NotNull UUID playerUUID) {
        discordSRV.scheduler().runOnMainThread(() -> discordSRV.console().commandExecutorProvider().getConsoleExecutor(null).runCommand("unmute " + playerUUID));
        return Task.completed(null);
    }

    private Punishment punishment(Entry entry) {
        return new Punishment(
                !entry.isPermanent() ? (entry.getDateEnd() > 0 ? Instant.ofEpochMilli(entry.getDateEnd()) : null) : null,
                entry.getReason() != null ? ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(entry.getReason())) : null,
                entry.getExecutorName() != null ? ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(entry.getExecutorName())) : null
        );
    }

    private class LiteBansEventListener extends Events.Listener {

        private final BukkitDiscordSRV discordSRV;
        private final BanSyncModule bans;
//        private final MuteSyncModule mutes;

        private LiteBansEventListener(BukkitDiscordSRV discordSRV) {
            this.discordSRV = discordSRV;
            this.bans = discordSRV.getModule(BanSyncModule.class);
//            this.mutes = discordSRV.getModule(MuteSyncModule.class);
        }

        public void onEntry(Entry entry, boolean added) {
            if (entry.getUuid() == null) return;

            UUID playerUUID = UUID.fromString(entry.getUuid());
            IPlayer player = discordSRV.playerProvider().player(playerUUID);
            if (player == null) {
                throw new RuntimeException("Player " + playerUUID + " not present in player provider");
            }

            switch (entry.getType()) {
                case "ban":
                    bans.notifyBanned(player, added ? punishment(entry) : null);
                case "mute":
//                    mutes.notifyMuted(player, added ? punishment(entry) : null);
            }
        }

        @Override
        public void entryAdded(Entry entry) {
            onEntry(entry, true);
        }

        @Override
        public void entryRemoved(Entry entry) {
            onEntry(entry, false);
        }
    }
}
