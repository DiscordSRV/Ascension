package com.discordsrv.fabric.module.ban;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.common.feature.bansync.BanSyncModule;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FabricBanModule extends AbstractFabricModule implements PunishmentModule.Bans {
    private static FabricBanModule instance;

    public FabricBanModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);

        instance = this;
    }


    @Override
    public void enable() {
        this.enabled = true;
    }

    public static void onBan(GameProfile gameProfile) {
        FabricDiscordSRV discordSRV = instance.discordSRV;
        BanSyncModule module = discordSRV.getModule(BanSyncModule.class);
        if (module != null) {
            instance.getBan(gameProfile.getId())
                    .whenComplete((punishment, t) -> module.notifyBanned(Objects.requireNonNull(discordSRV.playerProvider().player(gameProfile.getId())), punishment));
        }
    }

    @Override
    public CompletableFuture<@Nullable Punishment> getBan(@NotNull UUID playerUUID) {
        BannedPlayerList banList = discordSRV.getServer().getPlayerManager().getUserBanList();

        Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getUserCache()).getByUuid(playerUUID);
        if (!gameProfile.isPresent()) {
            return CompletableFuture.completedFuture(null);
        }

        BannedPlayerEntry banEntry = banList.get(gameProfile.get());
        if (banEntry == null) {
            return CompletableFuture.completedFuture(null);
        }
        Date expiration = banEntry.getExpiryDate();

        return CompletableFuture.completedFuture(new Punishment(
                expiration != null ? expiration.toInstant() : null,
                ComponentUtil.fromPlain(banEntry.getReason()),
                ComponentUtil.fromPlain(banEntry.getSource())
        ));
    }

    @Override
    public CompletableFuture<Void> addBan(
            @NotNull UUID playerUUID,
            @Nullable Instant until,
            @Nullable MinecraftComponent reason,
            @NotNull MinecraftComponent punisher
    ) {
        try {
            String reasonLegacy = reason != null ? ComponentUtil.fromAPI(reason).toString() : null;
            String punisherLegacy = ComponentUtil.fromAPI(punisher).toString();

            BannedPlayerList banList = discordSRV.getServer().getPlayerManager().getUserBanList();

            discordSRV.getServer().getPlayerManager().getUserBanList().add(new BannedPlayerEntry(
                    discordSRV.getServer().getUserCache().getByUuid(playerUUID).get(),
                    null,
                    ComponentUtil.fromAPI(reason).toString(),
                    null,
                    ComponentUtil.fromAPI(punisher).toString()
            ));

            ServerPlayerEntity serverPlayerEntity = discordSRV.getServer().getPlayerManager().getPlayer(playerUUID);
            if (serverPlayerEntity != null) {
                serverPlayerEntity.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.banned"));
            }
        } catch (Exception e) {
            discordSRV.logger().error("Failed to ban player", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeBan(@NotNull UUID playerUUID) {
        BannedPlayerList banList = discordSRV.getServer().getPlayerManager().getUserBanList();

        Optional<GameProfile> gameProfile = Objects.requireNonNull(discordSRV.getServer().getUserCache()).getByUuid(playerUUID);
        if (!gameProfile.isPresent()) {
            return CompletableFuture.completedFuture(null);
        }

        banList.remove(gameProfile.get());
        return CompletableFuture.completedFuture(null);
    }
}
