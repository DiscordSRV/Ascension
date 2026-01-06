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
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.util.ComponentUtil;
import me.leoko.advancedban.manager.PunishmentManager;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public class AdvancedBanIntegration extends PluginIntegration<BukkitDiscordSRV>
        implements PunishmentModule.Bans, PunishmentModule.Mutes, Listener {

    private PunishmentManager punishmentManager;

    public AdvancedBanIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEDBAN_INTEGRATION"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "AdvancedBan";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("me.leoko.advancedban.bukkit.BukkitMain");
        } catch (ClassNotFoundException ignored) {
            return false;
        }
        return super.isEnabled();
    }

    @Override
    public void enable() {
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());
        this.punishmentManager = PunishmentManager.get();
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        this.punishmentManager = null;
    }


    @Override
    public Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        return Task.completed(this.punishment(punishmentManager.getBan(playerUUID.toString())));
    }

    @Override
    public Task<Void> addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        String name = discordSRV.server().getOfflinePlayer(playerUUID).getName();
        me.leoko.advancedban.utils.Punishment.create(
                name,
                playerUUID.toString(),
                reason != null ? reason.asPlainString() : "Banned by an operator",
                punisher.asPlainString(),
                me.leoko.advancedban.utils.PunishmentType.BAN,
                until != null ? until.toEpochMilli() : -1L,
                "",
                false
        );
        return null;
    }

    @Override
    public Task<Void> removeBan(@NotNull UUID playerUUID) {
        punishmentManager.getBan(playerUUID.toString()).delete();
        return null;
    }

    @Override
    public Task<@Nullable Punishment> getMute(@NotNull UUID playerUUID) {
        return Task.completed(this.punishment(punishmentManager.getMute(playerUUID.toString())));
    }

    @Override
    public Task<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        String name = discordSRV.server().getOfflinePlayer(playerUUID).getName();
        me.leoko.advancedban.utils.Punishment.create(
                name,
                playerUUID.toString(),
                reason != null ? reason.asPlainString() : "Muted by an operator",
                punisher.asPlainString(),
                me.leoko.advancedban.utils.PunishmentType.MUTE,
                until != null ? until.toEpochMilli() : -1L,
                "",
                false
        );
        return null;
    }

    @Override
    public Task<Void> removeMute(@NotNull UUID playerUUID) {
        punishmentManager.getMute(playerUUID.toString()).delete();
        return null;
    }

    private Punishment punishment(me.leoko.advancedban.utils.Punishment punishment) {
        return new Punishment(
                punishment.getEnd() != -1 ? Instant.ofEpochMilli(punishment.getEnd()) : null,
                punishment.getReason() != null ? ComponentUtil.fromPlain(punishment.getReason()) : null,
                punishment.getOperator() != null ? ComponentUtil.fromPlain(punishment.getOperator()) : null
        );
    }
}
