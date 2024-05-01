package com.discordsrv.common.someone;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.profile.Profile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Someone {

    public static Someone.Resolved of(@NotNull DiscordSRVPlayer player, @NotNull DiscordUser user) {
        return of(player.uniqueId(), user.getId());
    }

    public static Someone.Resolved of(@NotNull UUID playerUUID, long userId) {
        return new Someone.Resolved(playerUUID, userId);
    }

    public static Someone of(@NotNull DiscordSRVPlayer player) {
        return of(player.uniqueId());
    }

    public static Someone of(@NotNull UUID playerUUID) {
        return new Someone(playerUUID, null);
    }

    public static Someone of(@NotNull DiscordUser user) {
        return of(user.getId());
    }

    public static Someone of(long userId) {
        return new Someone(null, userId);
    }

    private final UUID playerUUID;
    private final Long userId;

    private Someone(@Nullable UUID playerUUID, @Nullable Long userId) {
        this.playerUUID = playerUUID;
        this.userId = userId;
    }

    @NotNull
    public CompletableFuture<@NotNull Profile> profile(DiscordSRV discordSRV) {
        if (playerUUID != null) {
            return discordSRV.profileManager().lookupProfile(playerUUID);
        } else if (userId != null) {
            return discordSRV.profileManager().lookupProfile(userId);
        } else {
            throw new IllegalStateException("Cannot have Someone instance without either a Player UUID or User Id");
        }
    }

    @NotNull
    public CompletableFuture<Someone.@Nullable Resolved> withLinkedAccounts(DiscordSRV discordSRV) {
        if (playerUUID != null && userId != null) {
            return CompletableFuture.completedFuture(of(playerUUID, userId));
        }

        return profile(discordSRV).thenApply(profile -> {
            UUID playerUUID = profile.playerUUID();
            Long userId = profile.userId();
            if (playerUUID == null || userId == null) {
                return null;
            }
            return of(playerUUID, userId);
        });
    }

    @Nullable
    public UUID playerUUID() {
        return playerUUID;
    }

    @Nullable
    public Long userId() {
        return userId;
    }

    @Override
    public String toString() {
        return playerUUID != null ? playerUUID.toString() : Objects.requireNonNull(userId).toString();
    }

    @SuppressWarnings("DataFlowIssue")
    public static class Resolved extends Someone {

        private Resolved(@NotNull UUID playerUUID, @NotNull Long userId) {
            super(playerUUID, userId);
        }

        @Override
        public @NotNull UUID playerUUID() {
            return super.playerUUID();
        }

        @Override
        public @NotNull Long userId() {
            return super.userId();
        }
    }
}
