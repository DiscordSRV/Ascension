package com.discordsrv.common.bansync;

import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.linking.AccountLinkedEvent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.bansync.enums.BanSyncResult;
import com.discordsrv.common.config.main.BanSyncConfig;
import com.discordsrv.common.event.events.player.PlayerConnectedEvent;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.profile.Profile;
import com.discordsrv.common.sync.enums.SyncDirection;
import com.discordsrv.common.sync.enums.SyncSide;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BanSyncModule extends AbstractModule<DiscordSRV> {

    private final Map<Long, PunishmentEvent> events = new ConcurrentHashMap<>();

    public BanSyncModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public boolean isEnabled() {
        if (discordSRV.config().banSync.serverId == 0) {
            //return false;
        }

        return super.isEnabled();
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Collections.singleton(DiscordGatewayIntent.GUILD_MODERATION);
    }

    private Punishment punishment(Guild.Ban ban) {
        return ban != null ? new Punishment(null, ban.getReason(), ban.getUser().getName()) : null;
    }

    private CompletableFuture<Long> lookupLinkedAccount(UUID player) {
        return discordSRV.profileManager().lookupProfile(player)
                .thenApply(Profile::userId);
    }

    private CompletableFuture<UUID> lookupLinkedAccount(long userId) {
        return discordSRV.profileManager().lookupProfile(userId)
                .thenApply(Profile::playerUUID);
    }

    public void notifyBanned(IPlayer player, @Nullable Punishment punishment) {
        playerBanChange(player.uniqueId(), true, punishment);
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        playerBanChange(event.player().uniqueId(), false, null);
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

            userBanChange(userId, newState, punishment);
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
            resync(event.getPlayerUUID(), event.getUserId());
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

    public CompletableFuture<BanSyncResult> resync(UUID playerUUID) {
        return lookupLinkedAccount(playerUUID).thenCompose(userId -> {
            if (userId == null) {
                // Unlinked
                return null;
            }

            return resync(playerUUID, userId);
        });
    }

    public CompletableFuture<BanSyncResult> resync(long userId) {
        return lookupLinkedAccount(userId).thenCompose(playerUUID -> {
            if (playerUUID == null) {
                // Unlinked
                return null;
            }

            return resync(playerUUID, userId);
        });
    }

    public CompletableFuture<BanSyncResult> resync(UUID playerUUID, long userId) {
        return doResync(playerUUID, userId).whenComplete((r, t) -> {
            String label = playerUUID + ":" + Long.toUnsignedString(userId);
            if (t != null) {
                logger().error("Failed to update ban state for " + label, t);
            } else {
                logger().debug("Updated " + label + " ban state: " + r);
            }
        });
    }

    private CompletableFuture<BanSyncResult> doResync(UUID playerUUID, long userId) {
        BanSyncConfig config = discordSRV.config().banSync;

        SyncSide side = config.tieBreaker;
        if (side == null) {
            return CompletableFuture.completedFuture(BanSyncResult.INVALID_CONFIG);
        }

        switch (side) {
            case DISCORD:
                JDA jda = discordSRV.jda();
                if (jda == null) {
                    return CompletableFuture.completedFuture(BanSyncResult.NO_DISCORD_CONNECTION);
                }

                Guild guild = jda.getGuildById(config.serverId);
                if (guild == null) {
                    // Server doesn't exist
                    return CompletableFuture.completedFuture(BanSyncResult.GUILD_DOESNT_EXIST);
                }

                return getBan(guild, userId)
                        .thenCompose(ban -> changePlayerBanState(playerUUID, ban != null, punishment(ban)));
            case MINECRAFT:
                PunishmentModule.Bans bans = discordSRV.getModule(PunishmentModule.Bans.class);
                if (bans == null) {
                    return CompletableFuture.completedFuture(BanSyncResult.NO_PUNISHMENT_INTEGRATION);
                }

                return bans.getBan(playerUUID)
                        .thenCompose(punishment -> changeUserBanState(userId, punishment != null, punishment));
            default:
                throw new IllegalStateException("Missing side " + side.name());
        }
    }

    private void playerBanChange(UUID playerUUID, boolean newState, @Nullable Punishment punishment) {
        lookupLinkedAccount(playerUUID).thenCompose(userId -> {
            if (userId == null) {
                // Unlinked
                return null;
            }

            return changeUserBanState(userId, newState, punishment).whenComplete((r, t) -> {
                if (t != null) {
                    logger().error("Failed to update ban state for " + Long.toUnsignedString(userId), t);
                } else {
                    logger().debug("Updated " + Long.toUnsignedString(userId) + " ban state: " + r);
                }
            });
        });
    }

    private CompletableFuture<BanSyncResult> changeUserBanState(long userId, boolean newState, @Nullable Punishment punishment) {
        BanSyncConfig config = discordSRV.config().banSync;
        if (config.direction == SyncDirection.DISCORD_TO_MINECRAFT) {
            return CompletableFuture.completedFuture(BanSyncResult.WRONG_DIRECTION);
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
        return getBan(guild, userId).thenCompose(ban -> {
            if (ban == null) {
                if (newState) {
                    return guild.ban(snowflake, config.discordMessageHoursToDelete, TimeUnit.HOURS)
                            .reason(discordSRV.placeholderService().replacePlaceholders(config.discordBanReasonFormat, punishment))
                            .submit()
                            .thenApply(v -> BanSyncResult.BAN_USER);
                } else {
                    // Already unbanned
                    return CompletableFuture.completedFuture(BanSyncResult.ALREADY_IN_SYNC);
                }
            } else {
                if (newState) {
                    // Already banned
                    return CompletableFuture.completedFuture(BanSyncResult.ALREADY_IN_SYNC);
                } else {
                    return guild.unban(snowflake)
                            .reason(discordSRV.placeholderService().replacePlaceholders(config.discordUnbanReasonFormat, punishment))
                            .submit()
                            .thenApply(v -> BanSyncResult.UNBAN_USER);
                }
            }
        });
    }

    public void userBanChange(long userId, boolean newState, @Nullable Punishment punishment) {
        lookupLinkedAccount(userId).thenCompose(playerUUID -> {
            if (playerUUID == null) {
                // Unlinked
                return null;
            }

            return changePlayerBanState(playerUUID, newState, punishment).whenComplete((r, t) -> {
                if (t != null) {
                    logger().error("Failed to update ban state for " + playerUUID, t);
                } else {
                    logger().debug("Updated " + playerUUID + " ban state: " + r);
                }
            });
        });
    }

    private CompletableFuture<BanSyncResult> changePlayerBanState(UUID playerUUID, boolean newState, @Nullable Punishment punishment) {
        BanSyncConfig config = discordSRV.config().banSync;
        if (config.direction == SyncDirection.MINECRAFT_TO_DISCORD) {
            return CompletableFuture.completedFuture(BanSyncResult.WRONG_DIRECTION);
        }

        PunishmentModule.Bans bans = discordSRV.getModule(PunishmentModule.Bans.class);
        if (bans == null) {
            return CompletableFuture.completedFuture(BanSyncResult.NO_PUNISHMENT_INTEGRATION);
        }

        return bans.getBan(playerUUID).thenCompose(existingPunishment -> {
            if (existingPunishment == null) {
                if (newState) {
                    String reason = discordSRV.placeholderService().replacePlaceholders(config.gameBanReasonFormat, punishment);
                    String punisher = discordSRV.placeholderService().replacePlaceholders(config.gamePunisherFormat, punishment);
                    return bans.addBan(playerUUID, null, reason, punisher)
                            .thenApply(v -> BanSyncResult.BAN_PLAYER);
                } else {
                    // Already unbanned
                    return CompletableFuture.completedFuture(BanSyncResult.ALREADY_IN_SYNC);
                }
            } else {
                if (newState) {
                    // Already banned
                    return CompletableFuture.completedFuture(BanSyncResult.ALREADY_IN_SYNC);
                } else {
                    return bans.removeBan(playerUUID).thenApply(v -> BanSyncResult.UNBAN_PLAYER);
                }
            }
        });
    }

}
