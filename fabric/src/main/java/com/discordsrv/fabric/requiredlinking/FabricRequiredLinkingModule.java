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
import com.github.benmanes.caffeine.cache.Cache;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class FabricRequiredLinkingModule extends ServerRequireLinkingModule<FabricDiscordSRV> {

    private boolean enabled = false;
    private final Cache<UUID, Boolean> linkCheckRateLimit;

    public FabricRequiredLinkingModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();

        register();
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
        ServerPlayConnectionEvents.INIT.register(this::onPlayerPreLogin);

    }

    @Override
    public void disable() {
        super.disable();

        this.enabled = false;
    }

    private final List<UUID> loginsCancelled = new CopyOnWriteArrayList<>();

    public boolean isLoginCancelled(UUID playerUUID) {
        return loginsCancelled.contains(playerUUID);
    }

    public void removeLoginCancelled(UUID playerUUID) {
        loginsCancelled.remove(playerUUID);
    }

    private void onPlayerPreLogin(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        if(!this.enabled) return;

        ServerRequiredLinkingConfig config = config();
        if (!config.enabled || config.action != ServerRequiredLinkingConfig.Action.KICK) {
            return;
        }

        ServerPlayerEntity player = serverPlayNetworkHandler.player;
        UUID playerUUID = player.getUuid();
        String playerName = player.getName().getString();

        Component kickReason = getBlockReason(playerUUID, playerName, true).join();
        if (kickReason != null) {
            serverPlayNetworkHandler.disconnect(MinecraftServerAudiences.of(minecraftServer).asNative(kickReason));
            loginsCancelled.add(playerUUID);
        }
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
    // Freeze - Not implemented yet
    //

    private final Map<UUID, Component> frozen = new ConcurrentHashMap<>();
    private final List<UUID> loginsHandled = new CopyOnWriteArrayList<>();

    private boolean isFrozen(ServerPlayerEntity player) {
        return frozen.containsKey(player.getUuid());
    }

    private void freeze(IPlayer player, Component blockReason) {
        frozen.put(player.uniqueId(), blockReason);
        player.sendMessage(blockReason);
    }

//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
//        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
//            return;
//        }
//
//        UUID playerUUID = event.getUniqueId();
//        loginsHandled.add(playerUUID);
//        handleLogin(playerUUID, event.getName());
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onPlayerLogin(PlayerLoginEvent event) {
//        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
//            frozen.remove(event.getPlayer().getUniqueId());
//        }
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onPlayerJoinLowest(PlayerJoinEvent event) {
//        Player player = event.getPlayer();
//        UUID playerUUID = player.getUniqueId();
//        if (!loginsHandled.remove(playerUUID)) {
//            handleLogin(playerUUID, player.getName());
//        }
//    }
//
//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onPlayerJoinMonitor(PlayerJoinEvent event) {
//        UUID playerUUID = event.getPlayer().getUniqueId();
//
//        Component blockReason = frozen.get(playerUUID);
//        if (blockReason == null) {
//            return;
//        }
//
//        IPlayer srvPlayer = discordSRV.playerProvider().player(playerUUID);
//        if (srvPlayer == null) {
//            throw new IllegalStateException("Player not available: " + playerUUID);
//        }
//
//        srvPlayer.sendMessage(blockReason);
//    }

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

//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onPlayerMove(PlayerMoveEvent event) {
//        Component freezeReason = frozen.get(event.getPlayer().getUniqueId());
//        if (freezeReason == null) {
//            return;
//        }
//
//        Location from = event.getFrom(), to = event.getTo();
//        if (from.getWorld().getName().equals(to.getWorld().getName())
//                && from.getBlockX() == to.getBlockX()
//                && from.getBlockZ() == to.getBlockZ()
//                && from.getBlockY() >= to.getBlockY()) {
//            return;
//        }
//
//        event.setTo(
//                new Location(
//                        from.getWorld(),
//                        from.getBlockX() + 0.5,
//                        from.getBlockY(),
//                        from.getBlockZ() + 0.5,
//                        from.getYaw(),
//                        from.getPitch()
//                )
//        );
//
//        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
//        player.sendMessage(freezeReason);
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
//        Component freezeReason = frozen.get(event.getPlayer().getUniqueId());
//        if (freezeReason == null) {
//            event.getRecipients().removeIf(this::isFrozen);
//            return;
//        }
//
//        event.setCancelled(true);
//
//        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
//        player.sendMessage(freezeReason);
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
//    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
//        if (!isFrozen(event.getPlayer())) {
//            return;
//        }
//
//        event.setCancelled(true);
//
//        String message = event.getMessage();
//        if (message.startsWith("/")) message = message.substring(1);
//        if (message.equals("discord link") || message.equals("link")) {
//            IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
//
//            if (linkCheckRateLimit.getIfPresent(player.uniqueId()) != null) {
//                player.sendMessage(discordSRV.messagesConfig(player).pleaseWaitBeforeRunningThatCommandAgain.asComponent());
//                return;
//            }
//            linkCheckRateLimit.put(player.uniqueId(), true);
//
//            player.sendMessage(Component.text("Checking..."));
//
//            UUID uuid = player.uniqueId();
//            getBlockReason(uuid, player.username(), false).whenComplete((reason, t) -> {
//                if (t != null) {
//                    return;
//                }
//
//                if (reason == null) {
//                    frozen.remove(uuid);
//                } else {
//                    freeze(player, reason);
//                }
//            });
//        }
//    }
}
