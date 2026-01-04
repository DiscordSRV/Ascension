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

package com.discordsrv.common.feature.mutesync;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.exception.RestErrorResponseException;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.member.role.DiscordMemberRoleAddEvent;
import com.discordsrv.api.events.discord.member.role.DiscordMemberRoleRemoveEvent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.punishment.AbstractPunishmentSyncModule;
import com.discordsrv.common.abstraction.sync.RoleSyncModuleUtil;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.enums.mutes.MuteSyncDiscordAction;
import com.discordsrv.common.abstraction.sync.enums.mutes.MuteSyncDiscordTrigger;
import com.discordsrv.common.abstraction.sync.result.DiscordPermissionResult;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.sync.MuteSyncConfig;
import com.discordsrv.common.feature.mutesync.enums.MuteSyncCause;
import com.discordsrv.common.feature.mutesync.enums.MuteSyncResult;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.common.util.Game;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MuteSyncModule extends AbstractPunishmentSyncModule<MuteSyncConfig> {

    public MuteSyncModule(DiscordSRV discordSRV) {
        super(discordSRV, "MUTE_SYNC");
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Arrays.asList(DiscordGatewayIntent.GUILD_MODERATION, DiscordGatewayIntent.GUILD_MEMBERS);
    }

    public void notifyMuted(IPlayer player, @Nullable Punishment punishment) {
        gameChanged(MuteSyncCause.PLAYER_MUTED, Someone.of(discordSRV, player.uniqueId()), Game.INSTANCE, punishment);
    }

    @Override
    public String syncName() {
        return "Mute Sync";
    }

    @Override
    public String logFileName() {
        return "mutesync";
    }

    @Override
    public String gameTerm() {
        return "game mute";
    }

    @Override
    public String discordTerm() {
        return "Discord mute";
    }

    @Override
    public List<MuteSyncConfig> configs() {
        return Collections.singletonList(discordSRV.config().muteSync);
    }

    @Subscribe
    public void onMemberTimeoutChanged(GuildMemberUpdateTimeOutEvent event) {
        boolean isTimedOut = event.getNewTimeOutEnd() != null;
        handleDiscordTimeoutChange(event.getGuild(), event.getUser(), isTimedOut);
    }

    @Subscribe
    public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
        AuditLogEntry entry = event.getEntry();
        AuditLogChange logChange = entry.getChangeByKey(AuditLogKey.MEMBER_TIME_OUT);
        if (logChange == null) {
            return;
        }

        if (!shouldHandleDiscordTimeoutChanges()) return;

        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        List<MuteSyncConfig> configs = configsForDiscord.get(guildId);
        MuteSyncConfig config = configs.isEmpty() ? null : configs.get(0);
        if (config == null) {
            return;
        }

        long punisherId = entry.getUserIdLong();

        // This user should be cacheable as they just made an auditable action
        User punisher = event.getJDA().getUserById(punisherId);
        Member punisherMember = punisher != null ? guild.getMember(punisher) : null;

        MinecraftComponent punisherName = discordSRV.componentFactory().textBuilder(config.discordToMinecraft.punisherFormat)
                .addContext(punisher, punisherMember)
                .build();

        long mutedUserId = entry.getTargetIdLong();

        // Apply punishments instantly when audit log events arrive.
        if (logChange.getNewValue() != null) {
            upsertEvent(guildId, mutedUserId, true, MuteSyncCause.TIMEOUT_ADDED).applyPunishment(new Punishment(
                     Instant.parse(logChange.getNewValue()),
                     ComponentUtil.fromPlain(entry.getReason()),
                     punisherName
            ), MuteSyncCause.TIMEOUT_ADDED);
        } else {
            upsertEvent(guildId, mutedUserId, false, MuteSyncCause.TIMEOUT_REMOVED).applyPunishment(null, MuteSyncCause.TIMEOUT_REMOVED);
        }
    }

    @Subscribe
    public void onRoleAdd(DiscordMemberRoleAddEvent event) {
        handleRoleChanges(event.getMember(), event.getRoles(), true);
    }

    @Subscribe
    public void onRoleRemove(DiscordMemberRoleRemoveEvent event) {
        handleRoleChanges(event.getMember(), event.getRoles(), false);
    }

    private void handleRoleChanges(DiscordGuildMember member, List<DiscordRole> roles, boolean added) {
        MuteSyncConfig config = discordSRV.config().muteSync;
        if ((config.minecraftToDiscord.action == MuteSyncDiscordAction.ROLE || config.minecraftToDiscord.fallbackToRoleIfTimeoutTooLong) && roles.stream().anyMatch(role -> config.mutedRoleId == role.getId())) {
            if (config.discordToMinecraft.trigger != MuteSyncDiscordTrigger.TIMEOUT) {
                upsertEvent(roles.getFirst().getGuild().getId(), member.getUser().getId(), added, MuteSyncCause.MUTED_ROLE_CHANGED).applyPunishment(added ? new Punishment(null, null, null) : null, MuteSyncCause.MUTED_ROLE_CHANGED);
            } else {
                if (config.minecraftToDiscord.fallbackToRoleIfTimeoutTooLong) {
                    logger().debug(String.format(
                            "Handling muted role change for %s as a fallback for timeout-based mute sync.",
                            member.getUser().getAsTag()
                    ));
                    upsertEvent(roles.getFirst().getGuild().getId(), member.getUser().getId(), added, MuteSyncCause.MUTED_ROLE_CHANGED).applyPunishment(added ? new Punishment(null, null, null) : null, MuteSyncCause.MUTED_ROLE_CHANGED);
                } else {
                    logger().debug(String.format(
                            "Ignoring muted role change for %s because role changes are not configured to affect the game mute status.",
                            member.getUser().getAsTag()
                    ));
                }
            }
        }
    }

    private boolean shouldHandleDiscordTimeoutChanges() {
        return discordSRV.config().muteSync.discordToMinecraft.trigger != MuteSyncDiscordTrigger.ROLE; // If not equal to this then it will be one of the other 2
    }

    private void handleDiscordTimeoutChange(Guild guild, User user, boolean newState) {
        if (!shouldHandleDiscordTimeoutChanges()) {
            logger().debug(String.format("Not handling Discord timeout/untimeout for %s because doing so is disabled in the config", user.getAsTag()));
            return;
        }
        
        upsertEvent(guild.getIdLong(), user.getIdLong(), newState, MuteSyncCause.TIMEOUT_REMOVED);
    }

    private Task<@Nullable Punishment> getMuteOrMuteRole(Guild guild, long userId, MuteSyncConfig config) {
        UserSnowflake snowflake = UserSnowflake.fromId(userId);

        if (!shouldHandleDiscordTimeoutChanges()) return getMutedRole(guild, snowflake, config); // Ignoring timeouts entirely

        return discordSRV.discordAPI().toTask(guild.retrieveMember(snowflake))
                .thenApply(this::punishment)
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        return null;
                    }

                    throw t;
                })
                .whenFailed(f -> getMutedRole(guild, snowflake, config));
    }

    private Task<@Nullable Punishment> getMutedRole(Guild guild, UserSnowflake snowflake, MuteSyncConfig config) {
        return discordSRV.discordAPI().toTask(guild.retrieveMember(snowflake))
                .thenApply(member -> {
                    if ((config.minecraftToDiscord.action == MuteSyncDiscordAction.ROLE || config.minecraftToDiscord.fallbackToRoleIfTimeoutTooLong) && member.getRoles().stream().anyMatch(role -> config.mutedRoleId == role.getIdLong())) {
                        return new Punishment(null, null, null);
                    }

                    return null;
                })
                .mapException(RestErrorResponseException.class, t -> {
                    if (t.getErrorCode() == ErrorResponse.UNKNOWN_MEMBER.getCode()) {
                        return null;
                    }
                    
                    throw t;
                });
    }

    @Override
    protected Task<Punishment> getDiscord(MuteSyncConfig config, Someone.Resolved someone) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            // Server doesn't exist
            return Task.failed(new SyncFail(MuteSyncResult.GUILD_DOESNT_EXIST));
        }

        return getMuteOrMuteRole(guild.asJDA(), someone.userId(), config);
    }

    private Punishment punishment(Member member) {
        return member.getTimeOutEnd() != null ? new Punishment(member.getTimeOutEnd().toInstant(), ComponentUtil.fromPlain(""), null) : null;
    }

    @Override
    protected Task<Punishment> getGame(MuteSyncConfig config, Someone.Resolved someone) {
        PunishmentModule.Mutes mutes = discordSRV.getModule(PunishmentModule.Mutes.class);
        if (mutes == null) {
            return Task.failed(new SyncFail(MuteSyncResult.NO_PUNISHMENT_INTEGRATION));
        }

        return mutes.getMute(someone.playerUUID());
    }

    @Override
    protected Task<ISyncResult> applyDiscord(MuteSyncConfig config, Someone.Resolved someone, Punishment newState) {
        if (config.direction == SyncDirection.DISCORD_TO_MINECRAFT) {
            return Task.completed(GenericSyncResults.WRONG_DIRECTION);
        }

        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            return Task.completed(MuteSyncResult.GUILD_DOESNT_EXIST);
        }

        ISyncResult permissionFailResult = DiscordPermissionResult.check(guild.asJDA(), Collections.singleton(Permission.MODERATE_MEMBERS));
        if (permissionFailResult != null) {
            return Task.completed(permissionFailResult);
        }

        PlaceholderService placeholderService = discordSRV.placeholderService();
        UserSnowflake snowflake = UserSnowflake.fromId(someone.userId());
        switch (config.minecraftToDiscord.action) {
            case TIMEOUT:
                if (newState != null) {
                    if (newState.until() == null || newState.until().isAfter(Instant.now().plus(28, ChronoUnit.DAYS))) {
                        if (config.minecraftToDiscord.fallbackToRoleIfTimeoutTooLong) {
                            logger().debug(String.format(
                                    "Punishment until is longer than 28 days for %s, falling back to applying muted role.",
                                    someone
                            ));
                            return RoleSyncModuleUtil.doRoleChange(discordSRV, someone, config.mutedRoleId, true);
                        }

                        return Task.failed(new SyncFail(MuteSyncResult.PUNISHMENT_TOO_LONG));
                    }

                    return Task.of(guild.asJDA().timeoutUntil(snowflake, newState.until())
                            .reason(placeholderService.replacePlaceholders(config.minecraftToDiscord.muteReasonFormat, newState))
                            .submit()
                            .thenApply(v -> GenericSyncResults.ADD_DISCORD));
                } else {
                    return Task.of(guild.asJDA().removeTimeout(snowflake)
                            .reason(placeholderService.replacePlaceholders(config.minecraftToDiscord.unmuteReasonFormat))
                            .submit()
                            .thenApply(v -> GenericSyncResults.REMOVE_DISCORD));
                }
            case ROLE:
                boolean isMute = newState != null;
                return RoleSyncModuleUtil.doRoleChange(discordSRV, someone, config.mutedRoleId, isMute);
            default:
                return Task.failed(new SyncFail(MuteSyncResult.INVALID_CONFIG));
        }
    }

    @Override
    protected Task<ISyncResult> applyGame(MuteSyncConfig config, Someone.Resolved someone, Punishment newState) {
        if (config.direction == SyncDirection.MINECRAFT_TO_DISCORD) {
            return Task.completed(GenericSyncResults.WRONG_DIRECTION);
        }

        // This does not catch all circumstances under which a role change could precipitate a change in game state
        if (newState != null && newState.punisher() == null && newState.until() == null && newState.reason() == null) {
            // This punishment is a role, not a mute
            if (config.discordToMinecraft.trigger == MuteSyncDiscordTrigger.TIMEOUT && !config.minecraftToDiscord.fallbackToRoleIfTimeoutTooLong) { // Role change should not cause game change
                return Task.completed(MuteSyncResult.ROLE_CHANGE_CANNOT_CHANGE_GAME);
            }
        }

        PunishmentModule.Mutes mutes = discordSRV.getModule(PunishmentModule.Mutes.class);
        if (mutes == null) {
            return Task.completed(MuteSyncResult.NO_PUNISHMENT_INTEGRATION);
        }

        UUID playerUUID = someone.playerUUID();
        if (newState != null) {
            MinecraftComponent reason = discordSRV.componentFactory().textBuilder(config.discordToMinecraft.muteReasonFormat)
                    .addContext(newState)
                    .build();
            MinecraftComponent punisher = newState.punisher() != null
                    ? newState.punisher() // has contexts for user and member so ideally we should use this
                    : discordSRV.componentFactory().textBuilder(config.discordToMinecraft.punisherFormat)
                        .addContext(newState)
                        .build();

            return mutes.addMute(playerUUID, newState.until(), reason, punisher)
                    .then(v -> {
                        IPlayer player = discordSRV.playerProvider().player(playerUUID);
                        if (player == null) {
                            return Task.completed(null);
                        }

                        MinecraftComponent muteNotificationMessage = discordSRV.componentFactory()
                                .textBuilder(config.discordToMinecraft.muteNotificationMessage)
                                .addContext(newState)
                                .build();

                        player.sendMessage(ComponentUtil.fromAPI(muteNotificationMessage));
                        return Task.completed(null);
                    })
                    .thenApply(v -> GenericSyncResults.ADD_GAME);
        } else {
            return mutes.removeMute(playerUUID)
                    .then(v -> {
                        IPlayer player = discordSRV.playerProvider().player(playerUUID);
                        if (player == null) {
                            return Task.completed(null);
                        }

                        MinecraftComponent unmuteNotificationMessage = discordSRV.componentFactory()
                                .textBuilder(config.discordToMinecraft.unmuteNotificationMessage)
                                .build();

                        player.sendMessage(ComponentUtil.fromAPI(unmuteNotificationMessage));
                        return Task.completed(null);
                    })
                    .thenApply(v -> GenericSyncResults.REMOVE_GAME);
        }
    }

}
