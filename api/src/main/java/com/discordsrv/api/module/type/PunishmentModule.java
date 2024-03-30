package com.discordsrv.api.module.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public interface PunishmentModule {

    interface Bans extends PunishmentModule {
        Punishment getBan(@NotNull UUID playerUUID);
        void addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable String reason);
        void removeBan(@NotNull UUID playerUUID);
    }

    interface Mutes extends PunishmentModule {
        Punishment getMute(@NotNull UUID playerUUID);
        void addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable String reason);
        void removeMute(@NotNull UUID playerUUID);
    }

    class Punishment {

        private final boolean active;
        private final Instant until;
        private final String reason;

        public Punishment(boolean active, @Nullable Instant until, @Nullable String reason) {
            this.active = active;
            this.until = until;
            this.reason = reason;
        }

        public boolean active() {
            return active;
        }

        public Instant until() {
            return until;
        }

        public String reason() {
            return reason;
        }
    }
}
