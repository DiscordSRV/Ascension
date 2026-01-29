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
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.feature.bansync.BanSyncModule;
//import com.discordsrv.common.feature.mutesync.MuteSyncModule;
import com.discordsrv.common.util.ComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.arim.libertybans.api.*;
import space.arim.libertybans.api.event.PostPardonEvent;
import space.arim.libertybans.api.event.PostPunishEvent;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.events.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LibertyBansIntegration extends PluginIntegration<BukkitDiscordSRV>
        implements PunishmentModule.Bans, PunishmentModule.Mutes {

    private Omnibus omnibus;
    private LibertyBans libertyBans;

    private List<RegisteredListener> listeners;

    public LibertyBansIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "LIBERTYBANS_INTEGRATION"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "LibertyBans";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("space.arim.libertybans.api.LibertyBans");
        } catch (ClassNotFoundException ignored) {
            return false;
        }
        return super.isEnabled();
    }

    @Override
    public void enable() {
        this.omnibus = OmnibusProvider.getOmnibus();
        this.libertyBans = omnibus.getRegistry().getProvider(LibertyBans.class).orElse(null);

        this.omnibus.getEventBus().registerListeningMethods(this);
    }

    @Override
    public void disable() {
        this.omnibus.getEventBus().unregisterListeningMethods(this);
        this.libertyBans = null;
        this.omnibus = null;
    }

    @Override
    public Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        return Task.of(libertyBans.getSelector()
                .selectionBuilder()
                .type(PunishmentType.BAN)
                .victim(PlayerVictim.of(playerUUID))
                .selectActiveOnly()
                .build()
                .getFirstSpecificPunishment()
                .thenApply((punishment -> punishment.map(this::punishment).orElse(null)))
                .toCompletableFuture());
    }

    @Override
    public Task<Void> addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        DraftPunishmentBuilder draft = libertyBans.getDrafter()
                .draftBuilder()
                .type(PunishmentType.BAN)
                .victim(PlayerVictim.of(playerUUID))
                .reason(reason != null ? reason.asPlainString() : "Banned by DiscordSRV ban sync");
        draft = until != null ? draft.duration(Duration.between(Instant.now(), until)) : draft;

        return Task.of(draft.build().enactPunishment().toCompletableFuture().thenApply(__ -> null));
    }

    @Override
    public Task<Void> removeBan(@NotNull UUID playerUUID) {
        return Task.of(libertyBans.getRevoker()
                .revokeByTypeAndVictim(PunishmentType.BAN, PlayerVictim.of(playerUUID))
                .undoPunishment()
                .toCompletableFuture()
                .thenApply(__ -> null));
    }

    @Override
    public Task<@Nullable Punishment> getMute(@NotNull UUID playerUUID) {
        return Task.of(libertyBans.getSelector()
                .selectionBuilder()
                .type(PunishmentType.MUTE)
                .victim(PlayerVictim.of(playerUUID))
                .selectActiveOnly()
                .build()
                .getFirstSpecificPunishment()
                .thenApply((punishment -> punishment.map(this::punishment).orElse(null)))
                .toCompletableFuture());
    }


    @Override
    public Task<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        DraftPunishmentBuilder draft = libertyBans.getDrafter()
                .draftBuilder()
                .type(PunishmentType.MUTE)
                .victim(PlayerVictim.of(playerUUID))
                .reason(reason != null ? reason.asPlainString() : "Muted by DiscordSRV mute sync");
        draft = until != null ? draft.duration(Duration.between(Instant.now(), until)) : draft;

        return Task.of(draft.build().enactPunishment().toCompletableFuture().thenApply(__ -> null));
    }

    @Override
    public Task<Void> removeMute(@NotNull UUID playerUUID) {
        return Task.of(libertyBans.getRevoker()
                .revokeByTypeAndVictim(PunishmentType.MUTE, PlayerVictim.of(playerUUID))
                .undoPunishment()
                .toCompletableFuture()
                .thenApply(__ -> null));
    }

    private @Nullable Punishment punishment(space.arim.libertybans.api.punish.Punishment punishment) {
        if (punishment == null) return null;

        String operatorName = null;
        if (punishment.getOperator() instanceof PlayerOperator) {
            IOfflinePlayer player = discordSRV.playerProvider().lookupOfflinePlayer(((PlayerOperator) punishment.getOperator()).getUUID()).join();
            if  (player != null) {
                operatorName = player.username();
            }
        } else if (punishment.getOperator() instanceof ConsoleOperator) {
            operatorName = "Console";
        }

        return new Punishment(
                punishment.getEndDate(),
                punishment.getReason() != null ? ComponentUtil.fromPlain(punishment.getReason()) : null,
                operatorName != null ? ComponentUtil.fromPlain(operatorName) : null
        );
    }

    @ListeningMethod()
    public void onPunishment(PostPunishEvent event) {
        BanSyncModule bans = discordSRV.getModule(BanSyncModule.class);
//        MuteSyncModule mutes = discordSRV.getModule(MuteSyncModule.class);
        if (!(event.getPunishment().getVictim() instanceof PlayerVictim)) return;
        PlayerVictim victim = (PlayerVictim) event.getPunishment().getVictim();

        switch (event.getPunishment().getType()) {
            case BAN:
                if (bans != null) {
                    IPlayer player = discordSRV.playerProvider().player(victim.getUUID());
                    if (player == null) {
                        throw new RuntimeException("Player " + victim.getUUID() + " not present in player provider");
                    }

                    bans.notifyBanned(player, punishment(event.getPunishment()));
                }
                break;
            case MUTE:
//                if (mutes != null) {
//                    IPlayer player = discordSRV.playerProvider().player(victim.getUUID());
//                    if (player == null) {
//                        throw new RuntimeException("Player " + victim.getUUID() + " not present in player provider");
//                    }
//
//                    mutes.notifyMuted(player, punishment(event.getPunishment()));
//                }
        }
    }

    @ListeningMethod()
    public void onPardon(PostPardonEvent event) {
        BanSyncModule bans = discordSRV.getModule(BanSyncModule.class);
//        MuteSyncModule mutes = discordSRV.getModule(MuteSyncModule.class);
        if (!(event.getPunishment().getVictim() instanceof PlayerVictim)) return;
        PlayerVictim victim = (PlayerVictim) event.getPunishment().getVictim();

        switch (event.getPunishment().getType()) {
            case BAN:
                if (bans != null) {
                    IPlayer player = discordSRV.playerProvider().player(victim.getUUID());
                    if (player == null) {
                        throw new RuntimeException("Player " + victim.getUUID() + " not present in player provider");
                    }

                    bans.notifyBanned(player, null);
                }
                break;
//            case MUTE:
//                if (mutes != null) {
//                    IPlayer player = discordSRV.playerProvider().player(victim.getUUID());
//                    if (player == null) {
//                        throw new RuntimeException("Player " + victim.getUUID() + " not present in player provider");
//                    }
//
//                    mutes.notifyMuted(player, null);
//                }
//                break;
        }
    }
}
