package com.discordsrv.api.module.type;

import com.discordsrv.api.module.Module;
import com.discordsrv.api.punishment.Punishment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentModule extends Module {

    interface Bans extends PunishmentModule {
        CompletableFuture<@Nullable Punishment> getBan(@NotNull UUID playerUUID);
        CompletableFuture<Void> addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable String reason, @NotNull String punisher);
        CompletableFuture<Void> removeBan(@NotNull UUID playerUUID);
    }

    interface Mutes extends PunishmentModule {
        CompletableFuture<@Nullable Punishment> getMute(@NotNull UUID playerUUID);
        CompletableFuture<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable String reason, @NotNull String punisher);
        CompletableFuture<Void> removeMute(@NotNull UUID playerUUID);
    }
}
