package com.discordsrv.api.module.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentModule {

    interface Bans extends PunishmentModule {
        @Nullable
        CompletableFuture<Punishment> getBan(@NotNull UUID playerUUID);
        CompletableFuture<Void> addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable String reason, @NotNull String punisher);
        CompletableFuture<Void> removeBan(@NotNull UUID playerUUID);
    }

    interface Mutes extends PunishmentModule {
        @Nullable
        CompletableFuture<Punishment> getMute(@NotNull UUID playerUUID);
        CompletableFuture<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable String reason, @NotNull String punisher);
        CompletableFuture<Void> removeMute(@NotNull UUID playerUUID);
    }

    class Punishment {

        private final Instant until;
        private final String reason;
        private final String punisher;

        public Punishment(@Nullable Instant until, @Nullable String reason, @Nullable String punisher) {
            this.until = until;
            this.reason = reason;
            this.punisher = punisher;
        }

        public Instant until() {
            return until;
        }

        public String reason() {
            return reason;
        }

        public String punisher() {
            return punisher;
        }
    }
}
