/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.LeaveMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.LeaveMessageReceiveEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.LeaveMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.event.events.player.PlayerConnectedEvent;
import com.discordsrv.common.permission.Permission;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class LeaveMessageModule extends AbstractGameMessageModule<LeaveMessageConfig, LeaveMessageReceiveEvent> {

    private final Map<UUID, Pair<Long, Future<?>>> playersJoinedRecently = new ConcurrentHashMap<>();

    public LeaveMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "LEAVE_MESSAGES");
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        UUID playerUUID = event.player().uniqueId();
        Pair<Long, Future<?>> pair = playersJoinedRecently.remove(playerUUID);
        if (pair != null) {
            pair.getValue().cancel(true);
        }

        long maxMS = 0;
        for (BaseChannelConfig channel : discordSRV.channelConfig().getAllChannels()) {
            long ms = channel.leaveMessages.ignoreIfJoinedWithinMS;
            if (maxMS < ms) {
                maxMS = ms;
            }
        }
        if (maxMS > 0) {
            long currentTime = System.currentTimeMillis();
            Future<?> removeFuture = discordSRV.scheduler().runLater(() -> playersJoinedRecently.remove(playerUUID), Duration.ofMillis(maxMS));
            playersJoinedRecently.put(playerUUID, Pair.of(currentTime, removeFuture));
        }
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onLeaveMessageReceive(LeaveMessageReceiveEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        process(event, event.getPlayer(), event.getGameChannel());
        event.markAsProcessed();
    }

    @Override
    protected CompletableFuture<Void> forwardToChannel(
            @Nullable LeaveMessageReceiveEvent event,
            @Nullable IPlayer player,
            @NotNull BaseChannelConfig config,
            @Nullable GameChannel channel
    ) {
        if (player != null) {
            Pair<Long, Future<?>> pair = playersJoinedRecently.remove(player.uniqueId());
            if (pair != null) {
                long delta = System.currentTimeMillis() - pair.getKey();
                if (delta < config.leaveMessages.ignoreIfJoinedWithinMS) {
                    logger().info(player.username() + " joined within timeout period, join message will not be sent");
                    return CompletableFuture.completedFuture(null);
                }
            }
        }

        if (config.leaveMessages.enableSilentPermission && player != null && player.hasPermission(Permission.SILENT_QUIT)) {
            logger().info(player.username() + " is leaving silently, leave message will not be sent");
            return CompletableFuture.completedFuture(null);
        }
        return super.forwardToChannel(event, player, config, channel);
    }

    @Override
    public LeaveMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.leaveMessages;
    }

    @Override
    public void postClusterToEventBus(GameChannel channel, @NotNull ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new LeaveMessageForwardedEvent(channel, cluster));
    }

    @Override
    public void setPlaceholders(
            LeaveMessageConfig config,
            LeaveMessageReceiveEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        MinecraftComponent messageComponent = event.getMessage();
        Component message = messageComponent != null ? ComponentUtil.fromAPI(messageComponent) : null;

        formatter.addPlaceholder("message", message);
    }
}
