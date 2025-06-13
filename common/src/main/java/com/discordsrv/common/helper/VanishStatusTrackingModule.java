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

package com.discordsrv.common.helper;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.vanish.PlayerVanishStatusChangeEvent;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.PluginIntegrationConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.events.player.PlayerDisconnectedEvent;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class VanishStatusTrackingModule extends AbstractModule<DiscordSRV> {

    private final ThreadLocal<Boolean> localUpdate = ThreadLocal.withInitial(() -> false);
    private final AtomicBoolean receivingVanishEvents = new AtomicBoolean(false);
    private final Map<UUID, Boolean> vanishStatuses = new ConcurrentHashMap<>();
    private Future<?> vanishUpdateFuture;

    public VanishStatusTrackingModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "VANISH_TRACKER"));
    }

    @Override
    public boolean isEnabled() {
        return discordSRV.config().integrations.vanishTracking != PluginIntegrationConfig.VanishTracking.EVENT_ONLY;
    }

    @Override
    public void enable() {
        receivingVanishEvents.set(false);
        updateVanishStatuses();
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        startTimedTracking();
    }

    @Override
    public void disable() {
        stopTimedTracking();
        vanishStatuses.clear();
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        synchronized (vanishStatuses) {
            vanishStatuses.put(event.player().uniqueId(), event.player().isVanished());
        }
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        synchronized (vanishStatuses) {
            vanishStatuses.remove(event.player().uniqueId());
        }
    }

    @Subscribe
    public void onPlayerVanishStatusChange(PlayerVanishStatusChangeEvent event) {
        if (localUpdate.get()) {
            return;
        }

        synchronized (vanishStatuses) {
            vanishStatuses.put(event.getPlayer().uniqueId(), event.isNewStatus());
        }
        receivingVanishEvents.set(true);
    }

    public boolean useTimedUpdates() {
        switch (discordSRV.config().integrations.vanishTracking) {
            case AUTO:
                return !receivingVanishEvents.get();
            case EVENT_ONLY:
                return false;
            case TIMER:
            default:
                return true;
        }
    }

    private void startTimedTracking() {
        stopTimedTracking();

        if (useTimedUpdates()) {
            int seconds = discordSRV.config().integrations.vanishTrackingTimerSeconds;
            vanishUpdateFuture = discordSRV.scheduler().runAtFixedRate(this::updateVanishStatuses, Duration.ofSeconds(seconds));
        }
    }

    private void stopTimedTracking() {
        if (vanishUpdateFuture != null) {
            vanishUpdateFuture.cancel(true);
            vanishUpdateFuture = null;
        }
    }

    private void updateVanishStatuses() {
        if (!useTimedUpdates()) {
            return;
        }

        try {
            localUpdate.set(true);

            Set<UUID> uniqueIds = new HashSet<>();
            for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
                uniqueIds.add(player.uniqueId());

                boolean newStatus = player.isVanished();
                Boolean oldStatus;
                synchronized (vanishStatuses) {
                    oldStatus = vanishStatuses.put(player.uniqueId(), newStatus);
                }

                if (oldStatus != null && oldStatus != newStatus) {
                    // status changed
                    logger().debug(player.username() + " vanish status changed to " + newStatus);
                    discordSRV.eventBus().publish(new PlayerVanishStatusChangeEvent(player, newStatus, true, null));
                }
            }

            synchronized (vanishStatuses) {
                vanishStatuses.keySet().removeIf(uuid -> !uniqueIds.contains(uuid));
            }
        } finally {
            localUpdate.remove();
        }
    }
}
