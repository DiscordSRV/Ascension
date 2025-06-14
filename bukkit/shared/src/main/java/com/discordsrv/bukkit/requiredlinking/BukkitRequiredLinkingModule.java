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
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.feature.linking.requirelinking.ServerRequireLinkingModule;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BukkitRequiredLinkingModule extends ServerRequireLinkingModule<BukkitDiscordSRV> implements Listener {

    public BukkitRequiredLinkingModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public BukkitRequiredLinkingConfig config() {
        return (BukkitRequiredLinkingConfig) discordSRV.config().requiredLinking;
    }

    @Override
    public void enable() {
        super.enable();

        register(AsyncPlayerPreLoginEvent.class, this::handle);
        register(PlayerLoginEvent.class, this::handle);
        register(PlayerJoinEvent.class, this::handle);
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());
    }

    public void disable() {
        HandlerList.unregisterAll(this);
        super.disable();
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void register(Class<T> eventType, BiConsumer<T, EventPriority> eventConsumer) {
        for (EventPriority priority : EventPriority.values()) {
            if (priority == EventPriority.MONITOR) {
                continue;
            }

            discordSRV.server().getPluginManager().registerEvent(
                    eventType,
                    this,
                    priority,
                    (listener, event) -> eventConsumer.accept((T) event, priority),
                    discordSRV.plugin(),
                    true
            );
        }
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
    // Kick
    //

    private void handle(AsyncPlayerPreLoginEvent event, EventPriority priority) {
        handle(
                "AsyncPlayerPreLoginEvent",
                priority,
                event.getUniqueId(),
                event.getName(),
                () -> event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED ? event.getLoginResult().name() : null,
                text -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, text)
        );
    }

    private void handle(PlayerLoginEvent event, EventPriority priority) {
        Player player = event.getPlayer();
        handle(
                "PlayerLoginEvent",
                priority,
                player.getUniqueId(),
                player.getName(),
                () -> event.getResult() != PlayerLoginEvent.Result.ALLOWED ? event.getResult().name() : null,
                text -> event.disallow(PlayerLoginEvent.Result.KICK_OTHER, text)
        );
    }

    private void handle(PlayerJoinEvent event, EventPriority priority) {
        Player player = event.getPlayer();
        handle(
                "PlayerJoinEvent",
                priority,
                player.getUniqueId(),
                player.getName(),
                () -> null,
                player::kickPlayer
        );
    }

    private void handle(
            String eventType,
            EventPriority priority,
            UUID playerUUID,
            String playerName,
            Supplier<String> alreadyBlocked,
            Consumer<String> disallow
    ) {
        BukkitRequiredLinkingConfig config = config();
        if (config == null) {
            disallow.accept(NOT_READY_MESSAGE);
            return;
        }

        if (!config.enabled || action() != ServerRequiredLinkingConfig.Action.KICK
                || !eventType.equals(config.kick.event) || !priority.name().equals(config.kick.priority)) {
            return;
        }

        String blockType = alreadyBlocked.get();
        if (blockType != null) {
            discordSRV.logger().debug(playerName + " is already blocked for " + eventType + "/" + priority + " (" + blockType + ")");
            return;
        }

        Component kickReason = getBlockReason(playerUUID, playerName, true).join();
        if (kickReason != null) {
            disallow.accept(BukkitComponentSerializer.legacy().serialize(kickReason));
        }
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            // Blocked by something else, cleanup memory
            frozen.remove(event.getPlayer().getUniqueId());
        }
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
