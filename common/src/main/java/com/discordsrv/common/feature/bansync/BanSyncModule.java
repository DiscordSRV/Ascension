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

package com.discordsrv.common.feature.bansync;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.cause.GenericSyncCauses;
import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.result.DiscordPermissionResult;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.sync.BanSyncConfig;
import com.discordsrv.common.feature.bansync.enums.BanSyncCause;
import com.discordsrv.common.feature.bansync.enums.BanSyncResult;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.common.util.Game;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BanSyncModule extends AbstractSyncModule<DiscordSRV, BanSyncConfig, Game, Long, Punishment> {

    private final Map<Long, PunishmentEvent> events = new ConcurrentHashMap<>();

    public BanSyncModule(DiscordSRV discordSRV) {
        super(discordSRV, "BAN_SYNC");
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Collections.singleton(DiscordGatewayIntent.GUILD_MODERATION);
    }

    public void notifyBanned(IPlayer player, @Nullable Punishment punishment) {
        gameChanged(BanSyncCause.PLAYER_BANNED, Someone.of(discordSRV, player.uniqueId()), Game.INSTANCE, punishment);
    }

    @Override
    public String syncName() {
        return "Ban Sync";
    }

    @Override
    public String logFileName() {
        return "bansync";
    }

    @Override
    public String gameTerm() {
        return "game ban";
    }

    @Override
    public String discordTerm() {
        return "Discord ban";
    }

    @Override
    public List<BanSyncConfig> configs() {
        return Collections.singletonList(discordSRV.config().banSync);
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(Punishment one, Punishment two) {
        boolean oneActive = one != null;
        boolean twoActive = two != null;
        return (oneActive == twoActive) ? GenericSyncResults.both(oneActive) : null;
    }

    @Override
    public Punishment getRemovedState() {
        return null;
    }

    private PunishmentEvent upsertEvent(long guildId, long userId, boolean newState) {
        return events.computeIfAbsent(userId, key -> new PunishmentEvent(guildId, userId, newState));
    }

    private class PunishmentEvent {

        private final long guildId;
        private final long userId;
        private final boolean newState;
        private final Future<?> future;

        public PunishmentEvent(long guildId, long userId, boolean newState) {
            this.guildId = guildId;
            this.userId = userId;
            this.newState = newState;

            // Run in 5s if an audit log event doesn't arrive
            this.future = discordSRV.scheduler().runLater(() -> applyPunishment(null), Duration.ofSeconds(5));
        }

        public void applyPunishment(@Nullable Punishment punishment) {
            if (!future.cancel(false)) {
                return;
            }

            if (newState && punishment == null) {
                punishment = Punishment.UNKNOWN;
            }
            discordChanged(
                    GenericSyncCauses.LINK,
                    Someone.of(discordSRV, userId),
                    guildId,
                    punishment
            );
            events.remove(userId);
        }
    }

    @Subscribe
    public void onGuildBan(GuildBanEvent event) {
        upsertEvent(event.getGuild().getIdLong(), event.getUser().getIdLong(), true);
    }

    @Subscribe
    public void onGuildUnban(GuildUnbanEvent event) {
        upsertEvent(event.getGuild().getIdLong(), event.getUser().getIdLong(), false);
    }

    @Subscribe
    public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
        AuditLogEntry entry = event.getEntry();
        ActionType actionType = entry.getType();
        if (actionType != ActionType.BAN && actionType != ActionType.UNBAN) {
            return;
        }

        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        List<BanSyncConfig> configs = configsForDiscord.get(guildId);
        BanSyncConfig config = configs.isEmpty() ? null : configs.get(0);
        if (config == null) {
            return;
        }

        long punisherId = entry.getUserIdLong();

        // This user should be cacheable as they just made an auditable action
        User punisher = event.getJDA().getUserById(punisherId);
        Member punisherMember = punisher != null ? guild.getMember(punisher) : null;

        MinecraftComponent punisherName = discordSRV.componentFactory().textBuilder(config.gamePunisherFormat)
                .addContext(punisher, punisherMember)
                .build();

        long bannedUserId = entry.getTargetIdLong();

        // Apply punishments instantly when audit log events arrive.
        if (actionType == ActionType.BAN) {
            upsertEvent(guildId, bannedUserId, true).applyPunishment(new Punishment(
                    null,
                    ComponentUtil.fromPlain(entry.getReason()),
                    punisherName
            ));
        } else {
            upsertEvent(guildId, bannedUserId, false).applyPunishment(null);
        }
    }

    private Task<Guild.@Nullable Ban> getBan(Guild guild, long userId) {
        return discordSRV.discordAPI().toTask(guild.retrieveBan(UserSnowflake.fromId(userId)))
                .mapException(ErrorResponseException.class, t -> {
                    if (t.getErrorResponse() == ErrorResponse.UNKNOWN_BAN) {
                        return null;
                    }
                    throw t;
                });
    }

    @Override
    protected Task<Punishment> getDiscord(BanSyncConfig config, Someone.Resolved someone) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            // Server doesn't exist
            return Task.failed(new SyncFail(BanSyncResult.GUILD_DOESNT_EXIST));
        }

        return getBan(guild.asJDA(), someone.userId()).thenApply(this::punishment);
    }

    private Punishment punishment(Guild.Ban ban) {
        return ban != null ? new Punishment(null, ComponentUtil.fromPlain(ban.getReason()), null) : null;
    }

    @Override
    protected Task<Punishment> getGame(BanSyncConfig config, Someone.Resolved someone) {
        PunishmentModule.Bans bans = discordSRV.getModule(PunishmentModule.Bans.class);
        if (bans == null) {
            return Task.failed(new SyncFail(BanSyncResult.NO_PUNISHMENT_INTEGRATION));
        }

        return bans.getBan(someone.playerUUID());
    }

    @Override
    protected Task<ISyncResult> applyDiscord(BanSyncConfig config, Someone.Resolved someone, Punishment newState) {
        if (config.direction == SyncDirection.DISCORD_TO_MINECRAFT) {
            return Task.completed(GenericSyncResults.WRONG_DIRECTION);
        }

        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            return Task.completed(BanSyncResult.GUILD_DOESNT_EXIST);
        }

        ISyncResult permissionFailResult = DiscordPermissionResult.check(guild.asJDA(), Collections.singleton(Permission.BAN_MEMBERS));
        if (permissionFailResult != null) {
            return Task.completed(permissionFailResult);
        }

        UserSnowflake snowflake = UserSnowflake.fromId(someone.userId());
        if (newState != null) {
            return discordSRV.discordAPI().toTask(
                    guild.asJDA().ban(snowflake, config.discordMessageHoursToDelete, TimeUnit.HOURS)
                            .reason(discordSRV.placeholderService().replacePlaceholders(config.discordBanReasonFormat, newState))
                    )
                    .thenApply(v -> GenericSyncResults.ADD_DISCORD);
        } else {
            return discordSRV.discordAPI().toTask(
                    guild.asJDA().unban(snowflake)
                            .reason(discordSRV.placeholderService().replacePlaceholders(config.discordUnbanReasonFormat))
                    )
                    .thenApply(v -> GenericSyncResults.REMOVE_DISCORD);
        }
    }

    @Override
    protected Task<ISyncResult> applyGame(BanSyncConfig config, Someone.Resolved someone, Punishment newState) {
        if (config.direction == SyncDirection.MINECRAFT_TO_DISCORD) {
            return Task.completed(GenericSyncResults.WRONG_DIRECTION);
        }

        PunishmentModule.Bans bans = discordSRV.getModule(PunishmentModule.Bans.class);
        if (bans == null) {
            return Task.completed(BanSyncResult.NO_PUNISHMENT_INTEGRATION);
        }

        UUID playerUUID = someone.playerUUID();
        if (newState != null) {
            MinecraftComponent reason = discordSRV.componentFactory().textBuilder(config.gameBanReasonFormat)
                    .addContext(newState)
                    .build();
            MinecraftComponent punisher = discordSRV.componentFactory().textBuilder(config.gamePunisherFormat)
                    .addContext(newState)
                    .build();

            return bans.addBan(playerUUID, null, reason, punisher)
                    .then(v -> {
                        IPlayer player = discordSRV.playerProvider().player(playerUUID);
                        if (player == null) {
                            return Task.completed(null);
                        }

                        MinecraftComponent kickMessage = discordSRV.componentFactory()
                                .textBuilder(config.gameKickReason)
                                .addContext(newState)
                                .build();

                        return player.kick(ComponentUtil.fromAPI(kickMessage));
                    })
                    .thenApply(v -> GenericSyncResults.ADD_GAME);
        } else {
            return bans.removeBan(playerUUID).thenApply(v -> GenericSyncResults.REMOVE_GAME);
        }
    }

}
