package com.discordsrv.common.bansync;

import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.linking.AccountLinkedEvent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.bansync.enums.BanSyncCause;
import com.discordsrv.common.bansync.enums.BanSyncResult;
import com.discordsrv.common.config.main.BanSyncConfig;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.someone.Someone;
import com.discordsrv.common.sync.AbstractSyncModule;
import com.discordsrv.common.sync.SyncFail;
import com.discordsrv.common.sync.cause.GenericSyncCauses;
import com.discordsrv.common.sync.result.ISyncResult;
import com.discordsrv.common.sync.enums.SyncDirection;
import com.discordsrv.common.sync.result.GenericSyncResults;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BanSyncModule extends AbstractSyncModule<DiscordSRV, BanSyncConfig, Void, Long, Punishment> {

    private final Map<Long, PunishmentEvent> events = new ConcurrentHashMap<>();

    public BanSyncModule(DiscordSRV discordSRV) {
        super(discordSRV, "BAN_SYNC");
    }

    @Override
    public boolean isEnabled() {
        if (discordSRV.config().banSync.serverId == 0) {
            //return false; // TODO
        }

        return super.isEnabled();
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Collections.singleton(DiscordGatewayIntent.GUILD_MODERATION);
    }

    public void notifyBanned(IPlayer player, @Nullable Punishment punishment) {
        gameChanged(BanSyncCause.PLAYER_BANNED, Someone.of(player.uniqueId()), null, punishment);
    }

    @Override
    public String syncName() {
        return "Ban sync";
    }

    @Override
    public String logName() {
        return "bansync";
    }

    @Override
    public String gameTerm() {
        return "ban";
    }

    @Override
    public String discordTerm() {
        return "ban";
    }

    @Override
    public List<BanSyncConfig> configs() {
        return Collections.singletonList(discordSRV.config().banSync);
    }

    @Override
    protected boolean isActive(Punishment state) {
        return state != null;
    }

    private PunishmentEvent upsertEvent(long userId, boolean newState) {
        return events.computeIfAbsent(userId, key -> new PunishmentEvent(userId, newState));
    }

    private class PunishmentEvent {

        private final long userId;
        private final boolean newState;
        private final Future<?> future;

        public PunishmentEvent(long userId, boolean newState) {
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
                punishment = new Punishment(null, null, null);
            }
            gameChanged(
                    GenericSyncCauses.LINK,
                    Someone.of(userId),
                    null,
                    punishment
            );
        }
    }

    @Subscribe
    public void onGuildBan(GuildBanEvent event) {
        upsertEvent(event.getUser().getIdLong(), true);
    }

    @Subscribe
    public void onGuildUnban(GuildUnbanEvent event) {
        upsertEvent(event.getUser().getIdLong(), false);
    }

    @Subscribe
    public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
        AuditLogEntry entry = event.getEntry();
        ActionType actionType = entry.getType();
        if (actionType != ActionType.BAN && actionType != ActionType.UNBAN) {
            return;
        }

        long punisherId = entry.getUserIdLong();
        User punisher = event.getJDA().getUserById(punisherId);
        String punishmentName = punisher != null ? punisher.getName() : Long.toUnsignedString(punisherId);

        Punishment punishment = new Punishment(null, entry.getReason(), punishmentName);
        long bannedUserId = entry.getTargetIdLong();

        // Apply punishments instantly when audit log events arrive.
        if (actionType == ActionType.BAN) {
            upsertEvent(bannedUserId, true).applyPunishment(punishment);
        } else {
            upsertEvent(bannedUserId, false).applyPunishment(punishment);
        }
    }

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        BanSyncConfig config = discordSRV.config().banSync;
        if (config.resyncUponLinking) {
            resyncAll(GenericSyncCauses.LINK, Someone.of(event.getPlayerUUID(), event.getUserId()));
        }
    }

    private CompletableFuture<Guild.@Nullable Ban> getBan(Guild guild, long userId) {
        return guild.retrieveBan(UserSnowflake.fromId(userId)).submit().exceptionally(t -> {
            if (t instanceof ErrorResponseException && ((ErrorResponseException) t).getErrorResponse() == ErrorResponse.UNKNOWN_BAN) {
                return null;
            }
            throw (RuntimeException) t;
        });
    }

    @Override
    protected CompletableFuture<Punishment> getDiscord(BanSyncConfig config, long userId) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return CompletableFutureUtil.failed(new SyncFail(BanSyncResult.NO_DISCORD_CONNECTION));
        }

        Guild guild = jda.getGuildById(config.serverId);
        if (guild == null) {
            // Server doesn't exist
            return CompletableFutureUtil.failed(new SyncFail(BanSyncResult.GUILD_DOESNT_EXIST));
        }

        return getBan(guild, userId).thenApply(this::punishment);
    }

    private Punishment punishment(Guild.Ban ban) {
        return ban != null ? new Punishment(null, ban.getReason(), ban.getUser().getName()) : null;
    }

    @Override
    protected CompletableFuture<Punishment> getGame(BanSyncConfig config, UUID playerUUID) {
        PunishmentModule.Bans bans = discordSRV.getModule(PunishmentModule.Bans.class);
        if (bans == null) {
            return CompletableFutureUtil.failed(new SyncFail(BanSyncResult.NO_PUNISHMENT_INTEGRATION));
        }

        return bans.getBan(playerUUID);
    }

    @Override
    protected CompletableFuture<ISyncResult> applyDiscord(BanSyncConfig config, long userId, Punishment state) {
        if (config.direction == SyncDirection.DISCORD_TO_MINECRAFT) {
            return CompletableFuture.completedFuture(GenericSyncResults.WRONG_DIRECTION);
        }

        JDA jda = discordSRV.jda();
        if (jda == null) {
            return CompletableFuture.completedFuture(BanSyncResult.NO_DISCORD_CONNECTION);
        }

        Guild guild = jda.getGuildById(config.serverId);
        if (guild == null) {
            // Server doesn't exist
            return CompletableFuture.completedFuture(BanSyncResult.GUILD_DOESNT_EXIST);
        }

        UserSnowflake snowflake = UserSnowflake.fromId(userId);
        if (state != null) {
            return guild.ban(snowflake, config.discordMessageHoursToDelete, TimeUnit.HOURS)
                    .reason(discordSRV.placeholderService().replacePlaceholders(config.discordBanReasonFormat, state))
                    .submit()
                    .thenApply(v -> GenericSyncResults.ADD_DISCORD);
        } else {
            return guild.unban(snowflake)
                    .reason(discordSRV.placeholderService().replacePlaceholders(config.discordUnbanReasonFormat))
                    .submit()
                    .thenApply(v -> GenericSyncResults.REMOVE_DISCORD);
        }
    }

    @Override
    protected CompletableFuture<ISyncResult> applyGame(BanSyncConfig config, UUID playerUUID, Punishment state) {
        if (config.direction == SyncDirection.MINECRAFT_TO_DISCORD) {
            return CompletableFuture.completedFuture(GenericSyncResults.WRONG_DIRECTION);
        }

        PunishmentModule.Bans bans = discordSRV.getModule(PunishmentModule.Bans.class);
        if (bans == null) {
            return CompletableFuture.completedFuture(BanSyncResult.NO_PUNISHMENT_INTEGRATION);
        }

        if (state != null) {
            String reason = discordSRV.placeholderService().replacePlaceholders(config.gameBanReasonFormat, state);
            String punisher = discordSRV.placeholderService().replacePlaceholders(config.gamePunisherFormat, state);
            return bans.addBan(playerUUID, null, reason, punisher)
                    .thenApply(v -> GenericSyncResults.ADD_GAME);
        } else {
            return bans.removeBan(playerUUID).thenApply(v -> GenericSyncResults.REMOVE_GAME);
        }
    }

}
