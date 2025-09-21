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

package com.discordsrv.fabric.requiredlinking;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.feature.linking.requirelinking.ServerRequireLinkingModule;
import com.discordsrv.fabric.FabricComponentFactory;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.player.FabricPlayer;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class FabricRequiredLinkingModule extends ServerRequireLinkingModule<FabricDiscordSRV> {

    private static FabricRequiredLinkingModule INSTANCE;

    public static void withInstance(Consumer<FabricRequiredLinkingModule> consumer) {
        if (INSTANCE != null && INSTANCE.enabled) {
            consumer.accept(INSTANCE);
        }
    }

    //? if minecraft: >= 1.21.9 {
    public static Text canJoin(PlayerConfigEntry configEntry) {
        if (INSTANCE == null || INSTANCE.config() == null) {
            return Text.of(NOT_READY_MESSAGE);
        }

        return INSTANCE.checkCanJoin(new GameProfile(configEntry.id(), configEntry.name()));
    }
    //?} else {
    public static Text canJoin(GameProfile profile) {
        if (INSTANCE == null || INSTANCE.config() == null) {
            return Text.of(NOT_READY_MESSAGE);
        }

        return INSTANCE.checkCanJoin(profile);
    }
    //?}

    private final Map<UUID, Consumer<IPlayer>> loginsHandled = new ConcurrentHashMap<>();
    private boolean enabled = false;

    public FabricRequiredLinkingModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        INSTANCE = this;

        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerQuit);

        //? if minecraft: >=1.20.2 {
        net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents.CONFIGURE.register(this::onPlayerPreLogin);
        //?} else {
        /*ServerPlayConnectionEvents.INIT.register(this::onPlayerPreLogin);
         *///?}
    }

    @Override
    public void enable() {
        super.enable();
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
        super.disable();
    }

    @Override
    public ServerRequiredLinkingConfig config() {
        return discordSRV.config().requiredLinking;
    }
    @Override
    public void recheck(IPlayer player) {
        ServerPlayerEntity playerEntity = discordSRV.getServer().getPlayerManager().getPlayer(player.uniqueId());
        if (playerEntity == null) {
            return;
        }
        GameProfile gameProfile = playerEntity.getGameProfile();
        getBlockReason(gameProfile, false).whenComplete((component, throwable) -> handleBlock(player, component));
    }

    public Task<Component> getBlockReason(GameProfile gameProfile, boolean join) {
        //? if minecraft: >=1.21.9 {
        boolean allowed = config().whitelistedPlayersCanBypass && discordSRV.getServer().getPlayerManager().getWhitelist().isAllowed(PlayerConfigEntry.fromNickname(discordSRV.componentFactory().getName(gameProfile)));
        //? } else {
        boolean allowed = config().whitelistedPlayersCanBypass && discordSRV.getServer().getPlayerManager().getWhitelist().isAllowed(gameProfile);
        //? }
        if (allowed) {
            return Task.completed(null);
        }

        return getBlockReason(discordSRV.componentFactory().getId(gameProfile), discordSRV.componentFactory().getName(gameProfile), join);
    }

    //
    // Kick
    //

    @Nullable
    public Text checkCanJoin(GameProfile profile) {
        if (!enabled) return null;

        ServerRequiredLinkingConfig config = config();
        if (!config.enabled || action() != ServerRequiredLinkingConfig.Action.KICK) {
            return null;
        }

        Component kickReason = getBlockReason(profile, true).join();
        if (kickReason != null) {
            return discordSRV.componentFactory().toNative(kickReason);
        }

        return null;
    }

    //
    // Freeze
    //

    public void onPlayerMove(ServerPlayerEntity player, PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (!enabled) return;

        Component freezeReason = frozen.get(player.getUuid());
        if (freezeReason == null || action() == ServerRequiredLinkingConfig.Action.SPECTATOR) {
            return;
        }

        BlockPos from = player.getBlockPos();
        BlockPos to = new BlockPos(
                MathHelper.floor(packet.getX(player.getX())),
                MathHelper.floor(packet.getY(player.getY())),
                MathHelper.floor(packet.getZ(player.getZ()))
        );
        if (from.getX() == to.getX() && from.getY() >= to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        player.requestTeleport(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
        IPlayer srvPlayer = discordSRV.playerProvider().player(player);
        srvPlayer.sendMessage(freezeReason);

        ci.cancel();
    }

    public void onCommandExecute(com.mojang.brigadier.ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        if (!enabled) return;

        ServerPlayerEntity playerEntity = parseResults.getContext().getSource().getPlayer();
        if (playerEntity == null || !isFrozen(playerEntity)) {
            return;
        }

        IPlayer srvPlayer = discordSRV.playerProvider().player(playerEntity);
        INSTANCE.checkCommand(srvPlayer, command, () -> INSTANCE.getBlockReason(playerEntity.getGameProfile(), false));

        ci.cancel();
    }

    //
    // Freeze
    //

    private boolean isFrozen(ServerPlayerEntity player) {
        return frozen.get(player.getUuid()) != null;
    }

    @Override
    protected void changeToSpectator(IPlayer player) {
        if (player instanceof FabricPlayer) {
            discordSRV.getServer().execute(() -> ((FabricPlayer) player).getPlayer().changeGameMode(GameMode.SPECTATOR));
        }
    }

    @Override
    public void removeFromSpectator(IPlayer player) {
        ServerPlayerEntity playerEntity = discordSRV.getServer().getPlayerManager().getPlayer(player.uniqueId());
        if (playerEntity != null) {
            discordSRV.getServer().execute(() -> playerEntity.changeGameMode(discordSRV.getServer().getDefaultGameMode()));
        }
    }

    public boolean allowChatMessage(UUID uuid) {
        if (!enabled) return true;

        // True if the message should be sent
        Component freezeReason = frozen.get(uuid);
        if (freezeReason == null) {
            return true;
        }

        IPlayer iPlayer = discordSRV.playerProvider().player(uuid);
        iPlayer.sendMessage(freezeReason);
        return false;
    }

    //? if minecraft: <1.20.2 {
    /*private void onPlayerPreLogin(ServerPlayNetworkHandler handler, MinecraftServer minecraftServer) {
        if (!enabled) return;

        UUID playerUUID = handler.getPlayer().getUuid();
        GameProfile gameProfile = handler.getPlayer().getGameProfile();

        loginsHandled.put(playerUUID, handleFreezeLogin(playerUUID, () -> getBlockReason(gameProfile, true).join()));
    }
    *///?} else {
    private void onPlayerPreLogin(net.minecraft.server.network.ServerConfigurationNetworkHandler handler, MinecraftServer minecraftServer) {
        if (!enabled) return;

        GameProfile gameProfile = handler.getDebugProfile();
        UUID playerUUID = discordSRV.componentFactory().getId(handler.getDebugProfile());

        loginsHandled.put(playerUUID, handleFreezeLogin(playerUUID, () -> getBlockReason(gameProfile, true).join()));
    }
    //?}

    private void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        if (!enabled) return;

        ServerPlayerEntity player = serverPlayNetworkHandler.player;
        UUID playerUUID = player.getUuid();

        Consumer<IPlayer> callback = loginsHandled.remove(playerUUID);
        if (callback == null) {
            callback = handleFreezeLogin(playerUUID, () -> getBlockReason(player.getGameProfile(), false).join());
        }

        IPlayer srvPlayer = discordSRV.playerProvider().player(player);
        callback.accept(srvPlayer);

        handleBlock(srvPlayer, frozen.get(playerUUID));
    }

    private void onPlayerQuit(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        if (!enabled) return;

        UUID playerUUID = serverPlayNetworkHandler.player.getUuid();
        loginsHandled.remove(playerUUID);
        frozen.remove(playerUUID);
    }
}
