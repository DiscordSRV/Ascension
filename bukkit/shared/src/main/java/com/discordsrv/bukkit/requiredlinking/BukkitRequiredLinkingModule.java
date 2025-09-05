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

package com.discordsrv.bukkit.requiredlinking;

import com.discordsrv.api.task.Task;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.config.main.BukkitRequiredLinkingConfig;
import com.discordsrv.bukkit.requiredlinking.listener.BukkitRequiredLinkingAsyncPreLoginListener;
import com.discordsrv.bukkit.requiredlinking.listener.BukkitRequiredLinkingJoinListener;
import com.discordsrv.bukkit.requiredlinking.listener.BukkitRequiredLinkingListener;
import com.discordsrv.bukkit.requiredlinking.listener.BukkitRequiredLinkingLoginListener;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.feature.linking.requirelinking.ServerRequireLinkingModule;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BukkitRequiredLinkingModule extends ServerRequireLinkingModule<BukkitDiscordSRV> implements Listener {

    private final BukkitDiscordSRV discordSRV;
    private BukkitRequiredLinkingListener<?> kickListener;

    public BukkitRequiredLinkingModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    @Override
    public BukkitRequiredLinkingConfig config() {
        return discordSRV.config().requiredLinking;
    }

    @Override
    public BukkitDiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public void enable() {
        super.enable();
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());
    }

    @Override
    public void reload() {
        boolean useKick = false;
        String kickEvent = null;
        EventPriority eventPriority = EventPriority.HIGHEST;
        if (discordSRV.config() == null) {
            useKick = true;
        } else if (discordSRV.config() != null) {
            useKick = true;

            BukkitRequiredLinkingConfig.KickOptions kickOptions = config().kick;
            kickEvent = kickOptions.event;
            try {
                eventPriority = EventPriority.valueOf(kickOptions.priority);
            } catch (IllegalArgumentException ignored) {
                logger().error("Invalid event priority: " + kickOptions.priority);
            }
        }

        BukkitRequiredLinkingListener<?> newKickListener = null;
        if (useKick) {
            final String asyncPlayerPreLoginEvent = "AsyncPlayerPreLoginEvent";
            if (kickEvent == null) {
                kickEvent = asyncPlayerPreLoginEvent;
            }
            switch (kickEvent) {
                case "PlayerLoginEvent":
                    newKickListener = new BukkitRequiredLinkingLoginListener(this, eventPriority);
                    break;
                case "PlayerJoinEvent":
                    newKickListener = new BukkitRequiredLinkingJoinListener(this, eventPriority);
                    break;
                case asyncPlayerPreLoginEvent:
                default:
                    if (!kickEvent.equals(asyncPlayerPreLoginEvent)) {
                        logger().error("Invalid kick event: " + kickEvent);
                    }
                    newKickListener = new BukkitRequiredLinkingAsyncPreLoginListener(this, eventPriority);
                    break;
            }
        }
        if (this.kickListener != null) {
            this.kickListener.close();
        }
        this.kickListener = newKickListener;
    }

    @Override
    public void disable() {
        if (kickListener != null) {
            kickListener.close();
            kickListener = null;
        }
        HandlerList.unregisterAll(this);
        super.disable();
    }

    @Override
    public Task<Component> getBlockReason(UUID playerUUID, String playerName, boolean join) {
        if (config().whitelistedPlayersCanBypass) {
            for (OfflinePlayer player : discordSRV.server().getWhitelistedPlayers()) {
                if (player.getUniqueId().equals(playerUUID)) {
                    return Task.completed(null);
                }
            }
        }

        return super.getBlockReason(playerUUID, playerName, join);
    }

    @Override
    public void recheck(IPlayer player) {
        getBlockReason(player.uniqueId(), player.username(), false)
                .whenComplete((component, throwable) -> handleBlock(player, component));
    }

    //
    // Freeze & Spectator
    //

    private final Map<UUID, Consumer<IPlayer>> loginsHandled = new ConcurrentHashMap<>();

    private boolean isFrozen(Player player) {
        return frozen.containsKey(player.getUniqueId());
    }

    @Override
    public void changeToSpectator(IPlayer player) {
        Player bukkitPlayer = discordSRV.server().getPlayer(player.uniqueId());
        discordSRV.scheduler().runOnMainThread(
                bukkitPlayer,
                () -> bukkitPlayer.setGameMode(GameMode.SPECTATOR)
        );
    }

    @Override
    public void removeFromSpectator(IPlayer player) {
        Player bukkitPlayer = discordSRV.server().getPlayer(player.uniqueId());
        discordSRV.scheduler().runOnMainThread(
                bukkitPlayer,
                () -> bukkitPlayer.setGameMode(discordSRV.server().getDefaultGameMode())
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID playerUUID = event.getUniqueId();
        loginsHandled.put(playerUUID, handleFreezeLogin(playerUUID, () -> getBlockReason(playerUUID, event.getName(), false).join()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinLowest(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Consumer<IPlayer> callback = loginsHandled.remove(playerUUID);
        if (callback == null) {
            // AsyncPlayerPreLoginEvent might never get called
            callback = handleFreezeLogin(playerUUID, () -> getBlockReason(playerUUID, player.getName(), false).join());
        }

        callback.accept(discordSRV.playerProvider().player(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinMonitor(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Component blockReason = frozen.get(playerUUID);
        if (blockReason == null) {
            return;
        }

        IPlayer srvPlayer = discordSRV.playerProvider().player(playerUUID);
        if (srvPlayer == null) {
            throw new IllegalStateException("Player not available: " + playerUUID);
        }

        handleBlock(srvPlayer, blockReason);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Component freezeReason = frozen.get(event.getPlayer().getUniqueId());
        if (freezeReason == null || action() == ServerRequiredLinkingConfig.Action.SPECTATOR) {
            return;
        }

        Location from = event.getFrom(), to = event.getTo();
        if (from.getWorld().getName().equals(to.getWorld().getName())
                && from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ()
                && from.getBlockY() >= to.getBlockY()) {
            // Don't block falling down or moving within the block
            return;
        }

        event.setTo(
                new Location(
                        from.getWorld(),
                        from.getBlockX() + 0.5,
                        from.getBlockY(),
                        from.getBlockZ() + 0.5,
                        from.getYaw(),
                        from.getPitch()
                )
        );

        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        player.sendMessage(freezeReason);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Component freezeReason = frozen.get(event.getPlayer().getUniqueId());
        if (freezeReason == null) {
            event.getRecipients().removeIf(this::isFrozen);
            return;
        }

        event.setCancelled(true);

        IPlayer player = discordSRV.playerProvider().player(event.getPlayer());
        player.sendMessage(freezeReason);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isFrozen(player)) {
            return;
        }

        event.setCancelled(true);

        IPlayer srvPlayer = discordSRV.playerProvider().player(player);
        checkCommand(srvPlayer, event.getMessage(), () -> getBlockReason(srvPlayer.uniqueId(), srvPlayer.username(), false));
    }
}
