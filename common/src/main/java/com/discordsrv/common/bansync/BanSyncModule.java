package com.discordsrv.common.bansync;

import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.linking.AccountLinkedEvent;
import com.discordsrv.api.event.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.bansync.enums.BanSyncResult;
import com.discordsrv.common.event.events.player.PlayerConnectedEvent;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.profile.Profile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BanSyncModule extends AbstractModule<DiscordSRV> {

    public BanSyncModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    private CompletableFuture<Long> lookupLinkedAccount(UUID player) {
        return discordSRV.profileManager().lookupProfile(player)
                .thenApply(Profile::userId);
    }

    private CompletableFuture<UUID> lookupLinkedAccount(long userId) {
        return discordSRV.profileManager().lookupProfile(userId)
                .thenApply(Profile::playerUUID);
    }

    public void notifyBanned(IPlayer player, @Nullable String reason) {
        playerBanChange(player.uniqueId(), true);
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        playerBanChange(event.player().uniqueId(), false);
    }

    @Subscribe
    public void onGuildBan(GuildBanEvent event) {
        userBanChange(event.getUser().getIdLong(), true);
    }

    @Subscribe
    public void onGuildUnban(GuildUnbanEvent event) {
        userBanChange(event.getUser().getIdLong(), false);
    }

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {

    }

    @Subscribe
    public void onAccountUnlinked(AccountUnlinkedEvent event) {

    }

    private void playerBanChange(UUID player, boolean newState) {
        lookupLinkedAccount(player).thenApply(userId -> {
            if (userId == null) {
                // Unlinked
                return null;
            }

            // TODO: configurable reason format
            return changeUserBanState(userId, newState, null);
        });
    }

    private CompletableFuture<BanSyncResult> changeUserBanState(long userId, boolean newState, @Nullable String reason) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return CompletableFuture.completedFuture(BanSyncResult.NO_DISCORD_CONNECTION);
        }

        Guild guild = jda.getGuildById(0L); // TODO: config
        if (guild == null) {
            // Server doesn't exist
            return CompletableFuture.completedFuture(BanSyncResult.GUILD_DOESNT_EXIST);
        }

        UserSnowflake snowflake = UserSnowflake.fromId(userId);
        return guild.retrieveBan(snowflake).submit().exceptionally(t -> {
            if (t instanceof ErrorResponseException && ((ErrorResponseException) t).getErrorResponse() == ErrorResponse.UNKNOWN_BAN) {
                return null;
            }
            throw (RuntimeException) t;
        }).thenCompose(ban -> {
            if (ban == null) {
                if (newState) {
                    // TODO: configurable deletion timeframe
                    return guild.ban(snowflake, 0, TimeUnit.MILLISECONDS).reason(reason).submit().thenApply(v -> BanSyncResult.BAN_USER);
                } else {
                    // Already unbanned
                    return CompletableFuture.completedFuture(BanSyncResult.ALREADY_IN_SYNC);
                }
            } else {
                if (newState) {
                    // Already banned
                    return CompletableFuture.completedFuture(BanSyncResult.ALREADY_IN_SYNC);
                } else {
                    return guild.unban(snowflake).reason(reason).submit().thenApply(v -> BanSyncResult.UNBAN_USER);
                }
            }
        });
    }

    public void userBanChange(long userId, boolean newState) {
        lookupLinkedAccount(userId).thenApply(playerUUID -> {
            if (playerUUID == null) {
                // Unlinked
                return null;
            }

            // TODO: configurable reason format
            return changePlayerBanState(playerUUID, newState, null);
        });
    }

    private CompletableFuture<BanSyncResult> changePlayerBanState(UUID playerUUID, boolean newState, @Nullable String reason) {
        PunishmentModule.Bans bans = discordSRV.getModule(PunishmentModule.Bans.class);
        if (bans == null) {
            return CompletableFuture.completedFuture(BanSyncResult.NO_PUNISHMENT_INTEGRATION);
        }

        return bans.getBan(playerUUID)
                .thenCompose(punishment -> {
                    if (punishment == null) {
                        if (newState) {
                            return bans.addBan(playerUUID, null, reason, "DiscordSRV")
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
