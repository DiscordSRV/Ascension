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

import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.feature.linking.requirelinking.ServerRequireLinkingModule;
import com.discordsrv.fabric.player.FabricPlayer;
import com.github.benmanes.caffeine.cache.Cache;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.ParseResults;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class FabricRequiredLinkingModule extends ServerRequireLinkingModule<FabricDiscordSRV> {
    private static FabricRequiredLinkingModule instance;

    private boolean enabled = false;
    private final Cache<UUID, Boolean> linkCheckRateLimit;

    public FabricRequiredLinkingModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();

        register();

        instance = this;
    }

    @Override
    public ServerRequiredLinkingConfig config() {
        return discordSRV.config().requiredLinking;
    }

    @Override
    public void enable() {
        super.enable();

        this.enabled = true;
    }

    public void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((text, player, parameters) -> {
            // True if the message should be sent
            if (isFrozen(player)) {
                player.sendMessage(frozen.get(player.getUuid()));
                return false;
            }
            return true;
        });

        ServerConfigurationConnectionEvents.CONFIGURE.register(this::onPlayerPreLogin);
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerQuit);
    }

    @Override
    public void disable() {
        super.disable();

        this.enabled = false;
    }

    @Override
    public void recheck(IPlayer player) {
        getBlockReason(player.uniqueId(), player.username(), false).whenComplete((component, throwable) -> {
            if (component != null) {
                switch (action()) {
                    case KICK:
                        player.kick(component);
                        break;
                    case FREEZE:
                        freeze(player, component);
                        break;
                }
            } else if (action() == ServerRequiredLinkingConfig.Action.FREEZE) {
                frozen.remove(player.uniqueId());
            }
        });
    }

    public ServerRequiredLinkingConfig.Action action() {
        return config().action;
    }

    //
    // Kick
    //

    @Nullable
    public static Text checkCanJoin(GameProfile profile) {
        if (instance == null || (instance.discordSRV != null && instance.discordSRV.status() != DiscordSRV.Status.CONNECTED)) {
            return Text.of("Currently unavailable to check link status because the server is still connecting to Discord.\n\nTry again in a minute.");
        }
        if (!instance.enabled) return null;

        FabricDiscordSRV discordSRV = instance.discordSRV;
        assert discordSRV != null;
        ServerRequiredLinkingConfig config = instance.config();
        if (!config.enabled || config.action != ServerRequiredLinkingConfig.Action.KICK) {
            return null;
        }

        UUID playerUUID = profile.getId();
        String playerName = profile.getName();

        Component kickReason = instance.getBlockReason(playerUUID, playerName, true).join();
        if (kickReason != null) {
            return discordSRV.getAdventure().asNative(kickReason);
        }

        return null;
    }

    //
    // Freeze
    //

    private final Map<UUID, Component> frozen = new ConcurrentHashMap<>();
    private final List<UUID> loginsHandled = new CopyOnWriteArrayList<>();

    private boolean isFrozen(ServerPlayerEntity player) {
        Component freezeReason = frozen.get(player.getUuid());
        if (freezeReason == null) {
            frozen.remove(player.getUuid());
            return false;
        }
        return true;
    }

    private void freeze(IPlayer player, Component blockReason) {
        frozen.put(player.uniqueId(), blockReason);
        player.sendMessage(blockReason);
    }

    private void onPlayerPreLogin(ServerConfigurationNetworkHandler handler, MinecraftServer minecraftServer) {
        if(!enabled) return;
        UUID playerUUID = handler.getDebugProfile().getId();
        loginsHandled.add(playerUUID);
        handleLogin(playerUUID, handler.getDebugProfile().getName());
    }


    private void onPlayerJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        if(!enabled) return;
        UUID playerUUID = serverPlayNetworkHandler.player.getUuid();

        if(!loginsHandled.contains(playerUUID)) {
            handleLogin(playerUUID, serverPlayNetworkHandler.player.getName().getString());
        }

        Component blockReason = frozen.get(playerUUID);
        if (blockReason == null) {
            return;
        }

        IPlayer srvPlayer = discordSRV.playerProvider().player(playerUUID);
        if (srvPlayer == null) {
            throw new IllegalStateException("Player not available: " + playerUUID);
        }

        srvPlayer.sendMessage(blockReason);
    }

    private void onPlayerQuit(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        if(!enabled) return;
        UUID playerUUID = serverPlayNetworkHandler.player.getUuid();
        loginsHandled.remove(playerUUID);
        frozen.remove(playerUUID);
    }

    private void handleLogin(UUID playerUUID, String username) {
        if (discordSRV.isShutdown()) {
            return;
        } else if (!discordSRV.isReady()) {
            try {
                discordSRV.waitForStatus(DiscordSRV.Status.CONNECTED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ServerRequiredLinkingConfig config = config();
        if (!config.enabled || config.action != ServerRequiredLinkingConfig.Action.FREEZE) {
            return;
        }

        Component blockReason = getBlockReason(playerUUID, username, false).join();
        if (blockReason != null) {
            frozen.put(playerUUID, blockReason);
        }
    }

    public static void onPlayerMove(ServerPlayerEntity player, PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if(instance == null || !instance.enabled) return;
        Component freezeReason = instance.frozen.get(player.getUuid());
        if (freezeReason == null) {
            return;
        }

        BlockPos from = player.getBlockPos();
        BlockPos to = new BlockPos(MathHelper.floor(packet.getX(player.getX())), MathHelper.floor(packet.getY(player.getY())), MathHelper.floor(packet.getZ(player.getZ())));
        if(from.getX() == to.getX() && from.getY() >= to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        player.requestTeleport(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
        IPlayer iPlayer = instance.discordSRV.playerProvider().player(player);
        iPlayer.sendMessage(freezeReason);

        ci.cancel();
    }

    public static void onCommandExecute(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        if(instance == null || !instance.enabled) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;
        ServerPlayerEntity playerEntity = parseResults.getContext().getSource().getPlayer();
        if(playerEntity == null) return;

        if (!instance.isFrozen(playerEntity)) {
            return;
        }

        if(command.startsWith("/")) command = command.substring(1);
        if(command.equals("discord link") || command.equals("link")) {

            FabricPlayer player = discordSRV.playerProvider().player(playerEntity);

            UUID uuid = player.uniqueId();

            if (instance.linkCheckRateLimit.getIfPresent(uuid) != null) {
                player.sendMessage(discordSRV.messagesConfig(player).pleaseWaitBeforeRunningThatCommandAgain.asComponent());
                return;
            }
            instance.linkCheckRateLimit.put(uuid, true);

            player.sendMessage(discordSRV.messagesConfig(player).checkingLinkStatus.asComponent());

            instance.getBlockReason(uuid, player.username(), false).whenComplete((reason, t) -> {
                if (t != null) {
                    return;
                }

                if (reason == null) {
                    instance.frozen.remove(uuid);
                    player.sendMessage(discordSRV.messagesConfig(player).nowLinked1st.asComponent());
                } else {
                    instance.freeze(player, reason);
                }
            });
        }

        ci.cancel();
    }
}
