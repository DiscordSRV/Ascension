/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.JoinMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.JoinMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import com.discordsrv.common.event.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.permission.Permission;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class JoinMessageModule extends AbstractGameMessageModule<IMessageConfig, JoinMessageReceiveEvent> {

    private final Map<UUID, Runnable> delayedTasks = new HashMap<>();

    public JoinMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "JOIN_MESSAGES");
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onJoinMessageReceive(JoinMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), event.getGameChannel());
        event.markAsProcessed();
    }

    @Override
    protected CompletableFuture<Void> forwardToChannel(
            @Nullable JoinMessageReceiveEvent event,
            @Nullable IPlayer player,
            @NotNull BaseChannelConfig config
    ) {
        if (config.joinMessages().enableSilentPermission && player != null && player.hasPermission(Permission.SILENT_JOIN)) {
            logger().info(player.username() + " is joining silently, join message will not be sent");
            return CompletableFuture.completedFuture(null);
        }

        long delay = config.joinMessages().ignoreIfLeftWithinMS;
        if (player != null && delay > 0) {
            UUID playerUUID = player.uniqueId();

            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            synchronized (delayedTasks) {
                Future<?> future = discordSRV.scheduler().runLater(() -> {
                    CompletableFuture<Void> forward = super.forwardToChannel(event, player, config);

                    synchronized (delayedTasks) {
                        delayedTasks.remove(playerUUID);
                    }
                    try {
                        completableFuture.complete(forward.get());
                    } catch (ExecutionException e) {
                        completableFuture.completeExceptionally(e.getCause());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, Duration.ofMillis(delay));
                delayedTasks.put(playerUUID, () -> {
                    completableFuture.complete(null);
                    future.cancel(true);
                });
            }
            return completableFuture;
        }
        return super.forwardToChannel(event, player, config);
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        IPlayer player = event.player();
        Runnable cancel = delayedTasks.remove(player.uniqueId());
        if (cancel != null) {
            cancel.run();
            logger().info(player.username() + " left within timeout period, join message will not be sent");
        }
    }

    @Override
    public IMessageConfig mapConfig(JoinMessageReceiveEvent event, BaseChannelConfig channelConfig) {
        return channelConfig.joinMessages().getForEvent(event);
    }

    @Override
    public IMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.joinMessages();
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new JoinMessageForwardedEvent(cluster));
    }

    @Override
    public void setPlaceholders(
            IMessageConfig config,
            JoinMessageReceiveEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        MinecraftComponent messageComponent = event.getMessage();
        Component message = messageComponent != null ? ComponentUtil.fromAPI(messageComponent) : null;

        formatter.addPlaceholder("message", message);
    }
}
