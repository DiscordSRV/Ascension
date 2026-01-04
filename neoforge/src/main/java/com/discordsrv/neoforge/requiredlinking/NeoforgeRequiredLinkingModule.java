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

package com.discordsrv.neoforge.requiredlinking;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.feature.linking.requirelinking.ServerRequireLinkingModule;
import com.discordsrv.neoforge.NeoforgeDiscordSRV;
import com.discordsrv.neoforge.player.NeoforgePlayer;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NeoforgeRequiredLinkingModule extends ServerRequireLinkingModule<NeoforgeDiscordSRV> {

    private static NeoforgeRequiredLinkingModule INSTANCE;

    public static void withInstance(Consumer<NeoforgeRequiredLinkingModule> consumer) {
        if (INSTANCE != null && INSTANCE.enabled) {
            consumer.accept(INSTANCE);
        }
    }

    //? if minecraft: >= 1.21.9 {
    public static net.minecraft.network.chat.Component canJoin(net.minecraft.server.players.NameAndId configEntry) {
        if (INSTANCE == null || INSTANCE.config() == null) {
            return net.minecraft.network.chat.Component.nullToEmpty(NOT_READY_MESSAGE);
        }

        return INSTANCE.checkCanJoin(new GameProfile(configEntry.id(), configEntry.name()));
    }
    //?} else {
    /*public static net.minecraft.network.chat.Component canJoin(GameProfile profile) {
        if (INSTANCE == null || INSTANCE.config() == null) {
            return net.minecraft.network.chat.Component.nullToEmpty(NOT_READY_MESSAGE);
        }

        return INSTANCE.checkCanJoin(profile);
    }
    *///?}

    private final Map<UUID, Consumer<IPlayer>> loginsHandled = new ConcurrentHashMap<>();
    private boolean enabled = false;

    public NeoforgeRequiredLinkingModule(NeoforgeDiscordSRV discordSRV) {
        super(discordSRV);
        INSTANCE = this;

        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerQuit);
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
        ServerPlayer playerEntity = discordSRV.getServer().getPlayerList().getPlayer(player.uniqueId());
        if (playerEntity == null) {
            return;
        }
        GameProfile gameProfile = playerEntity.getGameProfile();
        getBlockReason(gameProfile, false).whenComplete((component, throwable) -> handleBlock(player, component));
    }

    public Task<Component> getBlockReason(GameProfile gameProfile, boolean join) {
        //? if minecraft: >=1.21.9 {
        boolean allowed = config().whitelistedPlayersCanBypass && discordSRV.getServer().getPlayerList().getWhiteList().isWhiteListed(net.minecraft.server.players.NameAndId.createOffline(discordSRV.getNameFromGameProfile(gameProfile)));
        //?} else {
        /*boolean allowed = config().whitelistedPlayersCanBypass && discordSRV.getServer().getPlayerList().getWhiteList().isWhiteListed(gameProfile);
        *///?}
        if (allowed) {
            return Task.completed(null);
        }

        return getBlockReason(discordSRV.getIdFromGameProfile(gameProfile), discordSRV.getNameFromGameProfile(gameProfile), join);
    }

    //
    // Kick
    //

    @Nullable
    public net.minecraft.network.chat.Component checkCanJoin(GameProfile profile) {
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

    public void onPlayerMove(ServerPlayer player, ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!enabled) return;

        Component freezeReason = frozen.get(player.getUUID());
        if (freezeReason == null || action() == ServerRequiredLinkingConfig.Action.SPECTATOR) {
            return;
        }

        BlockPos from = player.blockPosition();
        BlockPos to = new BlockPos(
                Mth.floor(packet.getX(player.getX())),
                Mth.floor(packet.getY(player.getY())),
                Mth.floor(packet.getZ(player.getZ()))
        );
        if (from.getX() == to.getX() && from.getY() >= to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        player.teleportTo(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
        IPlayer srvPlayer = discordSRV.playerProvider().player(player);
        srvPlayer.sendMessage(freezeReason);

        ci.cancel();
    }

    public void onCommandExecute(com.mojang.brigadier.ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        if (!enabled) return;

        ServerPlayer playerEntity = parseResults.getContext().getSource().getPlayer();
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

    private boolean isFrozen(ServerPlayer player) {
        return frozen.get(player.getUUID()) != null;
    }

    @Override
    protected void changeToSpectator(IPlayer player) {
        if (player instanceof NeoforgePlayer) {
            discordSRV.getServer().execute(() -> ((NeoforgePlayer) player).getPlayer().setGameMode(GameType.SPECTATOR));
        }
    }

    @Override
    public void removeFromSpectator(IPlayer player) {
        ServerPlayer playerEntity = discordSRV.getServer().getPlayerList().getPlayer(player.uniqueId());
        if (playerEntity != null) {
            discordSRV.getServer().execute(() -> playerEntity.setGameMode(discordSRV.getServer().getDefaultGameType()));
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

    public void onPlayerPreLogin(net.minecraft.server.network.ServerConfigurationPacketListenerImpl handler) {
        if (!enabled) return;

        GameProfile gameProfile = handler.getOwner();
        UUID playerUUID = discordSRV.getIdFromGameProfile(handler.getOwner());

        loginsHandled.put(playerUUID, handleFreezeLogin(playerUUID, () -> getBlockReason(gameProfile, true).join()));
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!enabled) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        UUID playerUUID = player.getUUID();

        Consumer<IPlayer> callback = loginsHandled.remove(playerUUID);
        if (callback == null) {
            callback = handleFreezeLogin(playerUUID, () -> getBlockReason(player.getGameProfile(), false).join());
        }

        IPlayer srvPlayer = discordSRV.playerProvider().player(player);
        callback.accept(srvPlayer);

        handleBlock(srvPlayer, frozen.get(playerUUID));
    }

    private void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!enabled) return;

        UUID playerUUID = event.getEntity().getUUID();
        loginsHandled.remove(playerUUID);
        frozen.remove(playerUUID);
    }
}
